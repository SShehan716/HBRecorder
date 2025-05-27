package com.ss.Misty_Screen_Recoder_lite;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hbisoft.hbrecorder.HBRecorder;

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
    }

    private void setupDropdowns() {
        // Video Encoder options
        String[] encoders = {"H264", "H265"};
        ArrayAdapter<String> encoderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, encoders);
        encoderDropdown.setAdapter(encoderAdapter);
        encoderDropdown.setText(encoders[0], false);

        // Resolution options
        String[] resolutions = {"720p", "1080p", "1440p", "2160p"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, resolutions);
        resolutionDropdown.setAdapter(resolutionAdapter);
        resolutionDropdown.setText(resolutions[0], false);

        // Frame rate options
        String[] framerates = {"24", "30", "60"};
        ArrayAdapter<String> framerateAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, framerates);
        framerateDropdown.setAdapter(framerateAdapter);
        framerateDropdown.setText(framerates[1], false);

        // Bitrate options
        String[] bitrates = {"2 Mbps", "4 Mbps", "8 Mbps", "16 Mbps"};
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bitrates);
        bitrateDropdown.setAdapter(bitrateAdapter);
        bitrateDropdown.setText(bitrates[1], false);

        // Output format options
        String[] formats = {"MPEG_4", "WEBM", "3GP"};
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, formats);
        outputFormatDropdown.setAdapter(formatAdapter);
        outputFormatDropdown.setText(formats[0], false);

        // Audio source options
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
} 