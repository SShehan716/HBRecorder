package com.ss.Misty_Screen_Recoder_lite;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import android.widget.ImageView;
import com.ss.Misty_Screen_Recoder_lite.LogUtils;

public class FloatingDockService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private int secondsElapsed;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d("FloatingDockService", "Service onCreate called");
        
        // Check overlay permission before attempting to add floating view
        if (!checkOverlayPermission()) {
            LogUtils.e("FloatingDockService", "Overlay permission not granted, stopping service");
            stopSelf();
            return;
        }
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            LogUtils.e("FloatingDockService", "WindowManager is null, stopping service");
            stopSelf();
            return;
        }
        
        // Add a small delay to ensure service is fully initialized
        new Handler().postDelayed(() -> {
            try {
                addFloatingDock();
            } catch (Exception e) {
                LogUtils.e("FloatingDockService", "Error adding floating dock: " + e.getMessage(), e);
                stopSelf();
            }
        }, 100); // 100ms delay
    }

    private void addFloatingDock() {
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        Context themedContext = new android.view.ContextThemeWrapper(this, R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        floatingView = inflater.inflate(R.layout.floating_dock, null);

        ImageView recorderIcon = floatingView.findViewById(R.id.recorder_icon);
        LinearLayout recorderBar = floatingView.findViewById(R.id.recorder_bar);
        TextView timerText = floatingView.findViewById(R.id.timer_text);
        ImageView closeIcon = floatingView.findViewById(R.id.close_icon);

        final int[] downRawX = {0};
        final int[] downRawY = {0};
        final int CLICK_DRAG_TOLERANCE = 10;
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // Only show timer when expanded (recorderBar visible)
        recorderIcon.setOnClickListener(v -> {
            recorderIcon.setVisibility(View.GONE);
            recorderBar.setVisibility(View.VISIBLE);
        });
        closeIcon.setOnClickListener(v -> {
            recorderBar.setVisibility(View.GONE);
            recorderIcon.setVisibility(View.VISIBLE);
        });

        // Drag logic for both icon and bar
        View.OnTouchListener dragListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    downRawX[0] = (int) event.getRawX();
                    downRawY[0] = (int) event.getRawY();
                    v.setPressed(false);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    params.x = initialX + deltaX;
                    params.y = initialY + deltaY;
                    windowManager.updateViewLayout(floatingView, params);
                    if (Math.abs(deltaX) > CLICK_DRAG_TOLERANCE || Math.abs(deltaY) > CLICK_DRAG_TOLERANCE) {
                        v.setPressed(false);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    int upRawX = (int) event.getRawX();
                    int upRawY = (int) event.getRawY();
                    int upDeltaX = upRawX - downRawX[0];
                    int upDeltaY = upRawY - downRawY[0];
                    int middle = screenWidth / 2;
                    if (params.x + v.getWidth() / 2 < middle) {
                        params.x = 0;
                    } else {
                        params.x = screenWidth - v.getWidth();
                    }
                    windowManager.updateViewLayout(floatingView, params);
                    if (Math.abs(upDeltaX) < CLICK_DRAG_TOLERANCE && Math.abs(upDeltaY) < CLICK_DRAG_TOLERANCE) {
                        v.performClick();
                    }
                    return true;
            }
            return false;
        };
        recorderIcon.setOnTouchListener(dragListener);
        recorderBar.setOnTouchListener(dragListener);

        // Timer logic (always running, only visible when expanded)
        timerHandler = new Handler();
        secondsElapsed = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                int hours = secondsElapsed / 3600;
                int minutes = (secondsElapsed % 3600) / 60;
                int seconds = secondsElapsed % 60;
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                timerText.setText(time);
                secondsElapsed++;
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);

        // Start with only icon visible
        recorderIcon.setVisibility(View.VISIBLE);
        recorderBar.setVisibility(View.GONE);

        try {
            windowManager.addView(floatingView, params);
            LogUtils.d("FloatingDockService", "Floating dock view added to window manager successfully");
        } catch (Exception e) {
            LogUtils.e("FloatingDockService", "Error adding floating view to window manager: " + e.getMessage(), e);
            // Clean up resources
            floatingView = null;
            windowManager = null;
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("FloatingDockService", "onDestroy called - cleaning up resources");
        
        // Clean up timer resources
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler = null;
            timerRunnable = null;
        }
        
        // Clean up floating view
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                LogUtils.e("FloatingDockService", "Error removing floating view: " + e.getMessage());
            }
            floatingView = null;
        }
        
        // Clear references
        windowManager = null;
        
        LogUtils.d("FloatingDockService", "FloatingDockService cleanup completed");
    }

    /**
     * Check if overlay permission is granted
     */
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                return android.provider.Settings.canDrawOverlays(this);
            } catch (Exception e) {
                LogUtils.e("FloatingDockService", "Error checking overlay permission: " + e.getMessage());
                return false;
            }
        } else {
            // For older versions, assume permission is granted
            return true;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 