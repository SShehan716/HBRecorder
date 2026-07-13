package com.ss.Misty_Screen_Recoder_lite;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Library of screen recordings saved by the app (Movies/HBRecorder).
 * From here videos can be played, trimmed, shared, deleted, or hidden
 * in the encrypted private vault.
 */
public class RecordingsActivity extends AppCompatActivity implements RecordingsAdapter.Listener {
    private static final int REQUEST_MEDIA_PERMISSION = 4001;

    private RecordingsAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        Toolbar toolbar = findViewById(R.id.recordings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_recordings);
        }

        emptyView = findViewById(R.id.recordings_empty);
        RecyclerView recyclerView = findViewById(R.id.recordings_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordingsAdapter(this);
        recyclerView.setAdapter(adapter);

        if (hasMediaPermission()) {
            loadRecordings();
        } else {
            requestMediaPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasMediaPermission()) {
            loadRecordings();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.shutdown();
    }

    // ---------- permissions ----------

    private String requiredPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return Manifest.permission.READ_MEDIA_VIDEO;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasMediaPermission() {
        return ContextCompat.checkSelfPermission(this, requiredPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMediaPermission() {
        androidx.core.app.ActivityCompat.requestPermissions(this,
                new String[]{requiredPermission()}, REQUEST_MEDIA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadRecordings();
            } else {
                Toast.makeText(this, R.string.media_permission_needed, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // ---------- data ----------

    private void loadRecordings() {
        List<RecordingItem> items = new ArrayList<>();
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
        };

        String selection;
        String[] selectionArgs;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
            selectionArgs = new String[]{"%HBRecorder%"};
        } else {
            selection = MediaStore.Video.Media.DATA + " LIKE ?";
            selectionArgs = new String[]{"%/HBRecorder/%"};
        }

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
                while (cursor.moveToNext()) {
                    RecordingItem item = new RecordingItem();
                    item.uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol));
                    item.displayName = cursor.getString(nameCol);
                    item.durationMs = cursor.getLong(durCol);
                    item.sizeBytes = cursor.getLong(sizeCol);
                    item.dateAddedMs = cursor.getLong(dateCol) * 1000L;
                    items.add(item);
                }
            }
        } catch (Exception e) {
            LogUtils.e("RecordingsActivity", "Failed to load recordings: " + e.getMessage());
        }

        adapter.setItems(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recordings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_open_vault) {
            startActivity(new Intent(this, VaultActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- item actions ----------

    @Override
    public void onItemClick(RecordingItem item) {
        playVideo(item);
    }

    @Override
    public void onItemMenuClick(RecordingItem item, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, R.string.action_play);
        popup.getMenu().add(0, 2, 1, R.string.action_trim);
        popup.getMenu().add(0, 3, 2, R.string.action_hide_vault);
        popup.getMenu().add(0, 4, 3, R.string.action_share);
        popup.getMenu().add(0, 5, 4, R.string.action_delete);
        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 1:
                    playVideo(item);
                    return true;
                case 2:
                    trimVideo(item);
                    return true;
                case 3:
                    hideVideo(item);
                    return true;
                case 4:
                    shareVideo(item);
                    return true;
                case 5:
                    confirmDelete(item);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void playVideo(RecordingItem item) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_URI, item.uri.toString());
        intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.displayName);
        startActivity(intent);
    }

    private void trimVideo(RecordingItem item) {
        Intent intent = new Intent(this, TrimActivity.class);
        intent.putExtra(TrimActivity.EXTRA_URI, item.uri.toString());
        intent.putExtra(TrimActivity.EXTRA_NAME, item.displayName);
        startActivity(intent);
    }

    private void hideVideo(RecordingItem item) {
        // The vault needs a PIN before anything can be hidden.
        if (!PinManager.isPinSet(this)) {
            Toast.makeText(this, R.string.vault_set_pin_first, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, VaultActivity.class));
            return;
        }
        String[] options = {
                getString(R.string.hide_only),
                getString(R.string.hide_and_encrypt)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_hide_vault)
                .setItems(options, (dialog, which) -> {
                    boolean encrypt = which == 1;
                    Toast.makeText(this, R.string.vault_hiding, Toast.LENGTH_SHORT).show();
                    VaultManager.getInstance(this).hideVideo(item.uri, item.displayName, encrypt,
                            new VaultManager.Callback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(RecordingsActivity.this,
                                            encrypt ? R.string.vault_hidden_encrypted : R.string.vault_hidden,
                                            Toast.LENGTH_SHORT).show();
                                    loadRecordings();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(RecordingsActivity.this,
                                            getString(R.string.vault_hide_failed, message), Toast.LENGTH_LONG).show();
                                    loadRecordings();
                                }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void shareVideo(RecordingItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("video/*");
        share.putExtra(Intent.EXTRA_STREAM, item.uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.action_share)));
    }

    private void confirmDelete(RecordingItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.delete_confirm, item.displayName))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteVideo(item))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteVideo(RecordingItem item) {
        try {
            int deleted = getContentResolver().delete(item.uri, null, null);
            if (deleted > 0) {
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            // The row belongs to a previous install; ask the system for permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    ArrayList<Uri> uris = new ArrayList<>();
                    uris.add(item.uri);
                    startIntentSenderForResult(
                            MediaStore.createDeleteRequest(getContentResolver(), uris).getIntentSender(),
                            4002, null, 0, 0, 0);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
            }
        }
        loadRecordings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4002) {
            loadRecordings();
        }
    }
}
