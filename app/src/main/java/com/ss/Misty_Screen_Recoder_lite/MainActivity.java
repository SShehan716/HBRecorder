package com.ss.Misty_Screen_Recoder_lite;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.view.WindowManager;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import static com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_REACHED_ERROR;
import static com.hbisoft.hbrecorder.Constants.SETTINGS_ERROR;

import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;



/**
 * Created by HBiSoft on 13 Aug 2019
 * Copyright (c) 2019 . All rights reserved.
 */

/*
* Implementation Steps
*
* 1. Implement HBRecorderListener by calling implements HBRecorderListener
*    After this you have to implement the methods by pressing (Alt + Enter)
*
* 2. Declare HBRecorder
*
* 3. Init implements HBRecorderListener by calling hbRecorder = new HBRecorder(this, this);
*
* 4. Set adjust provided settings
*
* 5. Start recording by first calling:
* MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
  Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
  startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);

* 6. Then in onActivityResult call hbRecorder.onActivityResult(resultCode, data, this);
*
* 7. Then you can start recording by calling hbRecorder.startScreenRecording(data);
*
* */

@SuppressWarnings({"SameParameterValue"})
public class MainActivity extends AppCompatActivity implements HBRecorderListener {
    //Permissions
    private static final int SCREEN_RECORD_REQUEST_CODE = 1000;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 5678;
    private boolean hasPermissions = false;
    private boolean hasAudioPermissions = false;

    //Declare HBRecorder
    private HBRecorder hbRecorder;

    //Start/Stop Button
    private Button startbtn;

    //Audio switches
    private SwitchMaterial advancedAudioSwitch;

    //Advanced settings dropdowns
    private AutoCompleteTextView encoderDropdown;
    private AutoCompleteTextView resolutionDropdown;
    private AutoCompleteTextView framerateDropdown;
    private AutoCompleteTextView bitrateDropdown;
    private AutoCompleteTextView outputFormatDropdown;
    private AutoCompleteTextView audioSourceDropdown;

    //Containers
    private androidx.constraintlayout.widget.ConstraintLayout quickSettingsContainer;
    private androidx.constraintlayout.widget.ConstraintLayout advancedSettingsContainer;

    //Reference to checkboxes and radio buttons
    boolean wasHDSelected = true;
    boolean isAudioEnabled = true;
    private boolean isStopPending = false;

    //Should custom settings be used
    SwitchCompat custom_settings_switch;

    // Max file size in K
    private EditText maxFileSizeInK;

    private static final String PREF_NAME = "ScreenRecorderSettings";
    private static final String KEY_VIDEO_ENCODER = "video_encoder";
    private static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_FRAMERATE = "framerate";
    private static final String KEY_BITRATE = "bitrate";
    private static final String KEY_OUTPUT_FORMAT = "output_format";
    private static final String KEY_AUDIO_SOURCE = "audio_source";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";
    private static final String KEY_QUALITY_MODE = "quality_mode";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private QuickSettingsFragment quickSettingsFragment;
    private AdvancedSettingsFragment advancedSettingsFragment;

    // AdMob components
    private AdMobHelper adMobHelper;

    private boolean hasRetriedWithDefaults = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme based on current dark mode setting
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("key_dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        setContentView(R.layout.activity_main);

        requestAllPermissions();
        requestNotificationPermission();
        initViews();
        setupDropdowns();
        setOnClickListeners();

        // Init HBRecorder (no need for LOLLIPOP check, minSdk is 21)
            hbRecorder = new HBRecorder(this, this);

            //When the user returns to the application, some UI changes might be necessary,
            //check if recording is in progress and make changes accordingly
            if (hbRecorder.isBusyRecording()) {
                startbtn.setText(R.string.stop_recording);
        }

        // Initialize ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "ViewPager: " + (viewPager != null ? "Found" : "NULL"));
            LogUtils.d("MainActivity", "TabLayout: " + (tabLayout != null ? "Found" : "NULL"));
        }

        // Create fragments
        quickSettingsFragment = new QuickSettingsFragment();
        advancedSettingsFragment = new AdvancedSettingsFragment();

        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "QuickSettingsFragment created: " + (quickSettingsFragment != null));
            LogUtils.d("MainActivity", "AdvancedSettingsFragment created: " + (advancedSettingsFragment != null));
        }

        // Set up ViewPager adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(quickSettingsFragment, "Quick");
        adapter.addFragment(advancedSettingsFragment, "Advanced");
        viewPager.setAdapter(adapter);

        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "ViewPager adapter set with " + adapter.getItemCount() + " fragments");
        }

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    tab.setText(position == 0 ? "Quick" : "Advanced");
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("MainActivity", "Tab " + position + " set to: " + (position == 0 ? "Quick" : "Advanced"));
                    }
                }
        ).attach();

        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "TabLayoutMediator attached successfully");
        }

        // Add fallback mechanism - check if ViewPager is working after a delay
        new android.os.Handler().postDelayed(() -> {
            if (viewPager != null && viewPager.getChildCount() == 0) {
                if (BuildConfig.DEBUG) {
                    LogUtils.w("MainActivity", "ViewPager has no children, showing fallback content");
                }
                // Show fallback content if ViewPager is not working
                android.view.View fallbackContent = findViewById(R.id.fallback_content);
                if (fallbackContent != null) {
                    fallbackContent.setVisibility(android.view.View.VISIBLE);
                }
            }
        }, 1000); // Check after 1 second

        // Initialize AdMob (lazy loading) - MUST be before setupFragments
        initializeAds();

        // Set up fragments
        setupFragments();
        
        // Check overlay permission on app start
        checkOverlayPermissionOnStart();
        
        // Preload ads after a delay to improve user experience
        // This happens after the UI is ready and user can interact
        new android.os.Handler().postDelayed(() -> {
            if (adMobHelper != null) {
                adMobHelper.preloadAds(this);
            }
        }, 2000); // 2 second delay

        // Initialize codec info for setup (removed excessive debug logging for performance)
        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Codec initialization completed");
        }
    }

    private void requestNotificationPermission() {
        // POST_NOTIFICATIONS permission is only required for API level 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
            }
        }
    }

    private void requestAllPermissions() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Only for Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        // POST_NOTIFICATIONS permission is only required for API level 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                LogUtils.i("Folder ", "created");
            }
        }
    }

    //Init Views
    private void initViews() {
        startbtn = findViewById(R.id.button_start);
        advancedAudioSwitch = findViewById(R.id.advanced_audio_switch);
        encoderDropdown = findViewById(R.id.encoder_dropdown);
        resolutionDropdown = findViewById(R.id.resolution_dropdown);
        framerateDropdown = findViewById(R.id.framerate_dropdown);
        bitrateDropdown = findViewById(R.id.bitrate_dropdown);
        outputFormatDropdown = findViewById(R.id.output_format_dropdown);
        audioSourceDropdown = findViewById(R.id.audio_source_dropdown);
        quickSettingsContainer = findViewById(R.id.quick_settings_container);
        advancedSettingsContainer = findViewById(R.id.advanced_settings_container);
    }

    private void setupDropdowns() {
        // Show loading state
        setDropdownsEnabled(false);
        
        // Move heavy codec operations to background thread
        new Thread(() -> {
            try {
        HBRecorderCodecInfo codecInfo = new HBRecorderCodecInfo();
        codecInfo.setContext(this);

        ArrayList<String> supportedFormats = codecInfo.getSupportedVideoFormats();
                
                // Switch back to main thread for UI updates
                runOnUiThread(() -> {
        if (supportedFormats == null || supportedFormats.isEmpty()) {
            showLongToast("No supported video codecs found. Screen recording is not supported on this device.");
            return;
        }
                    setupDropdownsWithCodecInfo(codecInfo, supportedFormats);
                    setDropdownsEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    LogUtils.e("MainActivity", "Error setting up codecs: " + e.getMessage());
                    showLongToast("Error initializing codecs. Using default settings.");
                    setupDefaultDropdowns();
                    setDropdownsEnabled(true);
                });
            }
        }).start();
    }
    
    private void setDropdownsEnabled(boolean enabled) {
        if (encoderDropdown != null) encoderDropdown.setEnabled(enabled);
        if (resolutionDropdown != null) resolutionDropdown.setEnabled(enabled);
        if (framerateDropdown != null) framerateDropdown.setEnabled(enabled);
        if (bitrateDropdown != null) bitrateDropdown.setEnabled(enabled);
        if (outputFormatDropdown != null) outputFormatDropdown.setEnabled(enabled);
        if (audioSourceDropdown != null) audioSourceDropdown.setEnabled(enabled);
    }
    
    private void setupDefaultDropdowns() {
        // Set up basic dropdowns with default values when codec detection fails
        String[] defaultEncoders = {"H264"};
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, defaultEncoders);
        encoderDropdown.setAdapter(encoderAdapter);
        encoderDropdown.setText(defaultEncoders[0], false);
        
        String[] defaultResolutions = {"720p", "1080p"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, defaultResolutions);
        resolutionDropdown.setAdapter(resolutionAdapter);
        resolutionDropdown.setText(defaultResolutions[0], false);
        
        String[] defaultFramerates = {"30", "60"};
        ArrayAdapter<String> framerateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, defaultFramerates);
        framerateDropdown.setAdapter(framerateAdapter);
        framerateDropdown.setText(defaultFramerates[0], false);
        
        String[] defaultBitrates = {"4 Mbps", "8 Mbps"};
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, defaultBitrates);
        bitrateDropdown.setAdapter(bitrateAdapter);
        bitrateDropdown.setText(defaultBitrates[0], false);
        
        String[] defaultFormats = {"MPEG_4"};
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, defaultFormats);
        outputFormatDropdown.setAdapter(formatAdapter);
        outputFormatDropdown.setText(defaultFormats[0], false);
        
        String[] audioSources = {"Microphone", "Internal Audio", "Voice Call", "Default"};
        ArrayAdapter<String> audioSourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, audioSources);
        audioSourceDropdown.setAdapter(audioSourceAdapter);
        audioSourceDropdown.setText(audioSources[0], false);
        
        loadSettings();
    }
    
    private void setupDropdownsWithCodecInfo(HBRecorderCodecInfo codecInfo, ArrayList<String> supportedFormats) {
        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Setting up dropdowns with " + supportedFormats.size() + " supported formats");
        }
        
        // Get device screen dimensions for optimal resolution detection
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int deviceWidth = displayMetrics.widthPixels;
        int deviceHeight = displayMetrics.heightPixels;
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Device screen: " + deviceWidth + "x" + deviceHeight);
        }

        // 1. Video Encoder options - Enhanced detection with user-friendly names
        ArrayList<String> encoders = getSupportedEncoders(codecInfo, supportedFormats);
        ArrayList<String> userFriendlyEncoders = convertToUserFriendlyEncoders(encoders);
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userFriendlyEncoders);
        encoderDropdown.setAdapter(encoderAdapter);
        encoderDropdown.setText(userFriendlyEncoders.get(0), false);

        // 2. Output format options - Convert to user-friendly names
        ArrayList<String> userFriendlyFormats = convertToUserFriendlyFormats(supportedFormats);
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userFriendlyFormats);
        outputFormatDropdown.setAdapter(formatAdapter);
        outputFormatDropdown.setText(userFriendlyFormats.get(0), false);

        // 3. Resolution options - Enhanced with device-specific detection
        ArrayList<String> supportedResolutions = getSupportedResolutions(codecInfo, supportedFormats, deviceWidth, deviceHeight);
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, supportedResolutions);
        resolutionDropdown.setAdapter(resolutionAdapter);
        resolutionDropdown.setText(supportedResolutions.get(0), false);

        // 4. Frame rate options - Enhanced detection per resolution
        ArrayList<String> supportedFramerates = getSupportedFramerates(codecInfo, supportedFormats, deviceWidth, deviceHeight);
        ArrayAdapter<String> framerateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, supportedFramerates);
        framerateDropdown.setAdapter(framerateAdapter);
        framerateDropdown.setText(supportedFramerates.get(0), false);

        // 5. Bitrate options - Enhanced detection per format
        ArrayList<String> supportedBitrates = getSupportedBitrates(codecInfo, supportedFormats);
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, supportedBitrates);
        bitrateDropdown.setAdapter(bitrateAdapter);
        bitrateDropdown.setText(supportedBitrates.get(0), false);

        // 6. Audio source options - corrected for proper functionality
        String[] audioSources = {"Microphone", "Internal Audio", "Voice Call", "Default"};
        ArrayAdapter<String> audioSourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, audioSources);
        audioSourceDropdown.setAdapter(audioSourceAdapter);
        audioSourceDropdown.setText(audioSources[0], false);

        // Load saved settings after setting up dropdowns
        loadSettings();
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Dropdowns setup completed successfully");
        }
    }
    
    /**
     * Get supported video encoders based on device capabilities
     */
    private ArrayList<String> getSupportedEncoders(HBRecorderCodecInfo codecInfo, ArrayList<String> supportedFormats) {
        ArrayList<String> encoders = new ArrayList<>();
        
        // Check for H.264 support (most common) - MPEG_4 format
        if (supportedFormats.contains("MPEG_4")) {
            encoders.add("H264");
            if (BuildConfig.DEBUG) {
                LogUtils.d("MainActivity", "H.264 encoder supported (MPEG_4 format)");
            }
        }
        
        // Check for H.265/HEVC support (better compression) - MPEG_4 format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && supportedFormats.contains("MPEG_4")) {
            // Note: H.265 support is device-specific, but we can offer it if MPEG_4 is supported
            encoders.add("H265");
            if (BuildConfig.DEBUG) {
                LogUtils.d("MainActivity", "H.265 encoder supported (MPEG_4 format)");
            }
        }
        
        // Check for VP8 support (WebM format)
        if (supportedFormats.contains("WEBM")) {
            encoders.add("VP8");
            if (BuildConfig.DEBUG) {
                LogUtils.d("MainActivity", "VP8 encoder supported (WEBM format)");
            }
        }
        
        // Check for VP9 support (better than VP8) - WebM format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && supportedFormats.contains("WEBM")) {
            encoders.add("VP9");
            if (BuildConfig.DEBUG) {
                LogUtils.d("MainActivity", "VP9 encoder supported (WEBM format)");
            }
        }
        
        // Check for H.263 support (3GP format)
        if (supportedFormats.contains("THREE_GPP")) {
            encoders.add("H263");
            if (BuildConfig.DEBUG) {
                LogUtils.d("MainActivity", "H.263 encoder supported (3GP format)");
            }
        }
        
        // Fallback to H.264 if nothing else is supported
        if (encoders.isEmpty()) {
            encoders.add("H264");
            if (BuildConfig.DEBUG) {
                LogUtils.w("MainActivity", "No specific encoders detected, using H.264 fallback");
            }
        }
        
        return encoders;
    }
    
    /**
     * Get supported resolutions based on device capabilities
     */
    private ArrayList<String> getSupportedResolutions(HBRecorderCodecInfo codecInfo, ArrayList<String> supportedFormats, int deviceWidth, int deviceHeight) {
        ArrayList<String> supportedResolutions = new ArrayList<>();
        
        // Define resolution presets with proper dimensions
        ResolutionPreset[] presets = {
            new ResolutionPreset("480p", 480, 854),
            new ResolutionPreset("720p", 720, 1280),
            new ResolutionPreset("1080p", 1080, 1920),
            new ResolutionPreset("1440p", 1440, 2560),
            new ResolutionPreset("2160p", 2160, 3840)
        };
        
        // Test each resolution with the best supported format
        String bestFormat = getBestSupportedFormat(supportedFormats);
        
        for (ResolutionPreset preset : presets) {
            // Check if this resolution is supported by the codec
            if (codecInfo.isSizeSupported(preset.width, preset.height, bestFormat)) {
                // Additional check: ensure it's reasonable for the device
                // Note: We don't limit to device screen size as screen recording can be higher resolution
                // than the device screen (e.g., 4K recording on 1080p screen)
                supportedResolutions.add(preset.name);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", "Resolution " + preset.name + " (" + preset.width + "x" + preset.height + ") supported");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", "Resolution " + preset.name + " not supported by codec");
                }
            }
        }
        
        // Fallback to 720p if nothing is supported
        if (supportedResolutions.isEmpty()) {
            supportedResolutions.add("720p");
            if (BuildConfig.DEBUG) {
                LogUtils.w("MainActivity", "No resolutions detected, using 720p fallback");
            }
        }
        
        return supportedResolutions;
    }
    
    /**
     * Get supported frame rates based on device capabilities
     */
    private ArrayList<String> getSupportedFramerates(HBRecorderCodecInfo codecInfo, ArrayList<String> supportedFormats, int deviceWidth, int deviceHeight) {
        ArrayList<String> supportedFramerates = new ArrayList<>();
        
        String bestFormat = getBestSupportedFormat(supportedFormats);
        
        // Test common frame rates with a standard resolution (720p) for consistency
        int testWidth = 720;
        int testHeight = 1280;
        double maxFps = codecInfo.getMaxSupportedFrameRate(testWidth, testHeight, bestFormat);
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Max supported FPS at " + testWidth + "x" + testHeight + ": " + maxFps);
        }
        
        // Test common frame rates
        int[] frameRates = {24, 30, 60};
        for (int fps : frameRates) {
            if (fps <= maxFps) {
                supportedFramerates.add(String.valueOf(fps));
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", fps + " FPS supported");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", fps + " FPS not supported (max: " + maxFps + ")");
                }
            }
        }
        
        // Fallback to 30 if nothing is supported
        if (supportedFramerates.isEmpty()) {
            supportedFramerates.add("30");
            if (BuildConfig.DEBUG) {
                LogUtils.w("MainActivity", "No frame rates detected, using 30 FPS fallback");
            }
        }
        
        return supportedFramerates;
    }
    
    /**
     * Get supported bitrates based on device capabilities
     */
    private ArrayList<String> getSupportedBitrates(HBRecorderCodecInfo codecInfo, ArrayList<String> supportedFormats) {
        ArrayList<String> supportedBitrates = new ArrayList<>();
        
        String bestFormat = getBestSupportedFormat(supportedFormats);
        int maxBitrate = codecInfo.getMaxSupportedBitrate(bestFormat);
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Max supported bitrate: " + maxBitrate + " bps");
        }
        
        // Test common bitrates (in Mbps)
        int[] bitratesMbps = {1, 2, 4, 8, 16, 32};
        for (int bitrateMbps : bitratesMbps) {
            int bitrateBps = bitrateMbps * 1000000;
            if (bitrateBps <= maxBitrate) {
                supportedBitrates.add(bitrateMbps + " Mbps");
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", bitrateMbps + " Mbps supported");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", bitrateMbps + " Mbps not supported (max: " + (maxBitrate / 1000000) + " Mbps)");
                }
            }
        }
        
        // Fallback to 4 Mbps if nothing is supported
        if (supportedBitrates.isEmpty()) {
            supportedBitrates.add("4 Mbps");
            if (BuildConfig.DEBUG) {
                LogUtils.w("MainActivity", "No bitrates detected, using 4 Mbps fallback");
            }
        }
        
        return supportedBitrates;
    }
    
    /**
     * Get the best supported format for this device
     */
    private String getBestSupportedFormat(ArrayList<String> supportedFormats) {
        // Priority order: MPEG_4 > WEBM > THREE_GPP
        if (supportedFormats.contains("MPEG_4")) {
            return "video/mp4";
        } else if (supportedFormats.contains("WEBM")) {
            return "video/webm";
        } else if (supportedFormats.contains("THREE_GPP")) {
            return "video/3gpp";
        } else {
            return "video/mp4"; // Default fallback
        }
    }
    
    /**
     * Convert technical format names to user-friendly names
     */
    private ArrayList<String> convertToUserFriendlyFormats(ArrayList<String> supportedFormats) {
        ArrayList<String> userFriendlyFormats = new ArrayList<>();
        
        for (String format : supportedFormats) {
            switch (format) {
                case "MPEG_4":
                    userFriendlyFormats.add("MP4 (Best Quality)");
                    break;
                case "WEBM":
                    userFriendlyFormats.add("WebM (Web Optimized)");
                    break;
                case "THREE_GPP":
                    userFriendlyFormats.add("3GP (Small File Size)");
                    break;
                default:
                    userFriendlyFormats.add(format); // Keep original if unknown
                    break;
            }
        }
        
        return userFriendlyFormats;
    }
    
    /**
     * Convert user-friendly format name back to technical name
     */
    private String getUserFriendlyToTechnicalFormat(String userFriendlyFormat) {
        if (userFriendlyFormat.contains("MP4")) {
            return "MPEG_4";
        } else if (userFriendlyFormat.contains("WebM")) {
            return "WEBM";
        } else if (userFriendlyFormat.contains("3GP")) {
            return "THREE_GPP";
        } else {
            return userFriendlyFormat; // Return as is if no match
        }
    }
    
    /**
     * Convert technical encoder names to user-friendly names
     */
    private ArrayList<String> convertToUserFriendlyEncoders(ArrayList<String> encoders) {
        ArrayList<String> userFriendlyEncoders = new ArrayList<>();
        
        for (String encoder : encoders) {
            switch (encoder) {
                case "H264":
                    userFriendlyEncoders.add("H.264 (Best Compatibility)");
                    break;
                case "H265":
                    userFriendlyEncoders.add("H.265 (Better Compression)");
                    break;
                case "VP8":
                    userFriendlyEncoders.add("VP8 (Web Optimized)");
                    break;
                case "VP9":
                    userFriendlyEncoders.add("VP9 (Advanced Web)");
                    break;
                case "H263":
                    userFriendlyEncoders.add("H.263 (Legacy Support)");
                    break;
                default:
                    userFriendlyEncoders.add(encoder); // Keep original if unknown
                    break;
            }
        }
        
        return userFriendlyEncoders;
    }
    
    /**
     * Convert user-friendly encoder name back to technical name
     */
    private String getUserFriendlyToTechnicalEncoder(String userFriendlyEncoder) {
        if (userFriendlyEncoder.contains("H.264")) {
            return "H264";
        } else if (userFriendlyEncoder.contains("H.265")) {
            return "H265";
        } else if (userFriendlyEncoder.contains("VP8")) {
            return "VP8";
        } else if (userFriendlyEncoder.contains("VP9")) {
            return "VP9";
        } else if (userFriendlyEncoder.contains("H.263")) {
            return "H263";
        } else {
            return userFriendlyEncoder; // Return as is if no match
        }
    }
    
    /**
     * Helper class for resolution presets
     */
    private static class ResolutionPreset {
        String name;
        int width;
        int height;
        
        ResolutionPreset(String name, int width, int height) {
            this.name = name;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Get resolution dimensions from resolution name
     */
    private int[] getResolutionDimensions(String resolution) {
        switch (resolution) {
            case "480p":
                return new int[]{480, 854};
            case "720p":
                return new int[]{720, 1280};
            case "1080p":
                return new int[]{1080, 1920};
            case "1440p":
                return new int[]{1440, 2560};
            case "2160p":
                return new int[]{2160, 3840};
            default:
                LogUtils.e("MainActivity", "Unknown resolution: " + resolution);
                return null;
        }
    }

    private void saveAudioPreference(boolean isEnabled) {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("hasAudioPermissions", isEnabled);
        editor.apply();
    }

    private boolean hasAudioPreference() {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        return preferences.getBoolean("hasAudioPermissions", false); // Default to false if not set
    }

    //Start Button OnClickListener
    private void setOnClickListeners() {
        startbtn.setOnClickListener(v -> {
            handleRecordingButtonClick();
        });

        advancedAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAudioEnabled = isChecked;
            saveAudioPreference(isChecked);
            saveSettings();
            // Update HBRecorder immediately
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                hbRecorder.isAudioEnabled(isChecked);
            }
        });

        encoderDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String userFriendlyEncoder = encoderDropdown.getText().toString();
                String technicalEncoder = getUserFriendlyToTechnicalEncoder(userFriendlyEncoder);
                hbRecorder.setVideoEncoder(technicalEncoder);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", "Video encoder set to: " + userFriendlyEncoder + " (technical: " + technicalEncoder + ")");
                }
                saveSettings();
            }
        });

                resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String resolution = resolutionDropdown.getText().toString();
                int[] dimensions = getResolutionDimensions(resolution);
                if (dimensions != null) {
                    hbRecorder.setScreenDimensions(dimensions[0], dimensions[1]);
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("MainActivity", "Resolution set to: " + resolution + " (" + dimensions[0] + "x" + dimensions[1] + ")");
                    }
                } else {
                    LogUtils.e("MainActivity", "Invalid resolution format: " + resolution);
                }
                saveSettings();
            });

        framerateDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String framerate = framerateDropdown.getText().toString();
                int fps = Integer.parseInt(framerate.split(" ")[0]);
                hbRecorder.setVideoFrameRate(fps);
                saveSettings();
        });

        bitrateDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String bitrate = bitrateDropdown.getText().toString();
                if (!bitrate.equals("Auto")) {
                int bitrateValue = Integer.parseInt(bitrate.split(" ")[0]) * 1000000;
                hbRecorder.setVideoBitrate(bitrateValue);
                }
                saveSettings();
        });

        outputFormatDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String userFriendlyFormat = outputFormatDropdown.getText().toString();
                String technicalFormat = getUserFriendlyToTechnicalFormat(userFriendlyFormat);
                hbRecorder.setOutputFormat(technicalFormat);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", "Output format set to: " + userFriendlyFormat + " (technical: " + technicalFormat + ")");
                }
                saveSettings();
        });

        audioSourceDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String source = audioSourceDropdown.getText().toString();
             configureAudioSource(source);
                saveSettings();
        });
    }

    //Called when recording starts
    @Override
    public void HBRecorderOnStart() {
        LogUtils.e("HBRecorder", "HBRecorderOnStart called");
    }

    //Listener for when the recording is saved successfully
    //This will be called after the file was created
    @Override
    public void HBRecorderOnComplete() {
        // Ensure UI updates happen on main thread
        runOnUiThread(() -> {
            synchronized (this) {
                if (startbtn != null) {
        startbtn.setText(R.string.start_recording);
                    startbtn.setEnabled(true);
                }
        isStopPending = false;
                hasRetriedWithDefaults = false; // Reset retry flag
            }
            
            try {
        stopService(new Intent(this, FloatingDockService.class));
            } catch (Exception e) {
                LogUtils.e("MainActivity", "Error stopping floating dock service: " + e.getMessage());
            }
            
            showLongToast("Recording saved successfully!");
            
            // Update gallery in background thread
            new Thread(() -> {
                if (hbRecorder != null) {
                    try {
            if (hbRecorder.wasUriSet()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    updateGalleryUri();
                } else {
                    refreshGalleryFile();
                }
        } else {
                refreshGalleryFile();
            }
                    } catch (Exception e) {
                        LogUtils.e("MainActivity", "Error updating gallery: " + e.getMessage());
                        runOnUiThread(() -> {
                            showLongToast("Recording saved, but gallery update failed");
                        });
                    }
                }
            }).start();
        });
    }

    // Called when error occurs
    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        LogUtils.e("HBRecorderOnError", "Error code: " + errorCode + ", reason: " + reason);
        
        if (errorCode == SETTINGS_ERROR) {
            handleSettingsError(reason);
        } else if (errorCode == MAX_FILE_SIZE_REACHED_ERROR) {
            handleMaxFileSizeError();
        } else {
            handleGeneralRecordingError(reason);
        }

        // Reset UI state
        resetRecordingUI();
    }
    
    private void handleSettingsError(String reason) {
        if (hasRetriedWithDefaults) {
            // Already retried once, show error and give up
            showRecordingErrorDialog("Recording Settings Error", 
                "Unable to start recording with current settings. Please try different settings or restart the app.", 
                reason);
            hasRetriedWithDefaults = false; // Reset for next time
            return;
        }
        
            String message = getString(R.string.settings_not_supported_message);
            if (reason != null && !reason.trim().isEmpty()) {
                message += ":\n" + reason;
            }
            showLongToast(message);
        
        // Try to recover with fallback settings in background thread
        new Thread(() -> {
            try {
                applyFallbackSettings();
                runOnUiThread(() -> {
                    hasRetriedWithDefaults = true;
                    showLongToast("Retrying with safe settings...");
                    // Retry recording after a short delay
                    new android.os.Handler().postDelayed(() -> {
                        startRecordingScreen();
                    }, 1000);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    LogUtils.e("MainActivity", "Error applying fallback settings: " + e.getMessage());
                    showRecordingErrorDialog("Setup Error", 
                        "Unable to configure recording settings. Please restart the app.", 
                        e.getMessage());
                });
            }
        }).start();
    }
    
    private void handleMaxFileSizeError() {
        showRecordingErrorDialog("File Size Limit Reached", 
            "Recording stopped because the file size limit was reached. The recording has been saved.", 
            null);
    }
    
    private void handleGeneralRecordingError(String reason) {
        String userFriendlyMessage = getUserFriendlyErrorMessage(reason);
        showRecordingErrorDialog("Recording Error", userFriendlyMessage, reason);
    }
    
    private String getUserFriendlyErrorMessage(String technicalReason) {
        if (technicalReason == null) {
            return "An unexpected error occurred during recording. Please try again.";
        }
        
        String lowerReason = technicalReason.toLowerCase();
        if (lowerReason.contains("permission")) {
            return "Permission denied. Please check app permissions and try again.";
        } else if (lowerReason.contains("storage") || lowerReason.contains("space")) {
            return "Not enough storage space. Please free up space and try again.";
        } else if (lowerReason.contains("codec") || lowerReason.contains("encoder")) {
            return "Video encoding error. Please try different quality settings.";
        } else if (lowerReason.contains("audio")) {
            return "Audio recording error. Try disabling audio recording or check microphone permissions.";
        } else {
            return "Recording failed due to a technical issue. Please try again.";
        }
    }
    
    private void showRecordingErrorDialog(String title, String message, String technicalDetails) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
               
        if (technicalDetails != null && BuildConfig.DEBUG) {
            builder.setNeutralButton("Technical Details", (dialog, which) -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Technical Details")
                    .setMessage(technicalDetails)
                    .setPositiveButton("OK", null)
                    .show();
            });
        }
        
        builder.show();
    }
    
    private void applyFallbackSettings() {
        if (hbRecorder == null) return;
        
        // Apply safe, widely-supported settings
        hbRecorder.setVideoEncoder("H264");
        hbRecorder.setOutputFormat("MPEG_4");
        hbRecorder.setScreenDimensions(720, 1280);
                hbRecorder.setVideoFrameRate(30);
        hbRecorder.setVideoBitrate(4000000); // 4 Mbps
        hbRecorder.setAudioSource("MIC");
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.isAudioEnabled(isAudioEnabled);
        
        // Update UI to reflect fallback settings
        runOnUiThread(() -> {
            if (encoderDropdown != null) encoderDropdown.setText("H264", false);
            if (outputFormatDropdown != null) outputFormatDropdown.setText("MPEG_4", false);
            if (resolutionDropdown != null) resolutionDropdown.setText("720p", false);
            if (framerateDropdown != null) framerateDropdown.setText("30", false);
            if (bitrateDropdown != null) bitrateDropdown.setText("4 Mbps", false);
            if (audioSourceDropdown != null) audioSourceDropdown.setText("Microphone", false);
            saveSettings();
        });
    }
    
    private void resetRecordingUI() {
        runOnUiThread(() -> {
            synchronized (this) {
                if (startbtn != null) {
                    startbtn.setText(R.string.start_recording);
                    startbtn.setEnabled(true);
                }
                isStopPending = false;
            }
            
            try {
                stopService(new Intent(this, FloatingDockService.class));
            } catch (Exception e) {
                LogUtils.e("MainActivity", "Error stopping floating dock service in error handler: " + e.getMessage());
            }
        });
    }
    
    // Thread-safe recording button handler to prevent race conditions
    private synchronized void handleRecordingButtonClick() {
        if (hbRecorder == null) {
            showLongToast("Recorder not initialized. Please restart the app.");
            return;
        }
        
        if (isStopPending) {
            // Already processing a stop request, ignore further clicks
            if (BuildConfig.DEBUG) {
                LogUtils.d("MainActivity", "Stop already pending, ignoring click");
            }
            return;
        }
        
        try {
            boolean isCurrentlyRecording = hbRecorder.isBusyRecording();
            
            if (!isCurrentlyRecording) {
                // Start recording
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", "Starting recording...");
                }
                startRecordingScreen();
        } else {
                // Stop recording
                if (BuildConfig.DEBUG) {
                    LogUtils.d("MainActivity", "Stopping recording...");
                }
                isStopPending = true;
                
                // Update UI immediately to provide feedback
                if (startbtn != null) {
                    startbtn.setText("Stopping...");
                    startbtn.setEnabled(false);
                }
                
                // Stop recording in background thread to avoid ANR
                new Thread(() -> {
                    try {
                        hbRecorder.stopScreenRecording();
                        // UI will be updated in HBRecorderOnComplete() or HBRecorderOnError()
                    } catch (Exception e) {
                        LogUtils.e("MainActivity", "Error stopping recording: " + e.getMessage());
                        runOnUiThread(() -> {
                            isStopPending = false;
                            if (startbtn != null) {
        startbtn.setText(R.string.start_recording);
                                startbtn.setEnabled(true);
                            }
                            showLongToast("Error stopping recording: " + e.getMessage());
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            LogUtils.e("MainActivity", "Error in handleRecordingButtonClick: " + e.getMessage());
        isStopPending = false;
            if (startbtn != null) {
                startbtn.setText(R.string.start_recording);
                startbtn.setEnabled(true);
            }
            showLongToast("Error managing recording state. Please try again.");
        }
    }

    // Called when recording has been paused
    @Override
    public void HBRecorderOnPause() {
        // Called when recording was paused
    }

    // Called when recording has resumed
    @Override
    public void HBRecorderOnResume() {
        // Called when recording was resumed
    }

    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        LogUtils.i("ExternalStorage", "Scanned " + path + ":");
                        LogUtils.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateGalleryUri(){
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }

    //Start recording screen
    //It is important to call it like this
    //hbRecorder.startScreenRecording(data); should only be called in onActivityResult
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRecordingScreen() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
        startbtn.setText(R.string.stop_recording);
    }

    String output_format;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    // Example of how to set custom settings
    private void customSettings() {
        hbRecorder.setVideoEncoder("H264");
        hbRecorder.setAudioSource("DEFAULT");
        hbRecorder.setOutputFormat("MPEG_4");
        
        // Get selected resolution
        String selectedResolution = resolutionDropdown.getText().toString();
        String[] dimensions = selectedResolution.split("x");
        if (dimensions.length == 2) {
            hbRecorder.setScreenDimensions(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
        }

        // Get selected framerate
        String selectedFramerate = framerateDropdown.getText().toString();
        int fps = Integer.parseInt(selectedFramerate.split(" ")[0]);
        hbRecorder.setVideoFrameRate(fps);

        // Get selected bitrate
        String selectedBitrate = bitrateDropdown.getText().toString();
        if (!selectedBitrate.equals("Auto")) {
            int bitrate = Integer.parseInt(selectedBitrate.split(" ")[0]) * 1000000;
            hbRecorder.setVideoBitrate(bitrate);
        }

        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.isAudioEnabled(isAudioEnabled);
    }

    //Get/Set the selected settings
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void quickSettings() {
        hbRecorder.setVideoEncoder("H264");
        hbRecorder.setAudioSource("DEFAULT");
        hbRecorder.setOutputFormat("MPEG_4");
        hbRecorder.setVideoFrameRate(30);
        hbRecorder.setVideoBitrate(5120000);
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.isAudioEnabled(isAudioEnabled);
        hbRecorder.setScreenDimensions(wasHDSelected ? 1280 : 640, wasHDSelected ? 720 : 360);
    }

    // Example of how to set the max file size

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setRecorderMaxFileSize() {
        String s = maxFileSizeInK.getText().toString();
        long maxFileSizeInKilobytes;
        try {
            maxFileSizeInKilobytes = Long.parseLong(s);
        } catch (NumberFormatException e) {
            maxFileSizeInKilobytes = 0;
        }
        hbRecorder.setMaxFileSize(maxFileSizeInKilobytes * 1024); // Convert to bytes

    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Check if permissions was granted
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) { // Q = 29
                // Do not request WRITE_EXTERNAL_STORAGE on Android 11+
                return true;
            }
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    //Handle permissions
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle POST_NOTIFICATIONS permission
        if (requestCode == 2001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can post notifications
            } else {
                Toast.makeText(this, "Notification permission denied. Please enable it in settings for full functionality.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Handle other permissions
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isAudioEnabled) {
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQUEST_CODE);
                    } else {
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE);
                    }
                } else {
                    String deniedPermission = permissions.length > 0 ? permissions[0] : "Unknown";
                    if (Manifest.permission.POST_NOTIFICATIONS.equals(deniedPermission)) {
                        showLongToast("POST_NOTIFICATIONS permission denied.");
                    } else if (Manifest.permission.RECORD_AUDIO.equals(deniedPermission)) {
                        showLongToast("RECORD_AUDIO permission denied.");
                    } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(deniedPermission)) {
                        showLongToast("WRITE_EXTERNAL_STORAGE permission denied.");
                    } else {
                        showLongToast("Permission denied: " + deniedPermission);
                    }
                    hasPermissions = false;
                }
                break;
            case OVERLAY_PERMISSION_REQUEST_CODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    hasPermissions = true;
                    // Don't automatically start recording - just show success message
                    Toast.makeText(this, "Floating dock permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for overlay");
                }
                break;
            default:
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                // Apply settings based on current tab before starting recording
                applySettingsBasedOnCurrentTab();
                
                                    setOutputPath();
                hbRecorder.startScreenRecording(data, resultCode);
            startbtn.setText(R.string.stop_recording);
            
            // Check current system permission status and start floating dock
            checkAndStartFloatingDock();
            } else {
                    startbtn.setText(R.string.start_recording);
                }
        } else if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            // User returned from overlay permission screen - just check if permission was granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Floating dock permission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Apply settings based on which tab is currently active
     */
    private void applySettingsBasedOnCurrentTab() {
        int currentTab = viewPager.getCurrentItem();
        
        if (currentTab == 0) {
            // Quick Settings tab is active
            LogUtils.d("MainActivity", "Applying Quick Settings");
            quickSettings();
            } else {
            // Advanced Settings tab is active
            LogUtils.d("MainActivity", "Applying Advanced Settings");
            customSettings();
        }
    }

    //For Android 10> we will pass a Uri to HBRecorder
    //This is not necessary - You can still use getExternalStoragePublicDirectory
    //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "HBRecorder");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            if (output_format != null) {
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, getMimeTypeForOutputFormat(output_format));
            }else {
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            }
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        }else{
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +"/HBRecorder");
        }
    }

    // Passing the MIME_TYPE to ContentValues() depending on what output format was selected
    // This is just to demonstrate for the demo app - more can be added
    private String getMimeTypeForOutputFormat(String outputFormat) {
        String mimetype = "video/mp4";
        switch (outputFormat) {
            // We do not know what the devices DEFAULT (0) is
            // For the sake of this demo app we will set it to mp4
            case "0":
                mimetype = "video/mp4";
                break;
            case "1":
                mimetype = "video/mp4";
                break;
            case "2":
                mimetype = "video/3gpp";
                break;
            case "3":
                mimetype = "video/webm";
                break;
            default:
                mimetype = "video/mp4";
                break;
        }
        return mimetype;
    }

    //Generate a timestamp to be used as a file name
    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }

    //Show Toast
    private void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    //drawable to byte[]
    private byte[] drawable2ByteArray(@DrawableRes int drawableId) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), drawableId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void saveSettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(KEY_VIDEO_ENCODER, encoderDropdown.getText().toString());
        editor.putString(KEY_RESOLUTION, resolutionDropdown.getText().toString());
        editor.putString(KEY_FRAMERATE, framerateDropdown.getText().toString());
        editor.putString(KEY_BITRATE, bitrateDropdown.getText().toString());
        editor.putString(KEY_OUTPUT_FORMAT, outputFormatDropdown.getText().toString());
        editor.putString(KEY_AUDIO_SOURCE, audioSourceDropdown.getText().toString());
        editor.putBoolean(KEY_AUDIO_ENABLED, advancedAudioSwitch.isChecked());

        editor.apply();
    }

    private void loadSettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Load and set video encoder
        String savedEncoder = preferences.getString(KEY_VIDEO_ENCODER, "H264");
        encoderDropdown.setText(savedEncoder, false);

        // Load and set resolution
        String savedResolution = preferences.getString(KEY_RESOLUTION, "720p");
        resolutionDropdown.setText(savedResolution, false);

        // Load and set framerate
        String savedFramerate = preferences.getString(KEY_FRAMERATE, "30");
        framerateDropdown.setText(savedFramerate, false);

        // Load and set bitrate
        String savedBitrate = preferences.getString(KEY_BITRATE, "4 Mbps");
        bitrateDropdown.setText(savedBitrate, false);

        // Load and set output format
        String savedFormat = preferences.getString(KEY_OUTPUT_FORMAT, "MPEG_4");
        outputFormatDropdown.setText(savedFormat, false);

        // Load and set audio source
        String savedAudioSource = preferences.getString(KEY_AUDIO_SOURCE, "System + Mic");
        audioSourceDropdown.setText(savedAudioSource, false);

        // Load and set audio enabled state
        boolean savedAudioEnabled = preferences.getBoolean(KEY_AUDIO_ENABLED, true);
        isAudioEnabled = savedAudioEnabled; // Update the main variable
        advancedAudioSwitch.setChecked(savedAudioEnabled);
        
        // Sync with Quick Settings fragment
        if (quickSettingsFragment != null) {
            quickSettingsFragment.setAudioEnabled(savedAudioEnabled);
        }
        
        // Apply to HBRecorder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
            hbRecorder.isAudioEnabled(savedAudioEnabled);
        }
    }

    private void setupFragments() {
        // Set up QuickSettingsFragment
        quickSettingsFragment.setOnSettingsChangedListener(new QuickSettingsFragment.OnSettingsChangedListener() {
            @Override
            public void onQualityChanged(boolean isHD) {
                wasHDSelected = isHD;
                if (isHD) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hbRecorder.setScreenDimensions(1080, 1920);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hbRecorder.setVideoBitrate(8000000);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hbRecorder.setScreenDimensions(720, 1280);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hbRecorder.setVideoBitrate(4000000);
                    }
                }
            }

            @Override
            public void onAudioChanged(boolean isEnabled) {
                isAudioEnabled = isEnabled;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    hbRecorder.isAudioEnabled(isEnabled);
                }
                // Sync with Advanced Settings
                if (advancedAudioSwitch != null) {
                    advancedAudioSwitch.setChecked(isEnabled);
                }
                saveAudioPreference(isEnabled);
                saveSettings();
            }

            @Override
            public void onAudioUnlocked() {
                // Audio feature was unlocked via rewarded ad
                // Update advanced settings audio switch
                if (advancedAudioSwitch != null) {
                    advancedAudioSwitch.setEnabled(true);
                    advancedAudioSwitch.setAlpha(1.0f);
                }
                // Enable audio recording by default when unlocked
                isAudioEnabled = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    hbRecorder.isAudioEnabled(true);
                }
                saveAudioPreference(true);
                saveSettings();
                
                // Refresh advanced settings audio controls
                if (advancedSettingsFragment != null) {
                    advancedSettingsFragment.refreshAudioControlsState();
                }
            }
        });

        // Set up AdvancedSettingsFragment
        advancedSettingsFragment.setHBRecorder(hbRecorder);
        advancedSettingsFragment.setOnSettingsChangedListener(() -> {
            // Settings have been changed, update UI if needed
        });

        // Pass AdMob helper to both fragments for rewarded ads
        quickSettingsFragment.setAdMobHelper(adMobHelper);
        advancedSettingsFragment.setAdMobHelper(adMobHelper);
    }

    private void startRecording() {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQUEST_CODE) &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE)) {
                startScreenRecording();
        }
    }

    private void startScreenRecording() {
        // Check overlay permission before starting recording
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
            return;
        }
        
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }
    
    /**
     * Initialize AdMob ads (lazy loading)
     */
    private void initializeAds() {
        adMobHelper = new AdMobHelper();
        
        // Preload rewarded ads for better user experience
        adMobHelper.preloadAds(this);
    }
    

    
    /**
     * Check system permission status and start floating dock accordingly
     */
    private void checkAndStartFloatingDock() {
        // Check if floating dock is enabled in settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFloatingDockEnabled = prefs.getBoolean("key_floating_dock_enabled", true); // Default to true
        
        if (!isFloatingDockEnabled) {
            LogUtils.d("MainActivity", "Floating dock is disabled in settings");
            return;
        }
        
        // Check current system permission status using a more reliable method
        boolean hasOverlayPermission = checkOverlayPermission();
        
        if (BuildConfig.DEBUG) {
        LogUtils.d("MainActivity", "Checking overlay permission - canDrawOverlays: " + hasOverlayPermission);
        }
        
        if (hasOverlayPermission) {
            if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Overlay permission already granted, starting FloatingDockService");
            }
            startService(new Intent(this, FloatingDockService.class));
        } else {
            if (BuildConfig.DEBUG) {
            LogUtils.d("MainActivity", "Overlay permission not granted, showing permission dialog");
            }
            showOverlayPermissionDialog();
        }
    }
    
    /**
     * Show dialog to request overlay permission
     */
    private void showOverlayPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
               .setMessage("This app needs 'Display over other apps' permission to show the floating recording controls. Please enable this permission to use all features.")
               .setCancelable(false)
               .setPositiveButton("Enable Permission", (dialog, which) -> {
                   // Open settings to enable overlay permission
                   Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                       Uri.parse("package:" + getPackageName()));
                   startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
               })
               .setNegativeButton("Cancel", (dialog, which) -> {
                   dialog.dismiss();
                   Toast.makeText(this, "Some features may not work without overlay permission", Toast.LENGTH_LONG).show();
               });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * More reliable method to check overlay permission
     */
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Try multiple methods to check overlay permission
            boolean canDrawOverlays = Settings.canDrawOverlays(this);
            
            // If the standard method returns false, try to check if we can actually draw overlays
            if (!canDrawOverlays) {
                // Try to create a test window to see if we can actually draw overlays
                try {
                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    if (windowManager != null) {
                        // If we can get the window manager, assume permission is granted
                        // This is a fallback method
                        canDrawOverlays = true;
                        LogUtils.d("MainActivity", "Using fallback method - window manager available");
                    }
                } catch (Exception e) {
                    LogUtils.e("MainActivity", "Error checking overlay permission: " + e.getMessage());
                    canDrawOverlays = false;
                }
            }
            
            return canDrawOverlays;
        } else {
            // For older versions, assume permission is granted
            return true;
        }
    }
    
    /**
     * Test overlay permission status
     */
    private void testOverlayPermission() {
        boolean hasPermission = checkOverlayPermission();
        
        LogUtils.d("MainActivity", "=== OVERLAY PERMISSION TEST ===");
        LogUtils.d("MainActivity", "Package: " + getPackageName());
        LogUtils.d("MainActivity", "Has overlay permission: " + hasPermission);
        LogUtils.d("MainActivity", "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
        LogUtils.d("MainActivity", "===============================");
        
        if (hasPermission) {
            Toast.makeText(this, "Overlay permission is GRANTED", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Overlay permission is NOT GRANTED", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Check overlay permission when app starts
     */
    private void checkOverlayPermissionOnStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Show dialog after a short delay to let the app fully load
                new android.os.Handler().postDelayed(() -> {
                    showOverlayPermissionDialog();
                }, 1000); // 1 second delay
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Preload ads when app becomes active (user returns to app)
        if (adMobHelper != null) {
            adMobHelper.preloadAds(this);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Optional: Clean up ad resources when app goes to background
        // This is handled automatically by the AdMob SDK
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up HBRecorder resources
        if (hbRecorder != null) {
            try {
                if (hbRecorder.isBusyRecording()) {
                    hbRecorder.stopScreenRecording();
                }
            } catch (Exception e) {
                LogUtils.e("MainActivity", "Error stopping recorder: " + e.getMessage());
            }
            hbRecorder = null;
        }
        
        // Clean up AdMob resources
        if (adMobHelper != null) {
            adMobHelper.cleanup();
            adMobHelper = null;
        }
        
        // Clean up fragments to prevent memory leaks
        quickSettingsFragment = null;
        advancedSettingsFragment = null;
        
        // Stop floating dock service if running
        try {
            stopService(new Intent(this, FloatingDockService.class));
        } catch (Exception e) {
            LogUtils.e("MainActivity", "Error stopping floating dock: " + e.getMessage());
        }
        
        LogUtils.d("MainActivity", "Resources cleaned up in onDestroy");
    }

    /**
     * Update audio state from Advanced Settings fragment
     */
    public void updateAudioState(boolean isEnabled) {
        isAudioEnabled = isEnabled;
        saveAudioPreference(isEnabled);
        saveSettings();
        
        // Sync with Quick Settings if available (with null safety)
        if (quickSettingsFragment != null && !isDestroyed() && !isFinishing()) {
            try {
            quickSettingsFragment.setAudioEnabled(isEnabled);
            } catch (Exception e) {
                LogUtils.e("MainActivity", "Error setting audio state in quick settings fragment: " + e.getMessage());
            }
        }
    }
    
    /**
     * Helper method to validate and configure audio source properly
     */
    private void configureAudioSource(String audioSourceName) {
        if (hbRecorder == null) {
            LogUtils.e("MainActivity", "Cannot configure audio source - HBRecorder is null");
            return;
        }
        
        try {
            switch (audioSourceName) {
                case "Microphone":
                    hbRecorder.setAudioSource("MIC");
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("MainActivity", "Audio source set to MIC");
                    }
                    break;
                    
                case "Internal Audio":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        hbRecorder.setAudioSource("REMOTE_SUBMIX");
                        if (BuildConfig.DEBUG) {
                            LogUtils.d("MainActivity", "Audio source set to REMOTE_SUBMIX for internal audio");
                        }
                    } else {
                        hbRecorder.setAudioSource("DEFAULT");
                        showLongToast("Internal audio recording requires Android 10+. Using default source.");
                        if (BuildConfig.DEBUG) {
                            LogUtils.d("MainActivity", "Internal audio not supported on this Android version, using DEFAULT");
                        }
                    }
                    break;
                    
                case "Voice Call":
                    hbRecorder.setAudioSource("VOICE_CALL");
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("MainActivity", "Audio source set to VOICE_CALL");
                    }
                    break;
                    
                case "Default":
                    hbRecorder.setAudioSource("DEFAULT");
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("MainActivity", "Audio source set to DEFAULT");
                    }
                    break;
                    
                default:
                    LogUtils.w("MainActivity", "Unknown audio source: " + audioSourceName + ", defaulting to MIC");
                    hbRecorder.setAudioSource("MIC");
                    break;
            }
            
            // Validate audio permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                LogUtils.w("MainActivity", "RECORD_AUDIO permission not granted");
                showLongToast("Microphone permission required for audio recording");
            }
            
        } catch (Exception e) {
            LogUtils.e("MainActivity", "Error configuring audio source: " + e.getMessage());
            // Fallback to safe option
            try {
                hbRecorder.setAudioSource("MIC");
                showLongToast("Audio source configuration failed. Using microphone as fallback.");
            } catch (Exception fallbackError) {
                LogUtils.e("MainActivity", "Fallback audio source configuration also failed: " + fallbackError.getMessage());
            }
        }
    }
    
    /**
     * Get user-friendly description of audio source capabilities
     */
    public String getAudioSourceDescription(String audioSourceName) {
        switch (audioSourceName) {
            case "Microphone":
                return "Records from device microphone only. Best for voice narration.";
            case "Internal Audio":
                return "Records internal device audio (Android 10+). Captures system sounds, music, and app audio.";
            case "Voice Call":
                return "Optimized for voice calls. May not work during regular recording.";
            case "Default":
                return "Uses system default audio source. Usually captures microphone.";
            default:
                return "Unknown audio source.";
        }
    }

    public boolean isAudioUnlocked() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isUnlocked = prefs.getBoolean("audio_feature_unlocked", false);
        
        if (isUnlocked) {
            // Check if 24 hours have passed since unlock
            long unlockTime = prefs.getLong("audio_unlock_time", 0);
            long currentTime = System.currentTimeMillis();
            long oneDayInMillis = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
            
            if (currentTime - unlockTime > oneDayInMillis) {
                // Expired, reset to locked state
                prefs.edit()
                        .putBoolean("audio_feature_unlocked", false)
                        .putLong("audio_unlock_time", 0)
                        .apply();
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Stop floating dock service if running
     */
    private void stopFloatingDock() {
        Intent serviceIntent = new Intent(this, FloatingDockService.class);
        stopService(serviceIntent);
        LogUtils.d("MainActivity", "Floating dock service stopped");
    }

    /**
     * Check if floating dock is enabled in settings
     */
    public boolean isFloatingDockEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("key_floating_dock_enabled", true); // Default to true
    }

    /**
     * Public method to handle audio unlock from any fragment
     */
    public void onAudioUnlocked() {
        // Audio feature was unlocked via rewarded ad
        // Update advanced settings audio switch
        if (advancedAudioSwitch != null) {
            advancedAudioSwitch.setEnabled(true);
            advancedAudioSwitch.setAlpha(1.0f);
        }
        // Enable audio recording by default when unlocked
        isAudioEnabled = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hbRecorder.isAudioEnabled(true);
        }
        saveAudioPreference(true);
        saveSettings();
        
        // Refresh both fragments' audio controls
        if (quickSettingsFragment != null) {
            quickSettingsFragment.updateAudioFeatureState();
        }
        if (advancedSettingsFragment != null) {
            advancedSettingsFragment.refreshAudioControlsState();
        }
    }
}
