package com.ss.Misty_Screen_Recoder_lite;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Lossless video trimming using MediaExtractor + MediaMuxer (no re-encode,
 * so trimming a 1 GB video takes seconds and loses no quality). The start
 * point snaps to the nearest previous keyframe.
 */
public final class TrimUtils {
    private static final String TAG = "TrimUtils";

    private TrimUtils() {
    }

    /**
     * Trims [startUs, endUs] from the source video into a new gallery entry.
     * Runs synchronously — call from a background thread.
     *
     * @return the display name of the saved file
     */
    public static String trim(Context context, Uri sourceUri, long startUs, long endUs, String baseName) throws IOException {
        File tempFile = new File(context.getCacheDir(), "trim_" + System.currentTimeMillis() + ".mp4");
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            extractor.setDataSource(context, sourceUri, null);

            muxer = new MediaMuxer(tempFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Map<Integer, Integer> trackMap = new HashMap<>();
            int maxInputSize = 1024 * 1024;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime == null) continue;
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    trackMap.put(i, muxer.addTrack(format));
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxInputSize = Math.max(maxInputSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
                    }
                }
            }
            if (trackMap.isEmpty()) {
                throw new IOException("No playable tracks found in video");
            }

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(maxInputSize);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long timeOffset = -1;

            while (true) {
                int size = extractor.readSampleData(buffer, 0);
                if (size < 0) break;
                long sampleTime = extractor.getSampleTime();
                if (sampleTime > endUs) break;
                if (timeOffset < 0) timeOffset = sampleTime;

                int trackIndex = extractor.getSampleTrackIndex();
                Integer muxerTrack = trackMap.get(trackIndex);
                if (muxerTrack != null) {
                    info.offset = 0;
                    info.size = size;
                    info.presentationTimeUs = sampleTime - timeOffset;
                    info.flags = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                            ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                    muxer.writeSampleData(muxerTrack, buffer, info);
                }
                extractor.advance();
            }

            muxer.stop();
        } finally {
            try {
                if (muxer != null) muxer.release();
            } catch (Exception e) {
                LogUtils.e(TAG, "Muxer release failed: " + e.getMessage());
            }
            extractor.release();
        }

        try {
            return saveToGallery(context, tempFile, baseName);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    private static String saveToGallery(Context context, File source, String baseName) throws IOException {
        String displayName = buildTrimmedName(baseName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HBRecorder");
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Could not create gallery entry");
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new IOException("Could not open gallery entry");
                VaultManager.copy(in, out);
            } catch (IOException e) {
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
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new java.io.FileOutputStream(outFile)) {
                VaultManager.copy(in, out);
            }
            android.media.MediaScannerConnection.scanFile(context,
                    new String[]{outFile.getAbsolutePath()}, null, null);
        }
        return displayName;
    }

    private static String buildTrimmedName(String baseName) {
        String name = baseName != null ? baseName : "video.mp4";
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        return stem + "_trimmed_" + System.currentTimeMillis() + ".mp4";
    }
}
