package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Toast;

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
    
    // SharedPreferences keys
    private static final String PREF_AUDIO_ENABLED = "audio_enabled";

    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        this.listener = listener;
    }

    public void setHBRecorder(HBRecorder hbRecorder) {
        this.hbRecorder = hbRecorder;
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

        // Resolution options (filter by isSizeSupported) - using robust detection like MainActivity
        String bestFormat = getBestSupportedFormat(supportedFormats);
        String[] allResolutions = {"480p", "720p", "1080p", "1440p", "2160p"};
        ArrayList<String> supportedResolutions = new ArrayList<>();
        
        for (String res : allResolutions) {
            int w = 480, h = 854;  // Default for 480p
            if (res.equals("720p")) { w = 720; h = 1280; }
            if (res.equals("1080p")) { w = 1080; h = 1920; }
            if (res.equals("1440p")) { w = 1440; h = 2560; }
            if (res.equals("2160p")) { w = 2160; h = 3840; }
            
            if (codecInfo.isSizeSupported(w, h, bestFormat)) {
                supportedResolutions.add(res);
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Resolution " + res + " (" + w + "x" + h + ") supported");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    LogUtils.d("AdvancedSettingsFragment", "Resolution " + res + " not supported");
                }
            }
        }
        
        if (supportedResolutions.isEmpty()) {
            supportedResolutions.add("720p");
            if (BuildConfig.DEBUG) {
                LogUtils.w("AdvancedSettingsFragment", "No resolutions detected, using 720p fallback");
            }
        }
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, supportedResolutions);
        resolutionDropdown.setAdapter(resolutionAdapter);
        resolutionDropdown.setText(supportedResolutions.get(0), false);

        // Frame rate options (filter by getMaxSupportedFrameRate)
        String[] allFramerates = {"24", "30", "60"};
        ArrayList<String> supportedFramerates = new ArrayList<>();
        for (String fr : allFramerates) {
            double maxFps = codecInfo.getMaxSupportedFrameRate(720, 1280, "video/mp4");
            if (Double.parseDouble(fr) <= maxFps) supportedFramerates.add(fr);
        }
        if (supportedFramerates.isEmpty()) supportedFramerates.add("30");
        ArrayAdapter<String> framerateAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, supportedFramerates);
        framerateDropdown.setAdapter(framerateAdapter);
        framerateDropdown.setText(supportedFramerates.get(0), false);

        // Bitrate options (filter by getMaxSupportedBitrate)
        String[] allBitrates = {"2 Mbps", "4 Mbps", "8 Mbps", "16 Mbps"};
        ArrayList<String> supportedBitrates = new ArrayList<>();
        int maxBitrate = codecInfo.getMaxSupportedBitrate("video/mp4");
        for (String br : allBitrates) {
            int val = Integer.parseInt(br.split(" ")[0]) * 1000000;
            if (val <= maxBitrate) supportedBitrates.add(br);
        }
        if (supportedBitrates.isEmpty()) supportedBitrates.add("4 Mbps");
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, supportedBitrates);
        bitrateDropdown.setAdapter(bitrateAdapter);
        bitrateDropdown.setText(supportedBitrates.get(0), false);

        // Audio source options - corrected for proper functionality
        String[] audioSources = {"Microphone", "Internal Audio", "Voice Call", "Default"};
        ArrayAdapter<String> audioSourceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, audioSources);
        audioSourceDropdown.setAdapter(audioSourceAdapter);
        audioSourceDropdown.setText(audioSources[0], false);
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
                notifySettingsChanged();
            }
        });

        resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String resolution = resolutionDropdown.getText().toString();
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
                hbRecorder.setVideoFrameRate(Integer.parseInt(framerateDropdown.getText().toString()));
                notifySettingsChanged();
            }
        });

        bitrateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                String bitrate = bitrateDropdown.getText().toString();
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
                    case "Internal Audio":
                        // Use REMOTE_SUBMIX for internal audio capture (Android 10+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            hbRecorder.setAudioSource("REMOTE_SUBMIX");
                        } else {
                            hbRecorder.setAudioSource("DEFAULT");
                            Toast.makeText(requireContext(), "Internal audio recording requires Android 10+. Using default source.", Toast.LENGTH_LONG).show();
                        }
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
        encoderDropdown.setText(encoder, false);
        resolutionDropdown.setText(resolution, false);
        framerateDropdown.setText(framerate, false);
        bitrateDropdown.setText(bitrate, false);
        outputFormatDropdown.setText(outputFormat, false);
        audioSourceDropdown.setText(audioSource, false);
        audioCheckbox.setChecked(audioEnabled);
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
    
    public void refreshAudioControlsState() {
        // No audio unlock logic, always enabled
    }

    private void updateAudioControlsState() {
        // No audio unlock logic, always enabled
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