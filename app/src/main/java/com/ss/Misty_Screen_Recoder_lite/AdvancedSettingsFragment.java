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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.material.button.MaterialButton;
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
    private ImageView advancedAudioLockIcon;
    private TextView audioLockedMessage;
    private com.google.android.material.textfield.TextInputLayout audioSourceContainer;
    private MaterialButton unlockAudioButton;
    private AdMobHelper adMobHelper;
    
    // SharedPreferences keys
    private static final String PREF_AUDIO_UNLOCKED = "audio_feature_unlocked";
    private static final String PREF_AUDIO_ENABLED = "audio_enabled";
    private static final String PREF_AUDIO_UNLOCK_TIME = "audio_unlock_time";

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
        advancedAudioLockIcon = view.findViewById(R.id.advanced_audio_lock_icon);
        audioLockedMessage = view.findViewById(R.id.audio_locked_message);
        audioSourceContainer = view.findViewById(R.id.audio_source_container);
        unlockAudioButton = view.findViewById(R.id.advanced_unlock_audio_button);

        setupDropdowns();
        setupListeners();
        updateAudioControlsState();
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

        // Video Encoder options (by supported MIME types)
        ArrayList<String> encoders = new ArrayList<>();
        for (String format : supportedFormats) {
            if (format.equals("MPEG_4")) encoders.add("H264");
            if (format.equals("WEBM")) encoders.add("VP8");
            if (format.equals("THREE_GPP")) encoders.add("H263");
        }
        if (encoders.isEmpty()) encoders.add("H264"); // fallback
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, encoders);
        encoderDropdown.setAdapter(encoderAdapter);
        encoderDropdown.setText(encoders.get(0), false);

        // Output format options (supported video formats)
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, supportedFormats);
        outputFormatDropdown.setAdapter(formatAdapter);
        outputFormatDropdown.setText(supportedFormats.get(0), false);

        // Resolution options (filter by isSizeSupported)
        String[] allResolutions = {"720p", "1080p", "1440p", "2160p"};
        ArrayList<String> supportedResolutions = new ArrayList<>();
        for (String res : allResolutions) {
            int w = 720, h = 1280;
            if (res.equals("1080p")) { w = 1080; h = 1920; }
            if (res.equals("1440p")) { w = 1440; h = 2560; }
            if (res.equals("2160p")) { w = 2160; h = 3840; }
            if (codecInfo.isSizeSupported(w, h, "video/mp4")) supportedResolutions.add(res);
        }
        if (supportedResolutions.isEmpty()) supportedResolutions.add("720p");
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

        // Audio source options (keep as is for now)
        String[] audioSources = {"System + Mic", "System", "Mic"};
        ArrayAdapter<String> audioSourceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, audioSources);
        audioSourceDropdown.setAdapter(audioSourceAdapter);
        audioSourceDropdown.setText(audioSources[0], false);
    }

    private void setupListeners() {
        encoderDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                hbRecorder.setVideoEncoder(encoderDropdown.getText().toString());
                notifySettingsChanged();
            }
        });

        resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
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
                notifySettingsChanged();
            }
        });

        audioSourceDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
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
                notifySettingsChanged();
            }
        });

        audioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hbRecorder != null) {
                hbRecorder.isAudioEnabled(isChecked);
                notifySettingsChanged();
                
                // Notify main activity to update its state
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateAudioState(isChecked);
                }
            }
        });

        unlockAudioButton.setOnClickListener(v -> {
            showUnlockAudioDialog();
        });
    }

    private void showUnlockAudioDialog() {
        // Show confirmation dialog to ensure user intent
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Unlock Audio Recording")
                .setMessage("Watch a short video ad to unlock the audio recording feature. This will allow you to record system audio and microphone during screen recording.")
                .setPositiveButton("Watch Video", (dialog, which) -> {
                    showRewardedAdToUnlock();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRewardedAdToUnlock() {
        if (adMobHelper != null) {
            // Show loading state
            unlockAudioButton.setEnabled(false);
            unlockAudioButton.setText("Loading...");
            
            adMobHelper.showRewardedAd((Activity) requireContext(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // User successfully watched the ad and earned reward
                    unlockAudioFeature();
                }
            });
        } else {
            Toast.makeText(requireContext(), "Ad service not available", Toast.LENGTH_SHORT).show();
            unlockAudioButton.setEnabled(true);
            unlockAudioButton.setText("Watch Video to Unlock");
        }
    }

    private void unlockAudioFeature() {
        // Save unlocked state with timestamp
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        long currentTime = System.currentTimeMillis();
        prefs.edit()
                .putBoolean(PREF_AUDIO_UNLOCKED, true)
                .putLong(PREF_AUDIO_UNLOCK_TIME, currentTime)
                .apply();
        
        // Update UI
        updateAudioControlsState();
        
        // Notify MainActivity to refresh other fragments
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onAudioUnlocked();
        }
        
        Toast.makeText(requireContext(), "Audio recording feature unlocked! It will expire in 24 hours.", Toast.LENGTH_LONG).show();
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

    public void refreshAudioControlsState() {
        updateAudioControlsState();
    }

    private void updateAudioControlsState() {
        // Use MainActivity's centralized isAudioUnlocked method
        boolean isAudioUnlocked = false;
        if (getActivity() instanceof MainActivity) {
            isAudioUnlocked = ((MainActivity) getActivity()).isAudioUnlocked();
        }

        if (!isAudioUnlocked) {
            // Audio is locked - show unlock UI
            advancedAudioLockIcon.setVisibility(View.VISIBLE);
            audioLockedMessage.setVisibility(View.VISIBLE);
            unlockAudioButton.setVisibility(View.VISIBLE);
            audioSourceContainer.setVisibility(View.GONE);
            audioCheckbox.setEnabled(false);
            audioCheckbox.setAlpha(0.5f);
        } else {
            // Audio is unlocked - show normal UI
            advancedAudioLockIcon.setVisibility(View.GONE);
            audioLockedMessage.setVisibility(View.GONE);
            unlockAudioButton.setVisibility(View.GONE);
            audioSourceContainer.setVisibility(View.VISIBLE);
            audioCheckbox.setEnabled(true);
            audioCheckbox.setAlpha(1.0f);
        }
    }
} 