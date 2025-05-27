package com.ss.Misty_Screen_Recoder_lite;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;

public class FloatingDockService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private Handler timerHandler = new Handler();
    private int seconds = 0;
    private boolean running = true;
    private TextView timerText;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        addFloatingDock();
        startTimer();
    }

    private void addFloatingDock() {
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 100;

        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_dock, null);
        timerText = floatingView.findViewById(R.id.timer);

        Button btnPause = floatingView.findViewById(R.id.btn_pause);
        Button btnStop = floatingView.findViewById(R.id.btn_stop);

        btnPause.setOnClickListener(v -> {
            Intent intent = new Intent("com.ss.Misty_Screen_Recoder_lite.ACTION_PAUSE_RECORDING");
            sendBroadcast(intent);
        });
        btnStop.setOnClickListener(v -> {
            Intent intent = new Intent("com.ss.Misty_Screen_Recoder_lite.ACTION_STOP_RECORDING");
            sendBroadcast(intent);
        });

        windowManager.addView(floatingView, params);
    }

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (running) {
                    int mins = seconds / 60;
                    int secs = seconds % 60;
                    timerText.setText(String.format("%02d:%02d", mins, secs));
                    seconds++;
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (floatingView != null) windowManager.removeView(floatingView);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 