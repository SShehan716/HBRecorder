package com.ss.Misty_Screen_Recoder_lite;

import android.app.Application;

public class MyApplication extends Application {
    private static MyApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    
    public static MyApplication getInstance() {
        return instance;
    }
} 