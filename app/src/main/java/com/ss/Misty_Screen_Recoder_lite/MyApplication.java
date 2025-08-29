package com.ss.Misty_Screen_Recoder_lite;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.UserMessagingPlatform;

public class MyApplication extends Application {
    private static MyApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
            // SDK initialization completed
        });
        // Request UMP consent as early as possible
        // Note: Requires Activity context to show the form; we just ensure consent info is prepared.
        // Actual form display will be triggered from first Activity.
        
        // Apply dark mode based on saved preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("key_dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    public static MyApplication getInstance() {
        return instance;
    }
} 