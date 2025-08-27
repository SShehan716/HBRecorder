package com.ss.Misty_Screen_Recoder_lite;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class SmartRecordingFragment extends Fragment {
    
    // UI Components
    private SwitchMaterial switchVoiceCommands;
    private SwitchMaterial switchVoiceFeedback;
    private MaterialButton btnTestVoice;
    private MaterialButton btnStartRecording;
    private TextInputEditText etRecordingName;
    private TextInputEditText etDate;
    private TextInputEditText etTime;
    private TextInputEditText etDuration;
    private TextInputEditText etQuality;
    private MaterialButton btnSchedule;
    private RecyclerView rvScheduledRecordings;
    private View tvNoScheduledRecordings;
    private TextView tvScheduledCount;
    
    // Voice Recognition
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isVoiceCommandsEnabled = false;
    private boolean isVoiceFeedbackEnabled = true;
    
    // Scheduling
    private AlarmManager alarmManager;
    private List<ScheduledRecording> scheduledRecordings = new ArrayList<>();
    private ScheduledRecordingsAdapter adapter;
    private final AtomicInteger requestIdGenerator = new AtomicInteger(1000);
    
    // Constants
    private static final String PREFS_NAME = "SmartRecordingPrefs";
    private static final String KEY_SCHEDULED_RECORDINGS = "scheduled_recordings";
    private static final String KEY_VOICE_COMMANDS_ENABLED = "voice_commands_enabled";
    private static final String KEY_VOICE_FEEDBACK_ENABLED = "voice_feedback_enabled";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // Voice command patterns
    private static final String[] START_COMMANDS = {
        "start recording", "begin recording", "record screen", "start screen recording",
        "start video", "begin video", "record video", "start capture", "begin capture"
    };
    
    private static final String[] STOP_COMMANDS = {
        "stop recording", "end recording", "stop screen", "stop video", "end video",
        "stop capture", "end capture", "pause recording", "halt recording", "finish recording"
    };
    
    private BroadcastReceiver recordingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("SMART_RECORDING_ACTION".equals(intent.getAction())) {
                int requestId = intent.getIntExtra("request_id", -1);
                if (requestId != -1) {
                    startScheduledRecording(requestId);
                }
            }
        }
    };
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_smart_recording, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();
        loadPreferences();
        setupVoiceRecognition();
        setupDateTimePickers();
        setupClickListeners();
        updateUI();
    }
    
    private void initializeViews(View view) {
        switchVoiceCommands = view.findViewById(R.id.switchVoiceCommands);
        switchVoiceFeedback = view.findViewById(R.id.switchVoiceFeedback);
        btnTestVoice = view.findViewById(R.id.btnTestVoice);
        btnStartRecording = view.findViewById(R.id.btnStartRecording);
        etRecordingName = view.findViewById(R.id.etRecordingName);
        etDate = view.findViewById(R.id.etDate);
        etTime = view.findViewById(R.id.etTime);
        etDuration = view.findViewById(R.id.etDuration);
        etQuality = view.findViewById(R.id.etQuality);
        btnSchedule = view.findViewById(R.id.btnSchedule);
        rvScheduledRecordings = view.findViewById(R.id.rvScheduledRecordings);
        tvNoScheduledRecordings = view.findViewById(R.id.tvNoScheduledRecordings);
        tvScheduledCount = view.findViewById(R.id.tvScheduledCount);
        
        alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
    }
    
    private void setupRecyclerView() {
        adapter = new ScheduledRecordingsAdapter(scheduledRecordings, this::onEditRecording, this::onCancelRecording);
        rvScheduledRecordings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvScheduledRecordings.setAdapter(adapter);
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isVoiceCommandsEnabled = prefs.getBoolean(KEY_VOICE_COMMANDS_ENABLED, false);
        isVoiceFeedbackEnabled = prefs.getBoolean(KEY_VOICE_FEEDBACK_ENABLED, true);
        
        String recordingsJson = prefs.getString(KEY_SCHEDULED_RECORDINGS, "[]");
        Type type = new TypeToken<ArrayList<ScheduledRecording>>(){}.getType();
        scheduledRecordings.clear();
        scheduledRecordings.addAll(new Gson().fromJson(recordingsJson, type));
        
        switchVoiceCommands.setChecked(isVoiceCommandsEnabled);
        switchVoiceFeedback.setChecked(isVoiceFeedbackEnabled);
    }
    
    private void setupVoiceRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    speakFeedback("Listening for voice command");
                }
                
                @Override
                public void onBeginningOfSpeech() {}
                
                @Override
                public void onRmsChanged(float rmsdB) {}
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {}
                
                @Override
                public void onError(int error) {
                    String errorMessage = "Voice recognition error";
                    switch (error) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMessage = "No voice command recognized";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMessage = "Voice command timeout";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMessage = "Network error";
                            break;
                    }
                    speakFeedback(errorMessage);
                    if (isVoiceCommandsEnabled) {
                        startVoiceRecognition();
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String command = matches.get(0).toLowerCase();
                        handleVoiceCommand(command);
                    }
                    if (isVoiceCommandsEnabled) {
                        startVoiceRecognition();
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {}
                
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
        
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }
    
    private void setupDateTimePickers() {
        Calendar calendar = Calendar.getInstance();
        
        etDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                    etDate.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });
        
        etTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    etTime.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            );
            timePicker.show();
        });
        
        etQuality.setOnClickListener(v -> {
            String[] qualities = {"HD", "Full HD", "4K"};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Select Quality")
                   .setItems(qualities, (dialog, which) -> etQuality.setText(qualities[which]))
                   .show();
        });
    }
    
    private void setupClickListeners() {
        switchVoiceCommands.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isVoiceCommandsEnabled = isChecked;
            savePreferences();
            if (isChecked) {
                if (checkPermission()) {
                    startVoiceRecognition();
                    speakFeedback("Voice commands enabled");
                } else {
                    requestPermission();
                    buttonView.setChecked(false);
                }
            } else {
                stopVoiceRecognition();
                speakFeedback("Voice commands disabled");
            }
        });
        
        switchVoiceFeedback.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isVoiceFeedbackEnabled = isChecked;
            savePreferences();
        });
        
        btnTestVoice.setOnClickListener(v -> {
            if (checkPermission()) {
                startVoiceRecognition();
                speakFeedback("Testing voice recognition");
            } else {
                requestPermission();
            }
        });
        
        btnStartRecording.setOnClickListener(v -> {
            if (getActivity() instanceof RecordingCallback) {
                ((RecordingCallback) getActivity()).startScreenRecording();
                speakFeedback("Starting recording");
            }
        });
        
        btnSchedule.setOnClickListener(v -> scheduleRecording());
    }
    
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermission() {
        ActivityCompat.requestPermissions(requireActivity(), 
            new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }
    
    private void startVoiceRecognition() {
        if (speechRecognizer != null && isVoiceCommandsEnabled) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command");
            speechRecognizer.startListening(intent);
        }
    }
    
    private void stopVoiceRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }
    
    private void handleVoiceCommand(String command) {
        LogUtils.d("SmartRecording", "Voice command: " + command);
        
        // Check for start commands
        for (String startCmd : START_COMMANDS) {
            if (command.contains(startCmd)) {
                if (getActivity() instanceof RecordingCallback) {
                    ((RecordingCallback) getActivity()).startScreenRecording();
                    speakFeedback("Starting recording");
                }
                return;
            }
        }
        
        // Check for stop commands
        for (String stopCmd : STOP_COMMANDS) {
            if (command.contains(stopCmd)) {
                if (getActivity() instanceof RecordingCallback) {
                    ((RecordingCallback) getActivity()).stopScreenRecording();
                    speakFeedback("Stopping recording");
                }
                return;
            }
        }
        
        // Check for schedule commands
        if (command.contains("schedule") || command.contains("set up")) {
            speakFeedback("Opening schedule dialog");
            return;
        }
        
        speakFeedback("Command not recognized");
    }
    
    private void speakFeedback(String message) {
        if (isVoiceFeedbackEnabled && textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void scheduleRecording() {
        String name = etRecordingName.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String timeStr = etTime.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String quality = etQuality.getText().toString().trim();
        
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(dateStr) || 
            TextUtils.isEmpty(timeStr) || TextUtils.isEmpty(durationStr)) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int duration = Integer.parseInt(durationStr);
            if (duration <= 0 || duration > 1440) {
                Toast.makeText(requireContext(), "Duration must be between 1 and 1440 minutes", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Parse date and time
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
            
            Date date = dateFormat.parse(dateStr);
            Date time = timeFormat.parse(timeStr);
            
            if (date == null || time == null) {
                Toast.makeText(requireContext(), "Invalid date or time format", Toast.LENGTH_SHORT).show();
                return;
            }
            
            calendar.setTime(date);
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(time);
            calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), "Scheduled time must be in the future", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int requestId = requestIdGenerator.getAndIncrement();
            ScheduledRecording recording = new ScheduledRecording(
                requestId, name, calendar.getTimeInMillis(), duration, quality
            );
            
            scheduledRecordings.add(recording);
            scheduleAlarm(recording);
            savePreferences();
            updateUI();
            
            speakFeedback("Recording scheduled for " + name);
            
            // Clear form
            etRecordingName.setText("");
            etDate.setText("");
            etTime.setText("");
            etDuration.setText("5");
            etQuality.setText("HD");
            
        } catch (Exception e) {
            LogUtils.e("SmartRecording", "Error scheduling recording", e);
            Toast.makeText(requireContext(), "Error scheduling recording", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void scheduleAlarm(ScheduledRecording recording) {
        Intent intent = new Intent(requireContext(), SmartRecordingReceiver.class);
        intent.setAction("SMART_RECORDING_ACTION");
        intent.putExtra("request_id", recording.getId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 
            recording.getId(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            recording.getScheduledTime(),
            pendingIntent
        );
    }
    
    private void startScheduledRecording(int requestId) {
        ScheduledRecording recording = findRecordingById(requestId);
        if (recording != null) {
            speakFeedback("Starting scheduled recording: " + recording.getName());
            if (getActivity() instanceof RecordingCallback) {
                ((RecordingCallback) getActivity()).startScreenRecording();
            }
            
            // Schedule stop after duration
            new android.os.Handler().postDelayed(() -> {
                if (getActivity() instanceof RecordingCallback) {
                    ((RecordingCallback) getActivity()).stopScreenRecording();
                    speakFeedback("Scheduled recording completed");
                }
            }, recording.getDuration() * 60 * 1000L);
        }
    }
    
    private ScheduledRecording findRecordingById(int id) {
        for (ScheduledRecording recording : scheduledRecordings) {
            if (recording.getId() == id) {
                return recording;
            }
        }
        return null;
    }
    
    private void onEditRecording(ScheduledRecording recording) {
        Toast.makeText(requireContext(), "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void onCancelRecording(ScheduledRecording recording) {
        // Cancel alarm
        Intent intent = new Intent(requireContext(), SmartRecordingReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 
            recording.getId(), 
            intent, 
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        
        // Remove from list
        scheduledRecordings.remove(recording);
        savePreferences();
        updateUI();
        
        speakFeedback("Recording cancelled");
    }
    
    private void savePreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putBoolean(KEY_VOICE_COMMANDS_ENABLED, isVoiceCommandsEnabled);
        editor.putBoolean(KEY_VOICE_FEEDBACK_ENABLED, isVoiceFeedbackEnabled);
        
        String recordingsJson = new Gson().toJson(scheduledRecordings);
        editor.putString(KEY_SCHEDULED_RECORDINGS, recordingsJson);
        
        editor.apply();
    }
    
    private void updateUI() {
        adapter.notifyDataSetChanged();
        
        if (scheduledRecordings.isEmpty()) {
            rvScheduledRecordings.setVisibility(View.GONE);
            tvNoScheduledRecordings.setVisibility(View.VISIBLE);
        } else {
            rvScheduledRecordings.setVisibility(View.VISIBLE);
            tvNoScheduledRecordings.setVisibility(View.GONE);
        }
        
        tvScheduledCount.setText(scheduledRecordings.size() + " scheduled");
    }
    
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("SMART_RECORDING_ACTION");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(recordingReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(recordingReceiver, filter);
        }
        
        if (isVoiceCommandsEnabled && checkPermission()) {
            startVoiceRecognition();
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        try {
            requireContext().unregisterReceiver(recordingReceiver);
        } catch (Exception ignored) {}
        
        stopVoiceRecognition();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (switchVoiceCommands.isChecked()) {
                    startVoiceRecognition();
                }
            } else {
                switchVoiceCommands.setChecked(false);
                Toast.makeText(requireContext(), "Microphone permission required for voice commands", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // Data classes
    public static class ScheduledRecording {
        private int id;
        private String name;
        private long scheduledTime;
        private int duration; // in minutes
        private String quality;
        
        public ScheduledRecording(int id, String name, long scheduledTime, int duration, String quality) {
            this.id = id;
            this.name = name;
            this.scheduledTime = scheduledTime;
            this.duration = duration;
            this.quality = quality;
        }
        
        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public long getScheduledTime() { return scheduledTime; }
        public int getDuration() { return duration; }
        public String getQuality() { return quality; }
        
        // Setters
        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }
        public void setDuration(int duration) { this.duration = duration; }
        public void setQuality(String quality) { this.quality = quality; }
    }
}
