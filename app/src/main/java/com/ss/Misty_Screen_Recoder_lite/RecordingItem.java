package com.ss.Misty_Screen_Recoder_lite;

import android.net.Uri;

import java.io.File;

/** A video shown in the recordings library or the vault list. */
public class RecordingItem {
    public Uri uri;                       // MediaStore uri (gallery items)
    public File file;                     // vault file (vault items)
    public VaultManager.VaultItem vaultItem; // set for vault items
    public String displayName;
    public long durationMs;
    public long sizeBytes;
    public long dateAddedMs;
    public boolean encrypted;

    public boolean isVaultItem() {
        return vaultItem != null;
    }
}
