package com.ss.Misty_Screen_Recoder_lite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // load settings fragment
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener{
        ListPreference key_video_resolution, key_audio_source, key_video_encoder, key_video_fps, key_video_bitrate, key_output_format;
        SwitchPreference key_record_audio, key_dark_mode;
        Preference key_floating_dock_permission, key_privacy_policy;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            // Initialize new preferences
            key_dark_mode = findPreference("key_dark_mode");
            key_floating_dock_permission = findPreference("key_floating_dock_permission");

            // Set up dark mode preference
            if (key_dark_mode != null) {
                key_dark_mode.setOnPreferenceChangeListener(this);
            }

            // Set up floating dock permission preference
            if (key_floating_dock_permission != null) {
                key_floating_dock_permission.setOnPreferenceClickListener(this);
            }

            // Set up privacy policy preference
            key_privacy_policy = findPreference("key_privacy_policy");
            if (key_privacy_policy != null) {
                key_privacy_policy.setOnPreferenceClickListener(this);
            }

            key_record_audio = findPreference(getString(R.string.key_record_audio));

            key_audio_source = findPreference(getString(R.string.key_audio_source));
            if (key_audio_source != null) {
                key_audio_source.setOnPreferenceChangeListener(this);
            }

            key_video_encoder = findPreference(getString(R.string.key_video_encoder));
            if (key_video_encoder != null) {
                key_video_encoder.setOnPreferenceChangeListener(this);
            }

            key_video_resolution = findPreference(getString(R.string.key_video_resolution));
            if (key_video_resolution != null) {
                key_video_resolution.setOnPreferenceChangeListener(this);
            }

            key_video_fps = findPreference(getString(R.string.key_video_fps));
            if (key_video_fps != null) {
                key_video_fps.setOnPreferenceChangeListener(this);
            }

            key_video_bitrate = findPreference(getString(R.string.key_video_bitrate));
            if (key_video_bitrate != null) {
                key_video_bitrate.setOnPreferenceChangeListener(this);
            }

            key_output_format = findPreference(getString(R.string.key_output_format));
            if (key_output_format != null) {
                key_output_format.setOnPreferenceChangeListener(this);
            }

            setPreviousSelectedAsSummary();

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String preferenceKey = preference.getKey();
            
            if ("key_dark_mode".equals(preferenceKey)) {
                boolean isDarkMode = (Boolean) newValue;
                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                // Save the preference
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                editor.putBoolean("key_dark_mode", isDarkMode);
                editor.apply();
                // Recreate the activity to apply the theme
                requireActivity().recreate();
                return true;
            }
            
            ListPreference listPreference;
            switch (preferenceKey) {
                case "key_audio_source":
                    listPreference = findPreference(getString(R.string.key_audio_source));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                    }
                    break;
                case "key_video_encoder":
                    listPreference = findPreference(getString(R.string.key_video_encoder));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_video_resolution":
                    listPreference = findPreference(getString(R.string.key_video_resolution));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_video_fps":
                    listPreference = findPreference(getString(R.string.key_video_fps));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }

                    break;
                case "key_video_bitrate":
                    listPreference = findPreference(getString(R.string.key_video_bitrate));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_output_format":
                    listPreference = findPreference(getString(R.string.key_output_format));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if ("key_floating_dock_permission".equals(preference.getKey())) {
                // Open system overlay permission settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + requireActivity().getPackageName()));
                    startActivity(intent);
                    Toast.makeText(requireContext(), "Please grant overlay permission", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Overlay permission not needed on this Android version", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if ("key_privacy_policy".equals(preference.getKey())) {
                // Open privacy policy (you can replace this URL with your actual privacy policy URL)
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://github.com/yourusername/your-repo/blob/main/PRIVACY_POLICY.md"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open privacy policy", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        }

        private void setPreviousSelectedAsSummary() {
            if (getActivity() != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String video_resolution = prefs.getString("key_video_resolution", null);
                boolean audio_enabled = prefs.getBoolean("key_record_audio", true);
                String audio_source = prefs.getString("key_audio_source", null);
                String video_encoder = prefs.getString("key_video_encoder", null);
                String video_frame_rate = prefs.getString("key_video_fps", null);
                String video_bit_rate = prefs.getString("key_video_bitrate", null);
                String output_format = prefs.getString("key_output_format", null);
                boolean dark_mode = prefs.getBoolean("key_dark_mode", false);

                /*Dark Mode Prefs*/
                if (key_dark_mode != null) {
                    key_dark_mode.setChecked(dark_mode);
                }

                /*Record Audio Prefs*/
                if (key_record_audio != null) {
                    key_record_audio.setChecked(audio_enabled);
                }

                /*Audio Source Prefs*/
                if (audio_source != null && key_audio_source != null) {
                    int index = key_audio_source.findIndexOfValue(audio_source);
                    key_audio_source.setSummary(key_audio_source.getEntries()[index]);

                } else if (key_audio_source != null) {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_audio_source.getContext()).getString(key_audio_source.getKey(), "");
                    key_audio_source.setSummary(defaultSummary);
                }

                /*Video Encoder Prefs*/
                if (video_encoder != null && key_video_encoder != null) {
                    int index = key_video_encoder.findIndexOfValue(video_encoder);
                    key_video_encoder.setSummary(key_video_encoder.getEntries()[index]);

                } else if (key_video_encoder != null) {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_video_encoder.getContext()).getString(key_video_encoder.getKey(), "");
                    key_video_encoder.setSummary(defaultSummary);
                }

                /*Video Resolution Prefs*/
                if (video_resolution != null && key_video_resolution != null) {
                    int index = key_video_resolution.findIndexOfValue(video_resolution);
                    key_video_resolution.setSummary(key_video_resolution.getEntries()[index]);

                } else if (key_video_resolution != null) {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_video_resolution.getContext()).getString(key_video_resolution.getKey(), "");
                    key_video_resolution.setSummary(defaultSummary);
                }

                /*Video Frame Rate Prefs*/
                if (video_frame_rate != null && key_video_fps != null) {
                    int index = key_video_fps.findIndexOfValue(video_frame_rate);
                    key_video_fps.setSummary(key_video_fps.getEntries()[index]);

                } else if (key_video_fps != null) {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_video_fps.getContext()).getString(key_video_fps.getKey(), "");
                    key_video_fps.setSummary(defaultSummary);
                }

                /*Video Bit Rate Prefs*/
                if (video_bit_rate != null && key_video_bitrate != null) {
                    int index = key_video_bitrate.findIndexOfValue(video_bit_rate);
                    key_video_bitrate.setSummary(key_video_bitrate.getEntries()[index]);

                } else if (key_video_bitrate != null) {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_video_bitrate.getContext()).getString(key_video_bitrate.getKey(), "");
                    key_video_bitrate.setSummary(defaultSummary);
                }

                /*Output Format Prefs*/
                if (output_format != null && key_output_format != null) {
                    int index = key_output_format.findIndexOfValue(output_format);
                    key_output_format.setSummary(key_output_format.getEntries()[index]);

                } else if (key_output_format != null) {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_output_format.getContext()).getString(key_output_format.getKey(), "");
                    key_output_format.setSummary(defaultSummary);
                }

            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}
