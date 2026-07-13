package com.ss.Misty_Screen_Recoder_lite;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM encryption for vault videos, keyed by a non-exportable
 * Android Keystore key. Files are encrypted in independent chunks so
 * multi-gigabyte videos never need to fit in memory.
 *
 * File format:
 *   MAGIC ("MSTYENC1")
 *   repeated chunks: [1 byte ivLen][iv][4 bytes ciphertextLen][ciphertext+tag]
 */
public final class VaultCrypto {
    private static final String KEY_ALIAS = "misty_vault_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final byte[] MAGIC = {'M', 'S', 'T', 'Y', 'E', 'N', 'C', '1'};
    private static final int CHUNK_SIZE = 4 * 1024 * 1024;
    private static final int GCM_TAG_BITS = 128;

    private VaultCrypto() {
    }

    private static SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    /** Returns true when the stream starts with the vault encryption magic bytes. */
    public static boolean isEncryptedFile(java.io.File file) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            byte[] magic = new byte[MAGIC.length];
            readFully(in, magic);
            return java.util.Arrays.equals(magic, MAGIC);
        } catch (IOException e) {
            return false;
        }
    }

    public static void encrypt(InputStream in, OutputStream out) throws GeneralSecurityException, IOException {
        SecretKey key = getOrCreateKey();
        out.write(MAGIC);
        byte[] buffer = new byte[CHUNK_SIZE];
        while (true) {
            int read = fillBuffer(in, buffer);
            if (read <= 0) break;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(buffer, 0, read);
            out.write(iv.length);
            out.write(iv);
            writeInt(out, encrypted.length);
            out.write(encrypted);
            if (read < buffer.length) break;
        }
        out.flush();
    }

    public static void decrypt(InputStream in, OutputStream out) throws GeneralSecurityException, IOException {
        SecretKey key = getOrCreateKey();
        byte[] magic = new byte[MAGIC.length];
        readFully(in, magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            throw new IOException("Not a vault-encrypted file");
        }
        while (true) {
            int ivLen = in.read();
            if (ivLen < 0) break; // clean end of file
            byte[] iv = new byte[ivLen];
            readFully(in, iv);
            int chunkLen = readInt(in);
            if (chunkLen < 0 || chunkLen > CHUNK_SIZE + 64) {
                throw new IOException("Corrupt vault file (bad chunk length)");
            }
            byte[] encrypted = new byte[chunkLen];
            readFully(in, encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            out.write(cipher.doFinal(encrypted));
        }
        out.flush();
    }

    private static int fillBuffer(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read < 0) break;
            total += read;
        }
        return total;
    }

    private static void readFully(InputStream in, byte[] target) throws IOException {
        int total = 0;
        while (total < target.length) {
            int read = in.read(target, total, target.length - total);
            if (read < 0) throw new EOFException("Unexpected end of stream");
            total += read;
        }
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static int readInt(InputStream in) throws IOException {
        int b1 = in.read(), b2 = in.read(), b3 = in.read(), b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0) throw new EOFException("Unexpected end of stream");
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }
}
