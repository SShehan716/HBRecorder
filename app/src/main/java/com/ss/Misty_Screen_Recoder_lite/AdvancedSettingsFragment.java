package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.content.res.AppCompatResources;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Toast;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;

import java.util.ArrayList;

public class AdvancedSettingsFragment extends Fragment {
    private AutoCompleteTextView encoderDropdown;
    private AutoCompleteTextView resolutionDropdown;
    private AutoCompleteTextView framerateDropdown;
    private AutoCompleteTextView bitrateDropdown;
    private AutoCompleteTextView outputFormatDropdown;
    private AutoCompleteTextView audioSourceDropdown;
    private CheckBox audioCheckbox;
    private OnSettingsChangedListener listener;
    private HBRecorder hbRecorder;
    private AdMobHelper adMobHelper;
    
    // SharedPreferences keys
    private static final String PREF_AUDIO_ENABLED = "audio_enabled";
    private static final String PREF_HIGHEST_RESOLUTION_UNLOCKED = "highest_resolution_unlocked";
    private static final String PREF_HIGHEST_FRAMERATE_UNLOCKED = "highest_framerate_unlocked";
    private static final String PREF_HIGHEST_BITRATE_UNLOCKED = "highest_bitrate_unlocked";

    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        this.listener = listener;
    }

    public void setHBRecorder(HBRecorder hbRecorder) {
        this.hbRecorder = hbRecorder;
    }

    public void setAdMobHelper(AdMobHelper adMobHelper) {
        this.adMobHelper = adMobHelper;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_advanced_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        encoderDropdown = view.findViewById(R.id.encoder_dropdown);
        resolutionDropdown = view.findViewById(R.id.resolution_dropdown);
        framerateDropdown = view.findViewById(R.id.framerate_dropdown);
        bitrateDropdown = view.findViewById(R.id.bitrate_dropdown);
        outputFormatDropdown = view.findViewById(R.id.output_format_dropdown);
        audioSourceDropdown = view.findViewById(R.id.audio_source_dropdown);
        audioCheckbox = view.findViewById(R.id.advanced_audio_checkbox);

        setupDropdowns();
        setupListeners();
        loadAudioPreference();
    }

    private void setupDropdowns() {
        HBRecorderCodecInfo codecInfo = new HBRecorderCodecInfo();
        codecInfo.setContext(requireContext());

        ArrayList<String> supportedFormats = codecInfo.getSupportedVideoFormats();
        if (supportedFormats == null || supportedFormats.isEmpty()) {
            Toast.makeText(requireContext(), "No supported video codecs found. Screen recording is not supported on this device.", Toast.LENGTH_LONG).show();
            // Optionally, disable the record button or settings here
            return;
        }

        // Video Encoder options (by supported MIME types) - User-friendly names
        ArrayList<String> encoders = new ArrayList<>();
        for (String format : supportedFormats) {
            if (format.equals("MPEG_4")) encoders.add("H264");
            if (format.equals("WEBM")) encoders.add("VP8");
            if (format.equals("THREE_GPP")) encoders.add("H263");
        }
        if (encoders.isEmpty()) encoders.add("H264"); // fallback
        
        // Convert to user-friendly names
        ArrayList<String> userFriendlyEncoders = convertToUserFriendlyEncoders(encoders);
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, userFriendlyEncoders);
        encoderDropdown.setAdapter(encoderAdapter);
        encoderDropdown.setText(userFriendlyEncoders.get(0), false);

        // Output format options (supported video formats) - User-friendly names
        ArrayList<String> userFriendlyFormats = convertToUserFriendlyFormats(supportedFormats);
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, userFriendlyFormats);
        outputFormatDropdown.setAdapter(formatAdapter);
        outputFormatDropdown.setText(userFriendlyFormats.get(0), false);

        // Resolution options (filter by isSizeSupported) - test with multiple formats
        String bestFormat = getBestSupportedFormat(supportedFormats);
        String[] allResolutions = {"480p", "720p", "1080p", "1440p", "2160p"};
        ArrayList<String> supportedResolutions = new ArrayList<>();
        
        // Test with multiple formats to get better resolution support
        String[] testFormats = {bestFormat, "video/mp4", "video/avc", "video/hevc"};
        
        for (String res : allResolutions) {
            int w = 480, h = 854;  // Default for 480p
            if (res.equals("720p")) { w = 720; h = 1280; }
            if (res.equals("1080p")) { w = 1080; h = 1920; }
            if (res.equals("1440p")) { w = 1440; h = 2560; }
            if (res.equals("2160p")) { w = 2160; h = 3840; }
            
            boolean resolutionSupported = false;
            
            // Test with multiple formats
            for (String format : testFormats) {
                try {
                    if (codecInfo.isSizeSupported(w, h, format)) {
                        resolutionSupported = true;
                        if (BuildConfig.DEBUG) {
                            LogUtils.d("AdvancedSettingsFragment", "Resolution " + res + " (" + w + "x" + h + ") supported with format: " + format);
                        }
                        break;
                    }
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("AdvancedSettingsFragment", "Error testing resolution " + res + " with format " + format + ": " + e.getMessage());
                    }
                }
            }
            
            if (resolutionSupported) {
                supportedResolutions.add(res);
            } else {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Resolution " + res + " (" + w + "x" + h + ") not supported with any format");
                }
            }
        }
        
        if (supportedResolutions.isEmpty()) {
            supportedResolutions.add("720p");
            if (BuildConfig.DEBUG) {
                LogUtils.w("AdvancedSettingsFragment", "No resolutions detected, using 720p fallback");
            }
        }
        
        // Dynamic resolution locking - only lock highest if multiple options
        String highestResolution = getHighestResolution(supportedResolutions);
        boolean lockHighestResolution = supportedResolutions.size() > 1 && !isHighestResolutionUnlocked();
        ArrayAdapter<String> resolutionAdapter = createLockedAdapter(requireContext(), supportedResolutions, highestResolution, lockHighestResolution);
        resolutionDropdown.setAdapter(resolutionAdapter);
        resolutionDropdown.setText(supportedResolutions.get(0), false);

        // Frame rate options (filter by getMaxSupportedFrameRate) - improved detection
        String[] allFramerates = {"24", "30", "60"};
        ArrayList<String> supportedFramerates = new ArrayList<>();
        
        // Test each frame rate individually with different resolutions
        for (String fr : allFramerates) {
            double requestedFps = Double.parseDouble(fr);
            boolean frameRateSupported = false;
            
            // Test with multiple resolutions to see if this frame rate is supported
            int[] testResolutions = {
                720, 1280,   // 720p
                1080, 1920,  // 1080p
                1440, 2560   // 1440p
            };
            
            for (int i = 0; i < testResolutions.length; i += 2) {
                int testWidth = testResolutions[i];
                int testHeight = testResolutions[i + 1];
                
                try {
                    double maxFps = codecInfo.getMaxSupportedFrameRate(testWidth, testHeight, bestFormat);
                    
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("AdvancedSettingsFragment", "Frame rate " + fr + " - Test resolution: " + testWidth + "x" + testHeight + " - Max supported: " + maxFps + " - Supported: " + (requestedFps <= maxFps));
                    }
                    
                    if (requestedFps <= maxFps) {
                        frameRateSupported = true;
                        break; // If supported at any resolution, we can use this frame rate
                    }
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("AdvancedSettingsFragment", "Error testing frame rate " + fr + " at " + testWidth + "x" + testHeight + ": " + e.getMessage());
                    }
                }
            }
            
            if (frameRateSupported) {
                supportedFramerates.add(fr);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Frame rate " + fr + " is supported");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Frame rate " + fr + " is NOT supported");
                }
            }
        }
        
        if (supportedFramerates.isEmpty()) {
            supportedFramerates.add("30");
            LogUtils.w("AdvancedSettingsFragment", "No frame rates detected, using 30 FPS fallback");
        }
        
        // Dynamic framerate locking - only lock highest if multiple options
        String highestFramerate = getHighestFramerate(supportedFramerates);
        boolean lockHighestFramerate = supportedFramerates.size() > 1 && !isHighestFramerateUnlocked();
        ArrayAdapter<String> framerateAdapter = createLockedAdapter(requireContext(), supportedFramerates, highestFramerate, lockHighestFramerate);
        framerateDropdown.setAdapter(framerateAdapter);
        framerateDropdown.setText(supportedFramerates.get(0), false);

        // Bitrate options (filter by getMaxSupportedBitrate) - test with multiple formats
        String[] allBitrates = {"2 Mbps", "4 Mbps", "8 Mbps", "16 Mbps", "32 Mbps"};
        ArrayList<String> supportedBitrates = new ArrayList<>();
        
        // Try multiple formats to get the best bitrate support
        int maxBitrate = 0;
        String bestFormatForBitrate = bestFormat;
        
        // Test with different formats to find the highest supported bitrate
        String[] bitrateTestFormats = {"video/mp4", "video/avc", "video/hevc", "video/x-vnd.on2.vp8", "video/x-vnd.on2.vp9"};
        
        for (String format : bitrateTestFormats) {
            try {
                int bitrate = codecInfo.getMaxSupportedBitrate(format);
                if (bitrate > maxBitrate) {
                    maxBitrate = bitrate;
                    bestFormatForBitrate = format;
                }
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Format " + format + " - Max bitrate: " + bitrate + " bps (" + (bitrate/1000000) + " Mbps)");
                }
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Format " + format + " - Error getting bitrate: " + e.getMessage());
                }
            }
        }
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("AdvancedSettingsFragment", "Best format for bitrate: " + bestFormatForBitrate + " - Max bitrate: " + maxBitrate + " bps (" + (maxBitrate/1000000) + " Mbps)");
        }
        
        for (String br : allBitrates) {
            int val = Integer.parseInt(br.split(" ")[0]) * 1000000;
            
            if (BuildConfig.DEBUG) {
                LogUtils.d("AdvancedSettingsFragment", "Bitrate " + br + " (" + val + " bps) - Supported: " + (val <= maxBitrate));
            }
            
            if (val <= maxBitrate) {
                supportedBitrates.add(br);
            }
        }
        
        if (supportedBitrates.isEmpty()) {
            supportedBitrates.add("4 Mbps");
            LogUtils.w("AdvancedSettingsFragment", "No bitrates detected, using 4 Mbps fallback");
        }
        
        // Dynamic bitrate locking - only lock highest if multiple options
        String highestBitrate = getHighestBitrate(supportedBitrates);
        boolean lockHighestBitrate = supportedBitrates.size() > 1 && !isHighestBitrateUnlocked();
        ArrayAdapter<String> bitrateAdapter = createLockedAdapter(requireContext(), supportedBitrates, highestBitrate, lockHighestBitrate);
        bitrateDropdown.setAdapter(bitrateAdapter);
        bitrateDropdown.setText(supportedBitrates.get(0), false);

        // Audio source options - corrected for proper functionality
        String[] audioSources = {"Microphone", "Voice Call", "Default"};
        ArrayAdapter<String> audioSourceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, audioSources);
        audioSourceDropdown.setAdapter(audioSourceAdapter);
        audioSourceDropdown.setText(audioSources[0], false);
    }

    private ArrayAdapter<String> createLockedAdapter(@NonNull android.content.Context context, @NonNull java.util.List<String> items, @NonNull String highestItem, boolean lockHighest) {
        return new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(items)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                applyLockIconIfNeeded(position, view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                applyLockIconIfNeeded(position, view);
                return view;
            }

            private void applyLockIconIfNeeded(int position, View view) {
                if (!(view instanceof TextView)) return;
                TextView tv = (TextView) view;
                String value = getItem(position);
                boolean isLocked = lockHighest && highestItem.equals(value);
                if (isLocked) {
                    android.graphics.drawable.Drawable d = AppCompatResources.getDrawable(getContext(), com.ss.Misty_Screen_Recoder_lite.R.drawable.ic_lock_small);
                    if (d != null) {
                        int size = (int) (tv.getResources().getDisplayMetrics().density * 18); // make bigger
                        d.setBounds(0, 0, size, size);
                        tv.setCompoundDrawables(null, null, d, null);
                    }
                } else {
                    tv.setCompoundDrawables(null, null, null, null);
                }
                tv.setCompoundDrawablePadding((int) (tv.getResources().getDisplayMetrics().density * 8));
                tv.setTag(isLocked ? "locked" : null);
            }
        };
    }

    private boolean isSelectionLocked(@NonNull android.widget.AutoCompleteTextView dropdown, @NonNull String selectedText) {
        ArrayAdapter adapter = (ArrayAdapter) dropdown.getAdapter();
        if (adapter == null) return false;
        int position = adapter.getPosition(selectedText);
        if (position < 0) return false;
        View itemView = adapter.getView(position, null, null);
        if (itemView instanceof TextView) {
            Object tag = ((TextView) itemView).getTag();
            return tag != null && "locked".equals(tag);
        }
        return false;
    }

    private void setupListeners() {
        encoderDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String userFriendlyEncoder = encoderDropdown.getText().toString();
                String technicalEncoder = getUserFriendlyToTechnicalEncoder(userFriendlyEncoder);
                hbRecorder.setVideoEncoder(technicalEncoder);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Video encoder set to: " + userFriendlyEncoder + " (technical: " + technicalEncoder + ")");
                }
                saveSettings();
                notifySettingsChanged();
            }
        });

        resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String resolution = resolutionDropdown.getText().toString();
                
                // Check if selected resolution is locked (using adapter tag)
                if (isSelectionLocked(resolutionDropdown, resolution)) {
                    showUnlockResolutionDialog();
                    return;
                }
                
                int[] dimensions = getResolutionDimensions(resolution);
                if (dimensions != null) {
                    hbRecorder.setScreenDimensions(dimensions[0], dimensions[1]);
                    if (BuildConfig.DEBUG) {
                        LogUtils.d("AdvancedSettingsFragment", "Resolution set to: " + resolution + " (" + dimensions[0] + "x" + dimensions[1] + ")");
                    }
                } else {
                    LogUtils.e("AdvancedSettingsFragment", "Invalid resolution format: " + resolution);
                }
                notifySettingsChanged();
            }
        });

        framerateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String framerate = framerateDropdown.getText().toString();
                
                // Check if selected framerate is locked (using adapter tag)
                if (isSelectionLocked(framerateDropdown, framerate)) {
                    showUnlockFramerateDialog();
                    return;
                }
                
                hbRecorder.setVideoFrameRate(Integer.parseInt(framerate));
                notifySettingsChanged();
            }
        });

        bitrateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String bitrate = bitrateDropdown.getText().toString();
                
                // Check if selected bitrate is locked (using adapter tag)
                if (isSelectionLocked(bitrateDropdown, bitrate)) {
                    showUnlockBitrateDialog();
                    return;
                }
                
                int bitrateValue = Integer.parseInt(bitrate.split(" ")[0]) * 1000000;
                hbRecorder.setVideoBitrate(bitrateValue);
                notifySettingsChanged();
            }
        });

        outputFormatDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String userFriendlyFormat = outputFormatDropdown.getText().toString();
                String technicalFormat = getUserFriendlyToTechnicalFormat(userFriendlyFormat);
                hbRecorder.setOutputFormat(technicalFormat);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Output format set to: " + userFriendlyFormat + " (technical: " + technicalFormat + ")");
                }
                notifySettingsChanged();
            }
        });

        audioSourceDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String source = audioSourceDropdown.getText().toString();
                switch (source) {
                    case "Microphone":
                        hbRecorder.setAudioSource("MIC");
                        break;
                    case "Voice Call":
                        hbRecorder.setAudioSource("VOICE_CALL");
                        break;
                    case "Default":
                        hbRecorder.setAudioSource("DEFAULT");
                        break;
                    default:
                        hbRecorder.setAudioSource("MIC");
                        break;
                }
                notifySettingsChanged();
            }
        });

        audioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                hbRecorder.isAudioEnabled(isChecked);
                notifySettingsChanged();
                saveAudioPreference(isChecked);
                
                // Notify main activity to update its state (with null safety)
                if (getActivity() instanceof MainActivity && isAdded() && !isDetached()) {
                    try {
                        ((MainActivity) getActivity()).updateAudioState(isChecked);
                    } catch (Exception e) {
                        LogUtils.e("AdvancedSettingsFragment", "Error updating audio state: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void notifySettingsChanged() {
        if (listener != null) {
            listener.onSettingsChanged();
        }
    }

    public void loadSettings(String encoder, String resolution, String framerate, String bitrate,
                           String outputFormat, String audioSource, boolean audioEnabled) {
        // Load saved preferences if available, otherwise use provided defaults
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        
        // Load saved settings with fallbacks to provided defaults
        String savedEncoder = prefs.getString("saved_encoder", encoder);
        String savedResolution = prefs.getString("saved_resolution", resolution);
        String savedFramerate = prefs.getString("saved_framerate", framerate);
        String savedBitrate = prefs.getString("saved_bitrate", bitrate);
        String savedOutputFormat = prefs.getString("saved_output_format", outputFormat);
        String savedAudioSource = prefs.getString("saved_audio_source", audioSource);
        boolean savedAudioEnabled = prefs.getBoolean("saved_audio_enabled", audioEnabled);
        
        // Set the values to dropdowns
        encoderDropdown.setText(savedEncoder, false);
        resolutionDropdown.setText(savedResolution, false);
        framerateDropdown.setText(savedFramerate, false);
        bitrateDropdown.setText(savedBitrate, false);
        outputFormatDropdown.setText(savedOutputFormat, false);
        audioSourceDropdown.setText(savedAudioSource, false);
        audioCheckbox.setChecked(savedAudioEnabled);
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("AdvancedSettingsFragment", "Settings loaded - Encoder: " + savedEncoder + 
                ", Resolution: " + savedResolution + ", Framerate: " + savedFramerate + 
                ", Bitrate: " + savedBitrate + ", Format: " + savedOutputFormat + 
                ", AudioSource: " + savedAudioSource + ", AudioEnabled: " + savedAudioEnabled);
        }
    }

    private void loadAudioPreference() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        // Default to true (enabled) for first install, then use saved preference
        boolean audioEnabled = prefs.getBoolean(PREF_AUDIO_ENABLED, true);
        audioCheckbox.setChecked(audioEnabled);
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("AdvancedSettingsFragment", "Audio preference loaded: " + audioEnabled);
        }
    }
    
    private void saveAudioPreference(boolean enabled) {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(PREF_AUDIO_ENABLED, enabled).apply();
    }
    
    private void saveSettings() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        
        // Save all current settings
        editor.putString("saved_encoder", encoderDropdown.getText().toString());
        editor.putString("saved_resolution", resolutionDropdown.getText().toString().replace(" 🔒", ""));
        editor.putString("saved_framerate", framerateDropdown.getText().toString().replace(" 🔒", ""));
        editor.putString("saved_bitrate", bitrateDropdown.getText().toString().replace(" 🔒", ""));
        editor.putString("saved_output_format", outputFormatDropdown.getText().toString());
        editor.putString("saved_audio_source", audioSourceDropdown.getText().toString());
        editor.putBoolean("saved_audio_enabled", audioCheckbox.isChecked());
        
        editor.apply();
        
        if (BuildConfig.DEBUG) {
            LogUtils.d("AdvancedSettingsFragment", "Settings saved successfully");
        }
    }
    
    public void refreshAudioControlsState() {
        // No audio unlock logic, always enabled
    }

    private void updateAudioControlsState() {
        // No audio unlock logic, always enabled
    }
    
    // Helper methods for dynamic locking
    private String getHighestResolution(ArrayList<String> resolutions) {
        if (resolutions.contains("2160p")) return "2160p";
        if (resolutions.contains("1440p")) return "1440p";
        if (resolutions.contains("1080p")) return "1080p";
        if (resolutions.contains("720p")) return "720p";
        if (resolutions.contains("480p")) return "480p";
        return resolutions.get(resolutions.size() - 1); // Return last if none match
    }
    
    private String getHighestFramerate(ArrayList<String> framerates) {
        if (framerates.contains("60")) return "60";
        if (framerates.contains("30")) return "30";
        if (framerates.contains("24")) return "24";
        return framerates.get(framerates.size() - 1); // Return last if none match
    }
    
    private String getHighestBitrate(ArrayList<String> bitrates) {
        if (bitrates.contains("32 Mbps")) return "32 Mbps";
        if (bitrates.contains("16 Mbps")) return "16 Mbps";
        if (bitrates.contains("8 Mbps")) return "8 Mbps";
        if (bitrates.contains("4 Mbps")) return "4 Mbps";
        if (bitrates.contains("2 Mbps")) return "2 Mbps";
        return bitrates.get(bitrates.size() - 1); // Return last if none match
    }
    
    private boolean isHighestResolutionUnlocked() {
        if (!isAdded() || getContext() == null) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(PREF_HIGHEST_RESOLUTION_UNLOCKED, false);
    }
    
    private boolean isHighestFramerateUnlocked() {
        if (!isAdded() || getContext() == null) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(PREF_HIGHEST_FRAMERATE_UNLOCKED, false);
    }
    
    private boolean isHighestBitrateUnlocked() {
        if (!isAdded() || getContext() == null) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(PREF_HIGHEST_BITRATE_UNLOCKED, false);
    }
    
    // Unlock dialog methods
    private void showUnlockResolutionDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Unlock High Resolution")
                .setMessage("Watch a short video ad to unlock the highest resolution for your device. This will allow you to record in the best possible quality.")
                .setPositiveButton("Watch Video", (dialog, which) -> {
                    showRewardedAdToUnlockResolution();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showUnlockFramerateDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Unlock High Frame Rate")
                .setMessage("Watch a short video ad to unlock the highest frame rate for your device. This will allow you to record smooth, high-quality video.")
                .setPositiveButton("Watch Video", (dialog, which) -> {
                    showRewardedAdToUnlockFramerate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showUnlockBitrateDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Unlock High Bitrate")
                .setMessage("Watch a short video ad to unlock the highest bitrate for your device. This will allow you to record with maximum video quality.")
                .setPositiveButton("Watch Video", (dialog, which) -> {
                    showRewardedAdToUnlockBitrate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRewardedAdToUnlockResolution() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (adMobHelper != null) {
            adMobHelper.showRewardedAd((Activity) getContext(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    unlockHighestResolution();
                }
            }, () -> {
                // Ad failed to load - enable feature anyway
                Toast.makeText(getContext(), "Ad unavailable. High resolution enabled for free!", Toast.LENGTH_SHORT).show();
                unlockHighestResolution();
            });
        } else {
            Toast.makeText(getContext(), "Ad service not available. High resolution enabled for free!", Toast.LENGTH_SHORT).show();
            unlockHighestResolution();
        }
    }
    
    private void showRewardedAdToUnlockFramerate() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (adMobHelper != null) {
            adMobHelper.showRewardedAd((Activity) getContext(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    unlockHighestFramerate();
                }
            }, () -> {
                // Ad failed to load - enable feature anyway
                Toast.makeText(getContext(), "Ad unavailable. High frame rate enabled for free!", Toast.LENGTH_SHORT).show();
                unlockHighestFramerate();
            });
        } else {
            Toast.makeText(getContext(), "Ad service not available. High frame rate enabled for free!", Toast.LENGTH_SHORT).show();
            unlockHighestFramerate();
        }
    }
    
    private void showRewardedAdToUnlockBitrate() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (adMobHelper != null) {
            adMobHelper.showRewardedAd((Activity) getContext(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    unlockHighestBitrate();
                }
            }, () -> {
                // Ad failed to load - enable feature anyway
                Toast.makeText(getContext(), "Ad unavailable. High bitrate enabled for free!", Toast.LENGTH_SHORT).show();
                unlockHighestBitrate();
            });
        } else {
            Toast.makeText(getContext(), "Ad service not available. High bitrate enabled for free!", Toast.LENGTH_SHORT).show();
            unlockHighestBitrate();
        }
    }
    
    private void unlockHighestResolution() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(PREF_HIGHEST_RESOLUTION_UNLOCKED, true).apply();
        
        // Refresh the dropdown to remove lock icon and auto-select highest
        setupDropdowns();
        
        // Force refresh the adapter to remove lock icon
        resolutionDropdown.post(() -> {
            ArrayList<String> items = getAdapterItems(resolutionDropdown);
            if (!items.isEmpty()) {
                String highest = getHighestResolution(items);
                resolutionDropdown.setText(highest, false);
                int[] dims = getResolutionDimensions(highest);
                if (dims != null && hbRecorder != null) {
                    hbRecorder.setScreenDimensions(dims[0], dims[1]);
                }
                saveSettings();
                notifySettingsChanged();
            }
        });
        
        Toast.makeText(getContext(), "High resolution unlocked!", Toast.LENGTH_SHORT).show();
    }
    
    private void unlockHighestFramerate() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(PREF_HIGHEST_FRAMERATE_UNLOCKED, true).apply();
        
        // Refresh the dropdown to remove lock icon and auto-select highest
        setupDropdowns();
        
        // Force refresh the adapter to remove lock icon
        framerateDropdown.post(() -> {
            ArrayList<String> items = getAdapterItems(framerateDropdown);
            if (!items.isEmpty()) {
                String highest = getHighestFramerate(items);
                framerateDropdown.setText(highest, false);
                if (hbRecorder != null) {
                    hbRecorder.setVideoFrameRate(Integer.parseInt(highest));
                }
                saveSettings();
                notifySettingsChanged();
            }
        });
        
        Toast.makeText(getContext(), "High frame rate unlocked!", Toast.LENGTH_SHORT).show();
    }
    
    private void unlockHighestBitrate() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(PREF_HIGHEST_BITRATE_UNLOCKED, true).apply();
        
        // Refresh the dropdown to remove lock icon and auto-select highest
        setupDropdowns();
        
        // Force refresh the adapter to remove lock icon
        bitrateDropdown.post(() -> {
            ArrayList<String> items = getAdapterItems(bitrateDropdown);
            if (!items.isEmpty()) {
                String highest = getHighestBitrate(items);
                bitrateDropdown.setText(highest, false);
                if (hbRecorder != null) {
                    int bitrateValue = Integer.parseInt(highest.split(" ")[0]) * 1000000;
                    hbRecorder.setVideoBitrate(bitrateValue);
                }
                saveSettings();
                notifySettingsChanged();
            }
        });
        
        Toast.makeText(getContext(), "High bitrate unlocked!", Toast.LENGTH_SHORT).show();
    }

    private ArrayList<String> getAdapterItems(@NonNull AutoCompleteTextView dropdown) {
        ArrayList<String> list = new ArrayList<>();
        ArrayAdapter adapter = (ArrayAdapter) dropdown.getAdapter();
        if (adapter == null) return list;
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item != null) list.add(item.toString());
        }
        return list;
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
                LogUtils.e("AdvancedSettingsFragment", "Unknown resolution: " + resolution);
                return null;
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
     * Get the best supported format from the list of supported formats
     * Priority: MPEG_4 > WEBM > THREE_GPP
     */
    private String getBestSupportedFormat(ArrayList<String> supportedFormats) {
        if (supportedFormats.contains("MPEG_4")) {
            return "MPEG_4";
        } else if (supportedFormats.contains("WEBM")) {
            return "WEBM";
        } else if (supportedFormats.contains("THREE_GPP")) {
            return "THREE_GPP";
        } else if (!supportedFormats.isEmpty()) {
            return supportedFormats.get(0); // Return first available
        } else {
            return "MPEG_4"; // Fallback
        }
    }
} 