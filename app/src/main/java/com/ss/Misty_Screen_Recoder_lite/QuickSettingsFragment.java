package com.ss.Misty_Screen_Recoder_lite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class QuickSettingsFragment extends Fragment {
    private ChipGroup qualityChipGroup;
    private CheckBox audioCheckbox;
    private OnSettingsChangedListener listener;

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

        setupListeners();
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
        });
    }

    public void setQuality(boolean isHD) {
        Chip chip = getView().findViewById(isHD ? R.id.hd_chip : R.id.sd_chip);
        chip.setChecked(true);
    }

    public void setAudioEnabled(boolean enabled) {
        audioCheckbox.setChecked(enabled);
    }
} 