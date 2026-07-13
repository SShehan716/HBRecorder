package com.ss.Misty_Screen_Recoder_lite;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class QuickSettingsFragment extends Fragment {
    private ChipGroup qualityChipGroup;
    private CheckBox audioCheckbox;
    private Chip hdChip;
    private Chip sdChip;
    private OnSettingsChangedListener listener;

    // SharedPreferences keys
    private static final String PREF_AUDIO_ENABLED = "audio_enabled";

    public interface OnSettingsChangedListener {
        void onQualityChanged(boolean isHD);
        void onAudioChanged(boolean isEnabled);
    }

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        this.listener = listener;
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

        hdChip.setChipIconVisible(false);
        setupListeners();
        loadAudioPreference();
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
    }

    private void loadAudioPreference() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        // Default to true (enabled) for first install, then use saved preference
        boolean audioEnabled = prefs.getBoolean(PREF_AUDIO_ENABLED, true);
        audioCheckbox.setChecked(audioEnabled);

        if (BuildConfig.DEBUG) {
            LogUtils.d("QuickSettingsFragment", "Audio preference loaded: " + audioEnabled);
        }
    }

    private void saveAudioPreference(boolean enabled) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(PREF_AUDIO_ENABLED, enabled).apply();
    }

    public void setQuality(boolean isHD) {
        if (getView() != null && isAdded() && !isDetached()) {
            try {
                Chip chip = getView().findViewById(isHD ? R.id.hd_chip : R.id.sd_chip);
                if (chip != null) {
                    chip.setChecked(true);
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
        // HD is always available now; nothing to refresh.
    }
}
