package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class QuickSettingsFragment extends Fragment {
    private ChipGroup qualityChipGroup;
    private CheckBox audioCheckbox;
    private Chip hdChip;
    private Chip sdChip;
    private OnSettingsChangedListener listener;
    private AdMobHelper adMobHelper;
    
    // SharedPreferences keys
    private static final String PREF_AUDIO_ENABLED = "audio_enabled";
    private static final String PREF_HD_UNLOCKED = "hd_feature_unlocked";
    private static final String PREF_HD_UNLOCK_TIME = "hd_unlock_time";

    public interface OnSettingsChangedListener {
        void onQualityChanged(boolean isHD);
        void onAudioChanged(boolean isEnabled);
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
        hdChip = view.findViewById(R.id.hd_chip);
        sdChip = view.findViewById(R.id.sd_chip);

        setupListeners();
        updateHDChipState();
    }

    private void setupListeners() {
        qualityChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.hd_chip) {
                // Check if HD is unlocked
                if (!isHDUnlocked()) {
                    // HD is locked, show unlock dialog
                    showUnlockHDDialog();
                    // Revert to SD selection
                    sdChip.setChecked(true);
                    return;
                }
            }
            
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
    }

    private void showUnlockHDDialog() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Unlock HD Recording")
                .setMessage("Watch a short video ad to unlock HD recording quality. This will allow you to record in 1080p resolution with higher bitrate for better video quality.")
                .setPositiveButton("Watch Video", (dialog, which) -> {
                    showRewardedAdToUnlockHD();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRewardedAdToUnlockHD() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (adMobHelper != null) {
            // Show loading state
            hdChip.setEnabled(false);
            hdChip.setText("Loading...");
            hdChip.setChipIconVisible(false);
            
            adMobHelper.showRewardedAd((Activity) getContext(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // User successfully watched the ad and earned reward
                    unlockHDFeature();
                }
            }, () -> {
                // Ad failed to load - show error message and restore locked state
                Toast.makeText(getContext(), "Ad unavailable. Please try again later.", Toast.LENGTH_SHORT).show();
                hdChip.setEnabled(true);
                updateHDChipState(); // This will show "HD 🔒" since feature is still locked
                
                // Ensure SD is selected since HD unlock failed
                if (sdChip != null) {
                    sdChip.setChecked(true);
                    if (listener != null) {
                        listener.onQualityChanged(false);
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Ad service not available", Toast.LENGTH_SHORT).show();
            hdChip.setEnabled(true);
            updateHDChipState(); // This will show "HD 🔒" since feature is still locked
            
            // Ensure SD is selected since HD unlock failed
            if (sdChip != null) {
                sdChip.setChecked(true);
                if (listener != null) {
                    listener.onQualityChanged(false);
                }
            }
        }
    }

    private void unlockHDFeature() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        // Save unlocked state with timestamp
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        long currentTime = System.currentTimeMillis();
        prefs.edit()
                .putBoolean(PREF_HD_UNLOCKED, true)
                .putLong(PREF_HD_UNLOCK_TIME, currentTime)
                .apply();
        
        // Update UI
        updateHDChipState();
        
        // Enable HD selection
        hdChip.setChecked(true);
        if (listener != null) {
            listener.onQualityChanged(true);
        }
        
        Toast.makeText(getContext(), "HD recording quality unlocked! It will expire in 24 hours.", Toast.LENGTH_LONG).show();
    }

    private boolean isHDUnlocked() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null) {
            return false; // Default to locked if not attached
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean isUnlocked = prefs.getBoolean(PREF_HD_UNLOCKED, false);
        
        if (isUnlocked) {
            // Check if 24 hours have passed since unlock
            long unlockTime = prefs.getLong(PREF_HD_UNLOCK_TIME, 0);
            long currentTime = System.currentTimeMillis();
            long oneDayInMillis = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
            
            if (currentTime - unlockTime > oneDayInMillis) {
                // Expired, reset to locked state
                prefs.edit()
                        .putBoolean(PREF_HD_UNLOCKED, false)
                        .putLong(PREF_HD_UNLOCK_TIME, 0)
                        .apply();
                return false;
            }
            return true;
        }
        return false;
    }

    private void updateHDChipState() {
        boolean isUnlocked = isHDUnlocked();
        
        if (!isUnlocked) {
            // HD is locked - show lock state
            hdChip.setText("HD");
            hdChip.setChipIcon(getResources().getDrawable(R.drawable.ic_lock_small));
            hdChip.setChipIconVisible(true);
            hdChip.setEnabled(true);
        } else {
            // HD is unlocked - show normal state
            hdChip.setText("HD");
            hdChip.setChipIconVisible(false);
            hdChip.setEnabled(true);
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

    public void setQuality(boolean isHD) {
        if (getView() != null && isAdded() && !isDetached()) {
            try {
                if (isHD && !isHDUnlocked()) {
                    // HD is locked, revert to SD
                    sdChip.setChecked(true);
                    if (listener != null) {
                        listener.onQualityChanged(false);
                    }
                } else {
                    Chip chip = getView().findViewById(isHD ? R.id.hd_chip : R.id.sd_chip);
                    if (chip != null) {
                        chip.setChecked(true);
                    }
                }
            } catch (Exception e) {
                LogUtils.e("QuickSettingsFragment", "Error setting quality: " + e.getMessage());
            }
        }
    }

    public void setAudioEnabled(boolean enabled) {
        if (audioCheckbox != null && isAdded() && !isDetached()) {
            try {
                audioCheckbox.setChecked(enabled);
            } catch (Exception e) {
                LogUtils.e("QuickSettingsFragment", "Error setting audio enabled: " + e.getMessage());
            }
        }
    }

    public void refreshHDChipState() {
        // Check if fragment is attached and has context
        if (!isAdded() || getContext() == null || hdChip == null) {
            return;
        }
        updateHDChipState();
    }
} 