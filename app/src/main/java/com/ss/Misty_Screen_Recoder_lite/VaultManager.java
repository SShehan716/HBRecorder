package com.ss.Misty_Screen_Recoder_lite;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the private video vault: hidden files live in app-private storage
 * (invisible to the gallery and other apps), optionally encrypted with
 * AES-256 via {@link VaultCrypto}. A JSON index keeps original metadata so
 * videos can be restored to the gallery later.
 */
public class VaultManager {
    private static final String TAG = "VaultManager";
    private static final String VAULT_DIR = "vault";
    private static final String INDEX_FILE = "vault_index.json";

    private static VaultManager instance;

    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess();

        void onError(String message);
    }

    public interface FileCallback {
        void onSuccess(File file);

        void onError(String message);
    }

    public static class VaultItem {
        public String id;            // stored file name inside the vault dir
        public String originalName;  // display name shown to the user
        public boolean encrypted;
        public long dateAdded;
        public long sizeBytes;

        public File getFile(Context context) {
            return new File(getVaultDir(context), id);
        }
    }

    private VaultManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized VaultManager getInstance(Context context) {
        if (instance == null) {
            instance = new VaultManager(context);
        }
        return instance;
    }

    private static File getVaultDir(Context context) {
        File dir = new File(context.getFilesDir(), VAULT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            LogUtils.e(TAG, "Failed to create vault directory");
        }
        return dir;
    }

    private File getIndexFile() {
        return new File(getVaultDir(appContext), INDEX_FILE);
    }

    // ---------- index handling ----------

    public synchronized List<VaultItem> listItems() {
        List<VaultItem> items = new ArrayList<>();
        File indexFile = getIndexFile();
        if (!indexFile.exists()) return items;
        try {
            byte[] data = new byte[(int) indexFile.length()];
            try (FileInputStream in = new FileInputStream(indexFile)) {
                int off = 0;
                while (off < data.length) {
                    int read = in.read(data, off, data.length - off);
                    if (read < 0) break;
                    off += read;
                }
            }
            JSONArray array = new JSONArray(new String(data, "UTF-8"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                VaultItem item = new VaultItem();
                item.id = obj.getString("id");
                item.originalName = obj.getString("originalName");
                item.encrypted = obj.optBoolean("encrypted", false);
                item.dateAdded = obj.optLong("dateAdded", 0);
                item.sizeBytes = obj.optLong("sizeBytes", 0);
                if (item.getFile(appContext).exists()) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to read vault index: " + e.getMessage());
        }
        return items;
    }

    private synchronized void saveIndex(List<VaultItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (VaultItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("originalName", item.originalName);
                obj.put("encrypted", item.encrypted);
                obj.put("dateAdded", item.dateAdded);
                obj.put("sizeBytes", item.sizeBytes);
                array.put(obj);
            }
            try (FileOutputStream out = new FileOutputStream(getIndexFile())) {
                out.write(array.toString().getBytes("UTF-8"));
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to save vault index: " + e.getMessage());
        }
    }

    // ---------- operations ----------

    /**
     * Copies a gallery video into the vault (optionally encrypting it) and then
     * removes the original from the gallery.
     */
    public void hideVideo(Uri sourceUri, String displayName, boolean encrypt, Callback callback) {
        executor.execute(() -> {
            File target = null;
            try {
                VaultItem item = new VaultItem();
                item.id = UUID.randomUUID().toString() + (encrypt ? ".enc" : ".vid");
                item.originalName = displayName != null ? displayName : "video_" + System.currentTimeMillis() + ".mp4";
                item.encrypted = encrypt;
                item.dateAdded = System.currentTimeMillis();

                target = item.getFile(appContext);
                ContentResolver resolver = appContext.getContentResolver();
                try (InputStream in = resolver.openInputStream(sourceUri);
                     OutputStream out = new FileOutputStream(target)) {
                    if (in == null) throw new IOException("Cannot open source video");
                    if (encrypt) {
                        VaultCrypto.encrypt(in, out);
                    } else {
                        copy(in, out);
                    }
                }
                item.sizeBytes = target.length();

                // Remove the original from the gallery. If this fails we roll back
                // so the user never gets a duplicate.
                int deleted = resolver.delete(sourceUri, null, null);
                if (deleted <= 0) {
                    throw new IOException("Could not remove the original video from the gallery");
                }

                List<VaultItem> items = listItems();
                items.add(item);
                saveIndex(items);
                postSuccess(callback);
            } catch (Exception e) {
                if (target != null && target.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    target.delete();
                }
                LogUtils.e(TAG, "hideVideo failed: " + e.getMessage());
                postError(callback, e.getMessage());
            }
        });
    }

    /** Restores a vault item back into the public gallery (Movies/HBRecorder). */
    public void restoreVideo(VaultItem item, Callback callback) {
        executor.execute(() -> {
            try {
                File source = item.getFile(appContext);
                if (!source.exists()) throw new IOException("Vault file missing");

                try (InputStream rawIn = new FileInputStream(source)) {
                    writeToGallery(rawIn, item.originalName, item.encrypted);
                }

                //noinspection ResultOfMethodCallIgnored
                source.delete();
                removeFromIndex(item.id);
                postSuccess(callback);
            } catch (Exception e) {
                LogUtils.e(TAG, "restoreVideo failed: " + e.getMessage());
                postError(callback, e.getMessage());
            }
        });
    }

    /** Permanently deletes a vault item. */
    public void deleteVideo(VaultItem item, Callback callback) {
        executor.execute(() -> {
            File file = item.getFile(appContext);
            if (file.exists() && !file.delete()) {
                postError(callback, "Could not delete file");
                return;
            }
            removeFromIndex(item.id);
            postSuccess(callback);
        });
    }

    /**
     * Produces a playable file for the item. Unencrypted items are returned
     * directly; encrypted items are decrypted into the app cache first.
     */
    public void preparePlayback(VaultItem item, FileCallback callback) {
        executor.execute(() -> {
            try {
                File source = item.getFile(appContext);
                if (!source.exists()) throw new IOException("Vault file missing");
                if (!item.encrypted) {
                    postFile(callback, source);
                    return;
                }
                File cacheFile = new File(appContext.getCacheDir(), "vault_play_" + item.id + ".mp4");
                if (!cacheFile.exists() || cacheFile.length() == 0) {
                    try (InputStream in = new FileInputStream(source);
                         OutputStream out = new FileOutputStream(cacheFile)) {
                        VaultCrypto.decrypt(in, out);
                    } catch (Exception e) {
                        //noinspection ResultOfMethodCallIgnored
                        cacheFile.delete();
                        throw e;
                    }
                }
                postFile(callback, cacheFile);
            } catch (Exception e) {
                LogUtils.e(TAG, "preparePlayback failed: " + e.getMessage());
                if (callback != null) {
                    String msg = e.getMessage();
                    mainHandler.post(() -> callback.onError(msg != null ? msg : "Decryption failed"));
                }
            }
        });
    }

    /** Deletes any decrypted playback copies left in the cache. */
    public void clearPlaybackCache() {
        executor.execute(() -> {
            File cacheDir = appContext.getCacheDir();
            File[] files = cacheDir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.getName().startsWith("vault_play_")) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        });
    }

    private void writeToGallery(InputStream rawIn, String displayName, boolean encrypted) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = appContext.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Video.Media.MIME_TYPE, guessMimeType(displayName));
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HBRecorder");
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Could not create gallery entry");
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new IOException("Could not open gallery entry");
                if (encrypted) {
                    VaultCrypto.decrypt(rawIn, out);
                } else {
                    copy(rawIn, out);
                }
            } catch (Exception e) {
                resolver.delete(uri, null, null);
                throw e;
            }
            values.clear();
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
        } else {
            File moviesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder");
            if (!moviesDir.exists() && !moviesDir.mkdirs()) {
                throw new IOException("Could not create Movies/HBRecorder");
            }
            File outFile = new File(moviesDir, displayName);
            if (outFile.exists()) {
                outFile = new File(moviesDir, System.currentTimeMillis() + "_" + displayName);
            }
            try (OutputStream out = new FileOutputStream(outFile)) {
                if (encrypted) {
                    VaultCrypto.decrypt(rawIn, out);
                } else {
                    copy(rawIn, out);
                }
            }
            android.media.MediaScannerConnection.scanFile(appContext,
                    new String[]{outFile.getAbsolutePath()}, null, null);
        }
    }

    private synchronized void removeFromIndex(String id) {
        List<VaultItem> items = listItems();
        List<VaultItem> remaining = new ArrayList<>();
        for (VaultItem item : items) {
            if (!item.id.equals(id)) remaining.add(item);
        }
        saveIndex(remaining);
    }

    private static String guessMimeType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".3gp")) return "video/3gpp";
        return "video/mp4";
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    private void postSuccess(Callback callback) {
        if (callback != null) mainHandler.post(callback::onSuccess);
    }

    private void postError(Callback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message != null ? message : "Unknown error"));
        }
    }

    private void postFile(FileCallback callback, File file) {
        if (callback != null) mainHandler.post(() -> callback.onSuccess(file));
    }
}
