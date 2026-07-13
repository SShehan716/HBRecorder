package com.ss.Misty_Screen_Recoder_lite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * PIN-protected private vault. Hidden videos live in app-private storage,
 * invisible to the gallery and other apps; encrypted ones are additionally
 * protected with AES-256 keyed by the Android Keystore.
 */
public class VaultActivity extends AppCompatActivity implements RecordingsAdapter.Listener {

    private RecordingsAdapter adapter;
    private TextView emptyView;
    private View contentView;
    private boolean unlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);
        // Keep vault contents out of the recents screen and screenshots.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        Toolbar toolbar = findViewById(R.id.vault_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.private_vault);
        }

        emptyView = findViewById(R.id.vault_empty);
        contentView = findViewById(R.id.vault_content);
        RecyclerView recyclerView = findViewById(R.id.vault_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordingsAdapter(this);
        recyclerView.setAdapter(adapter);

        contentView.setVisibility(View.INVISIBLE);

        if (PinManager.isPinSet(this)) {
            promptForPin();
        } else {
            promptCreatePin();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.shutdown();
        // Remove decrypted playback copies whenever the vault closes.
        VaultManager.getInstance(this).clearPlaybackCache();
    }

    // ---------- PIN ----------

    private EditText buildPinField() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint(R.string.pin_hint);
        return input;
    }

    private void promptCreatePin() {
        EditText input = buildPinField();
        new AlertDialog.Builder(this)
                .setTitle(R.string.vault_create_pin)
                .setMessage(R.string.vault_create_pin_message)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String pin = input.getText().toString().trim();
                    if (pin.length() < 4) {
                        Toast.makeText(this, R.string.pin_too_short, Toast.LENGTH_SHORT).show();
                        promptCreatePin();
                        return;
                    }
                    confirmCreatePin(pin);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private void confirmCreatePin(String firstPin) {
        EditText input = buildPinField();
        new AlertDialog.Builder(this)
                .setTitle(R.string.vault_confirm_pin)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (firstPin.equals(input.getText().toString().trim())) {
                        PinManager.setPin(this, firstPin);
                        Toast.makeText(this, R.string.pin_created, Toast.LENGTH_SHORT).show();
                        onUnlocked();
                    } else {
                        Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show();
                        promptCreatePin();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private void promptForPin() {
        EditText input = buildPinField();
        new AlertDialog.Builder(this)
                .setTitle(R.string.vault_enter_pin)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (PinManager.verifyPin(this, input.getText().toString().trim())) {
                        onUnlocked();
                    } else {
                        Toast.makeText(this, R.string.pin_wrong, Toast.LENGTH_SHORT).show();
                        promptForPin();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private void onUnlocked() {
        unlocked = true;
        contentView.setVisibility(View.VISIBLE);
        loadVaultItems();
    }

    // ---------- data ----------

    private void loadVaultItems() {
        if (!unlocked) return;
        List<VaultManager.VaultItem> vaultItems = VaultManager.getInstance(this).listItems();
        List<RecordingItem> items = new ArrayList<>();
        for (VaultManager.VaultItem vaultItem : vaultItems) {
            RecordingItem item = new RecordingItem();
            item.vaultItem = vaultItem;
            item.file = vaultItem.getFile(this);
            item.displayName = vaultItem.originalName;
            item.sizeBytes = vaultItem.sizeBytes;
            item.dateAddedMs = vaultItem.dateAdded;
            item.encrypted = vaultItem.encrypted;
            items.add(item);
        }
        adapter.setItems(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.change_pin);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == 1) {
            changePin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changePin() {
        if (!unlocked) return;
        EditText input = buildPinField();
        new AlertDialog.Builder(this)
                .setTitle(R.string.vault_create_pin)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String pin = input.getText().toString().trim();
                    if (pin.length() < 4) {
                        Toast.makeText(this, R.string.pin_too_short, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PinManager.setPin(this, pin);
                    Toast.makeText(this, R.string.pin_created, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------- item actions ----------

    @Override
    public void onItemClick(RecordingItem item) {
        playItem(item);
    }

    @Override
    public void onItemMenuClick(RecordingItem item, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, R.string.action_play);
        popup.getMenu().add(0, 2, 1, R.string.action_restore);
        popup.getMenu().add(0, 3, 2, R.string.action_delete);
        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 1:
                    playItem(item);
                    return true;
                case 2:
                    restoreItem(item);
                    return true;
                case 3:
                    confirmDelete(item);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void playItem(RecordingItem item) {
        if (item.encrypted) {
            Toast.makeText(this, R.string.vault_decrypting, Toast.LENGTH_SHORT).show();
        }
        VaultManager.getInstance(this).preparePlayback(item.vaultItem, new VaultManager.FileCallback() {
            @Override
            public void onSuccess(java.io.File file) {
                Intent intent = new Intent(VaultActivity.this, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_URI, Uri.fromFile(file).toString());
                intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.displayName);
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(VaultActivity.this, getString(R.string.vault_play_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void restoreItem(RecordingItem item) {
        Toast.makeText(this, R.string.vault_restoring, Toast.LENGTH_SHORT).show();
        VaultManager.getInstance(this).restoreVideo(item.vaultItem, new VaultManager.Callback() {
            @Override
            public void onSuccess() {
                Toast.makeText(VaultActivity.this, R.string.vault_restored, Toast.LENGTH_SHORT).show();
                loadVaultItems();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(VaultActivity.this, getString(R.string.vault_restore_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(RecordingItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.delete_confirm, item.displayName))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        VaultManager.getInstance(this).deleteVideo(item.vaultItem, new VaultManager.Callback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(VaultActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                                loadVaultItems();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(VaultActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
