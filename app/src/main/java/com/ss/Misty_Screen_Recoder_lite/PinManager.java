package com.ss.Misty_Screen_Recoder_lite;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Stores and verifies the vault PIN as a salted PBKDF2 hash.
 * The raw PIN is never persisted.
 */
public final class PinManager {
    private static final String PREFS_NAME = "vault_pin_prefs";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_HASH = "pin_hash";
    private static final int ITERATIONS = 12000;
    private static final int KEY_LENGTH_BITS = 256;

    private PinManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isPinSet(Context context) {
        return prefs(context).contains(KEY_HASH);
    }

    public static void setPin(Context context, String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(pin, salt);
        prefs(context).edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .apply();
    }

    public static boolean verifyPin(Context context, String pin) {
        SharedPreferences p = prefs(context);
        String saltB64 = p.getString(KEY_SALT, null);
        String hashB64 = p.getString(KEY_HASH, null);
        if (saltB64 == null || hashB64 == null) return false;
        byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
        byte[] expected = Base64.decode(hashB64, Base64.NO_WRAP);
        byte[] actual = hash(pin, salt);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] hash(String pin, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
