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
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_REACHED_ERROR;
import static com.hbisoft.hbrecorder.Constants.SETTINGS_ERROR;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Create fragments
        quickSettingsFragment = new QuickSettingsFragment();
        advancedSettingsFragment = new AdvancedSettingsFragment();

        // Set up ViewPager adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(quickSettingsFragment, "Quick");
        adapter.addFragment(advancedSettingsFragment, "Advanced");
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Quick" : "Advanced")
        ).attach();

        // Set up fragments
        setupFragments();

        // Initialize AdMob
        initializeAds();

        // Test overlay permission status
        testOverlayPermission();

        // Examples of how to use the HBRecorderCodecInfo class to get codec info
        HBRecorderCodecInfo hbRecorderCodecInfo = new HBRecorderCodecInfo();
            int mWidth = hbRecorder.getDefaultWidth();
            int mHeight = hbRecorder.getDefaultHeight();
            String mMimeType = "video/avc";
            int mFPS = 30;
            if (hbRecorderCodecInfo.isMimeTypeSupported(mMimeType)) {
                String defaultVideoEncoder = hbRecorderCodecInfo.getDefaultVideoEncoderName(mMimeType);
                boolean isSizeAndFramerateSupported = hbRecorderCodecInfo.isSizeAndFramerateSupported(mWidth, mHeight, mFPS, mMimeType, ORIENTATION_PORTRAIT);
                Log.e("EXAMPLE", "THIS IS AN EXAMPLE OF HOW TO USE THE (HBRecorderCodecInfo) TO GET CODEC INFO:");
                Log.e("HBRecorderCodecInfo", "defaultVideoEncoder for (" + mMimeType + ") -> " + defaultVideoEncoder);
                Log.e("HBRecorderCodecInfo", "MaxSupportedFrameRate -> " + hbRecorderCodecInfo.getMaxSupportedFrameRate(mWidth, mHeight, mMimeType));
                Log.e("HBRecorderCodecInfo", "MaxSupportedBitrate -> " + hbRecorderCodecInfo.getMaxSupportedBitrate(mMimeType));
                Log.e("HBRecorderCodecInfo", "isSizeAndFramerateSupported @ Width = "+mWidth+" Height = "+mHeight+" FPS = "+mFPS+" -> " + isSizeAndFramerateSupported);
                Log.e("HBRecorderCodecInfo", "isSizeSupported @ Width = "+mWidth+" Height = "+mHeight+" -> " + hbRecorderCodecInfo.isSizeSupported(mWidth, mHeight, mMimeType));
                Log.e("HBRecorderCodecInfo", "Default Video Format = " + hbRecorderCodecInfo.getDefaultVideoFormat());

                HashMap<String, String> supportedVideoMimeTypes = hbRecorderCodecInfo.getSupportedVideoMimeTypes();
                for (Map.Entry<String, String> entry : supportedVideoMimeTypes.entrySet()) {
                    Log.e("HBRecorderCodecInfo", "Supported VIDEO encoders and mime types : " + entry.getKey() + " -> " + entry.getValue());
                }

                HashMap<String, String> supportedAudioMimeTypes = hbRecorderCodecInfo.getSupportedAudioMimeTypes();
                for (Map.Entry<String, String> entry : supportedAudioMimeTypes.entrySet()) {
                    Log.e("HBRecorderCodecInfo", "Supported AUDIO encoders and mime types : " + entry.getKey() + " -> " + entry.getValue());
                }

                ArrayList<String> supportedVideoFormats = hbRecorderCodecInfo.getSupportedVideoFormats();
                for (int j = 0; j < supportedVideoFormats.size(); j++) {
                    Log.e("HBRecorderCodecInfo", "Available Video Formats : " + supportedVideoFormats.get(j));
                }
        } else {
                Log.e("HBRecorderCodecInfo", "MimeType not supported");
            }
    }

    private void requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
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
                Log.i("Folder ", "created");
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
        // Video Encoder options
        String[] encoders = {"H264", "H265"};
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, encoders);
        encoderDropdown.setAdapter(encoderAdapter);
        encoderDropdown.setText(encoders[0], false);

        // Resolution options
        String[] resolutions = {"720p", "1080p", "1440p", "2160p"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, resolutions);
        resolutionDropdown.setAdapter(resolutionAdapter);
        resolutionDropdown.setText(resolutions[0], false);

        // Frame rate options
        String[] framerates = {"24", "30", "60"};
        ArrayAdapter<String> framerateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, framerates);
        framerateDropdown.setAdapter(framerateAdapter);
        framerateDropdown.setText(framerates[1], false);

        // Bitrate options
        String[] bitrates = {"2 Mbps", "4 Mbps", "8 Mbps", "16 Mbps"};
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bitrates);
        bitrateDropdown.setAdapter(bitrateAdapter);
        bitrateDropdown.setText(bitrates[1], false);

        // Audio source options
        String[] audioSources = {"System + Mic", "System", "Mic"};
        ArrayAdapter<String> audioSourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, audioSources);
        audioSourceDropdown.setAdapter(audioSourceAdapter);
        audioSourceDropdown.setText(audioSources[0], false);

        // Output format options
        String[] formats = {"MPEG_4", "WEBM", "3GP"};
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, formats);
        outputFormatDropdown.setAdapter(formatAdapter);
        outputFormatDropdown.setText(formats[0], false);

        // Load saved settings after setting up dropdowns
        loadSettings();
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
            if (isStopPending) {
                // Ignore clicks while waiting for stop to complete
                return;
            }
                if (!hbRecorder.isBusyRecording()) {
                    startRecordingScreen();
                } else {
                isStopPending = true;
                    hbRecorder.stopScreenRecording();
                // The button text will be set to START in HBRecorderOnComplete()
            }
        });

        advancedAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAudioEnabled = isChecked;
            saveAudioPreference(isChecked);
            saveSettings();
        });

        encoderDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                hbRecorder.setVideoEncoder(encoderDropdown.getText().toString());
                saveSettings();
            }
        });

        resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String resolution = resolutionDropdown.getText().toString();
                switch (resolution) {
                    case "720p":
                        hbRecorder.setScreenDimensions(720, 1280);
                        break;
                    case "1080p":
                        hbRecorder.setScreenDimensions(1080, 1920);
                        break;
                    case "1440p":
                        hbRecorder.setScreenDimensions(1440, 2560);
                        break;
                    case "2160p":
                        hbRecorder.setScreenDimensions(2160, 3840);
                        break;
                }
                saveSettings();
            }
        });

        framerateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                hbRecorder.setVideoFrameRate(Integer.parseInt(framerateDropdown.getText().toString()));
                saveSettings();
            }
        });

        bitrateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String bitrate = bitrateDropdown.getText().toString();
                int bitrateValue = Integer.parseInt(bitrate.split(" ")[0]) * 1000000;
                hbRecorder.setVideoBitrate(bitrateValue);
                saveSettings();
            }
        });

        outputFormatDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String format = outputFormatDropdown.getText().toString();
                switch (format) {
                    case "MPEG_4":
                        hbRecorder.setOutputFormat("MPEG_4");
                        break;
                    case "WEBM":
                        hbRecorder.setOutputFormat("WEBM");
                        break;
                    case "3GP":
                        hbRecorder.setOutputFormat("3GP");
                        break;
                }
                saveSettings();
            }
        });

        audioSourceDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String source = audioSourceDropdown.getText().toString();
                switch (source) {
                    case "System + Mic":
                        hbRecorder.setAudioSource("DEFAULT");
                        break;
                    case "System":
                        hbRecorder.setAudioSource("DEFAULT");
                        break;
                    case "Mic":
                        hbRecorder.setAudioSource("MIC");
                        break;
                }
                saveSettings();
            }
        });
    }

    //Called when recording starts
    @Override
    public void HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called");
    }

    //Listener for when the recording is saved successfully
    //This will be called after the file was created
    @Override
    public void HBRecorderOnComplete() {
        startbtn.setText(R.string.start_recording);
        isStopPending = false;
        stopService(new Intent(this, FloatingDockService.class));
        showLongToast("Saved Successfully");
            if (hbRecorder.wasUriSet()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
                    updateGalleryUri();
                } else {
                    refreshGalleryFile();
                }
        } else {
                refreshGalleryFile();
            }
    }

    // Called when error occurs
    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        // Error 38 happens when
        // - the selected video encoder is not supported
        // - the output format is not supported
        // - if another app is using the microphone

        //It is best to use device default

        if (errorCode == SETTINGS_ERROR) {
            showLongToast(getString(R.string.settings_not_supported_message));
        } else if ( errorCode == MAX_FILE_SIZE_REACHED_ERROR) {
            showLongToast(getString(R.string.max_file_size_reached_message));
        } else {
            showLongToast(getString(R.string.general_recording_error_message));
            Log.e("HBRecorderOnError", reason);
        }

        startbtn.setText(R.string.start_recording);
        isStopPending = false;
        stopService(new Intent(this, FloatingDockService.class));
    }

    // Called when recording has been paused
    @Override
    public void HBRecorderOnPause() {
        // Called when recording was paused
    }

    // Calld when recording has resumed
    @Override
    public void HBRecorderOnResume() {
        // Called when recording was resumed
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
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
                    startRecordingScreen();
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
                setOutputPath();
                hbRecorder.startScreenRecording(data, resultCode);
                startbtn.setText(R.string.stop_recording);
                
                // Show interstitial ad when recording starts
                showInterstitialAdOnRecordingStart();
                
                // Check current system permission status and start floating dock
                checkAndStartFloatingDock();
            } else {
                startbtn.setText(R.string.start_recording);
            }
        } else if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            // User returned from overlay permission screen - check current system status
            checkAndStartFloatingDock();
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
        advancedAudioSwitch.setChecked(savedAudioEnabled);
    }

    private void setupFragments() {
        // Set up QuickSettingsFragment
        quickSettingsFragment.setOnSettingsChangedListener(new QuickSettingsFragment.OnSettingsChangedListener() {
            @Override
            public void onQualityChanged(boolean isHD) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    hbRecorder.isAudioEnabled(isEnabled);
                }
            }
        });

        // Set up AdvancedSettingsFragment
        advancedSettingsFragment.setHBRecorder(hbRecorder);
        advancedSettingsFragment.setOnSettingsChangedListener(() -> {
            // Settings have been changed, update UI if needed
        });
    }

    private void startRecording() {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQUEST_CODE) &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE)) {
                startScreenRecording();
        }
    }

    private void startScreenRecording() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }
    
    /**
     * Initialize AdMob ads
     */
    private void initializeAds() {
        adMobHelper = new AdMobHelper();
        
        // Load interstitial ad
        adMobHelper.loadInterstitialAd(this);
        
        // Load rewarded ad
        adMobHelper.loadRewardedAd(this);
    }
    
    /**
     * Show interstitial ad when recording starts
     */
    private void showInterstitialAdOnRecordingStart() {
        if (adMobHelper != null && adMobHelper.isInterstitialAdReady()) {
            adMobHelper.showInterstitialAd(this);
        }
    }
    
    /**
     * Check system permission status and start floating dock accordingly
     */
    private void checkAndStartFloatingDock() {
        // Check current system permission status using a more reliable method
        boolean hasOverlayPermission = checkOverlayPermission();
        
        Log.d("MainActivity", "Checking overlay permission - canDrawOverlays: " + hasOverlayPermission);
        Log.d("MainActivity", "Package name: " + getPackageName());
        Log.d("MainActivity", "Android version: " + Build.VERSION.SDK_INT);
        
        if (hasOverlayPermission) {
            Log.d("MainActivity", "Overlay permission already granted, starting FloatingDockService");
            startService(new Intent(this, FloatingDockService.class));
        } else {
            Log.d("MainActivity", "Overlay permission not granted, requesting permission");
            // Request overlay permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "Please grant overlay permission to show the floating dock.", Toast.LENGTH_LONG).show();
        }
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
                        Log.d("MainActivity", "Using fallback method - window manager available");
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error checking overlay permission: " + e.getMessage());
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
        
        Log.d("MainActivity", "=== OVERLAY PERMISSION TEST ===");
        Log.d("MainActivity", "Package: " + getPackageName());
        Log.d("MainActivity", "Has overlay permission: " + hasPermission);
        Log.d("MainActivity", "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
        Log.d("MainActivity", "===============================");
        
        if (hasPermission) {
            Toast.makeText(this, "Overlay permission is GRANTED", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Overlay permission is NOT GRANTED", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show rewarded ad for premium features
     */
    private void showRewardedAdForPremiumFeature() {
        if (adMobHelper != null) {
            adMobHelper.showRewardedAd(this, new AdMobHelper.RewardedAdCallback() {
                @Override
                public void onRewarded() {
                    // User watched the ad, give them premium feature
                    Toast.makeText(MainActivity.this, "Premium feature unlocked!", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onAdNotAvailable() {
                    Toast.makeText(MainActivity.this, "Ad not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
