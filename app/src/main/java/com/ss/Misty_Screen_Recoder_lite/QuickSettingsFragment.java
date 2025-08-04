package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class QuickSettingsFragment extends Fragment {
    private ChipGroup qualityChipGroup;
    private CheckBox audioCheckbox;
    private MaterialButton unlockAudioButton;
    private ImageView audioLockIcon;
    private OnSettingsChangedListener listener;
    private AdMobHelper adMobHelper;
    
    // SharedPreferences keys
    private static final String PREF_AUDIO_UNLOCKED = "audio_feature_unlocked";
    private static final String PREF_AUDIO_ENABLED = "audio_enabled";

    public interface OnSettingsChangedListener {
        void onQualityChanged(boolean isHD);
        void onAudioChanged(boolean isEnabled);
        void onAudioUnlocked();
    }

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        this.listener = listener;
    }

    public void setAdMobHelper(AdMobHelper adMobHelper) {
        this.adMobHelper = adMobHelper;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quick_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        qualityChipGroup = view.findViewById(R.id.quality_chip_group);
        audioCheckbox = view.findViewById(R.id.audio_checkbox);
        unlockAudioButton = view.findViewById(R.id.unlock_audio_button);
        audioLockIcon = view.findViewById(R.id.audio_lock_icon);

        setupListeners();
        updateAudioFeatureState();
    }

    private void setupListeners() {
        qualityChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (listener != null) {
                listener.onQualityChanged(checkedId == R.id.hd_chip);
            }
        });

        audioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onAudioChanged(isChecked);
            }
            saveAudioPreference(isChecked);
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
        // Save unlocked state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putBoolean(PREF_AUDIO_UNLOCKED, true).apply();
        
        // Update UI
        updateAudioFeatureState();
        
        // Notify parent
        if (listener != null) {
            listener.onAudioUnlocked();
        }
        
        Toast.makeText(requireContext(), "Audio recording feature unlocked!", Toast.LENGTH_SHORT).show();
    }

    private void updateAudioFeatureState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isAudioUnlocked = prefs.getBoolean(PREF_AUDIO_UNLOCKED, false);
        
        if (isAudioUnlocked) {
            // Feature is unlocked
            unlockAudioButton.setVisibility(View.GONE);
            audioLockIcon.setVisibility(View.GONE);
            audioCheckbox.setEnabled(true);
            audioCheckbox.setAlpha(1.0f);
            
            // Restore saved audio preference
            boolean audioEnabled = prefs.getBoolean(PREF_AUDIO_ENABLED, false);
            audioCheckbox.setChecked(audioEnabled);
        } else {
            // Feature is locked
            unlockAudioButton.setVisibility(View.VISIBLE);
            audioLockIcon.setVisibility(View.VISIBLE);
            audioCheckbox.setEnabled(false);
            audioCheckbox.setChecked(false);
            audioCheckbox.setAlpha(0.5f);
            unlockAudioButton.setEnabled(true);
            unlockAudioButton.setText("Watch Video to Unlock");
        }
    }

    private void saveAudioPreference(boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putBoolean(PREF_AUDIO_ENABLED, enabled).apply();
    }

    public void setQuality(boolean isHD) {
        Chip chip = getView().findViewById(isHD ? R.id.hd_chip : R.id.sd_chip);
        chip.setChecked(true);
    }

    public void setAudioEnabled(boolean enabled) {
        if (audioCheckbox.isEnabled()) {
            audioCheckbox.setChecked(enabled);
        }
    }

    public boolean isAudioUnlocked() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        return prefs.getBoolean(PREF_AUDIO_UNLOCKED, false);
    }
} 