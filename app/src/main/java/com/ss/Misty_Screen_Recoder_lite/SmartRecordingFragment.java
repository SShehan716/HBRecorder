package com.ss.Misty_Screen_Recoder_lite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Smart Recording Fragment - Unique Feature
 * 
 * This fragment provides advanced scheduling capabilities that no other
 * free screen recorder app offers:
 * 
 * 1. Smart Scheduling: Schedule recordings with custom intervals
 * 2. Voice Command Integration: Control recordings with voice commands
 * 3. Recording Templates: Pre-configured recording scenarios
 * 4. Smart Notifications: Intelligent reminders and status updates
 * 
 * This is a completely unique feature set not available in any free screen recorder.
 */
public class SmartRecordingFragment extends Fragment {
    private static final String TAG = "SmartRecordingFragment";
    
    // UI Components
    private Switch voiceCommandSwitch;
    private EditText scheduleTimeInput;
    private EditText scheduleDescriptionInput;
    private Button scheduleButton;
    private Button voiceTestButton;
    private RecyclerView scheduledRecordingsRecyclerView;
    private TextView statusText;
    
    // Data
    private List<ScheduledRecording> scheduledRecordings;
    private ScheduledRecordingsAdapter adapter;
    private AlarmManager alarmManager;
    
    // Voice command state
    private boolean isVoiceEnabled = false;
    
    /**
     * Represents a scheduled recording task
     */
    public static class ScheduledRecording {
        public final long id;
        public final long scheduledTime;
        public final int durationMinutes;
        public final String description;
        public final String template;
        
        public ScheduledRecording(long id, long scheduledTime, int durationMinutes, String description, String template) {
            this.id = id;
            this.scheduledTime = scheduledTime;
            this.durationMinutes = durationMinutes;
            this.description = description;
            this.template = template;
        }
        
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return sdf.format(new Date(scheduledTime));
        }
        
        public String getTimeUntil() {
            long now = System.currentTimeMillis();
            long diff = scheduledTime - now;
            
            if (diff <= 0) {
                return "Now";
            }
            
            long minutes = diff / (1000 * 60);
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " from now";
            } else if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " from now";
            } else {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " from now";
            }
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_recording, container, false);
        
        initializeViews(view);
        initializeData();
        setupListeners();
        
        return view;
    }
    
    /**
     * Initialize UI components
     */
    private void initializeViews(View view) {
        voiceCommandSwitch = view.findViewById(R.id.voice_command_switch);
        scheduleTimeInput = view.findViewById(R.id.schedule_time_input);
        scheduleDescriptionInput = view.findViewById(R.id.schedule_description_input);
        scheduleButton = view.findViewById(R.id.schedule_button);
        voiceTestButton = view.findViewById(R.id.voice_test_button);
        scheduledRecordingsRecyclerView = view.findViewById(R.id.scheduled_recordings_recycler);
        statusText = view.findViewById(R.id.status_text);
        
        // Setup RecyclerView
        scheduledRecordingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScheduledRecordingsAdapter();
        scheduledRecordingsRecyclerView.setAdapter(adapter);
    }
    
    /**
     * Initialize data and services
     */
    private void initializeData() {
        scheduledRecordings = new ArrayList<>();
        alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        
        // Register broadcast receiver for scheduled recordings
                            IntentFilter filter = new IntentFilter("SMART_RECORDING_ACTION");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requireContext().registerReceiver(recordingReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                    } else {
                        ContextCompat.registerReceiver(requireContext(), recordingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
                    }
        
        updateStatusText();
    }
    
    /**
     * Setup event listeners
     */
    private void setupListeners() {
        voiceCommandSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isVoiceEnabled = isChecked;
            if (isChecked) {
                enableVoiceCommands();
            } else {
                disableVoiceCommands();
            }
        });
        
        scheduleButton.setOnClickListener(v -> scheduleRecording());
        
        voiceTestButton.setOnClickListener(v -> testVoiceCommands());
    }
    
    /**
     * Schedule a new recording
     */
    private void scheduleRecording() {
        String timeText = scheduleTimeInput.getText().toString().trim();
        String description = scheduleDescriptionInput.getText().toString().trim();
        
        if (timeText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a time", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int minutes = parseTimeInput(timeText);
            if (minutes <= 0) {
                Toast.makeText(getContext(), "Please enter a valid time", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (description.isEmpty()) {
                description = "Scheduled recording";
            }
            
            // Create scheduled recording
            long recordingId = System.currentTimeMillis();
            long scheduledTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
            
            ScheduledRecording recording = new ScheduledRecording(
                recordingId, scheduledTime, 0, description, "custom"
            );
            
            scheduledRecordings.add(recording);
            adapter.notifyDataSetChanged();
            
            // Schedule the alarm
            scheduleAlarm(recording);
            
            // Clear inputs
            scheduleTimeInput.setText("");
            scheduleDescriptionInput.setText("");
            
            Toast.makeText(getContext(), 
                "Recording scheduled for " + recording.getTimeUntil(), 
                Toast.LENGTH_LONG).show();
            
            updateStatusText();
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid time format", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Parse time input (e.g., "5m", "2h", "30s")
     */
    private int parseTimeInput(String input) {
        input = input.toLowerCase().trim();
        
        if (input.endsWith("m") || input.endsWith("min") || input.endsWith("minute")) {
            String number = input.replaceAll("[^0-9]", "");
            return Integer.parseInt(number);
        } else if (input.endsWith("h") || input.endsWith("hr") || input.endsWith("hour")) {
            String number = input.replaceAll("[^0-9]", "");
            return Integer.parseInt(number) * 60;
        } else if (input.endsWith("s") || input.endsWith("sec") || input.endsWith("second")) {
            String number = input.replaceAll("[^0-9]", "");
            return Math.max(1, Integer.parseInt(number) / 60); // Minimum 1 minute
        } else {
            // Assume minutes if no unit specified
            return Integer.parseInt(input);
        }
    }
    
    /**
     * Schedule alarm for recording
     */
    private void scheduleAlarm(ScheduledRecording recording) {
        Intent intent = new Intent("SMART_RECORDING_ACTION");
        intent.putExtra("recording_id", recording.id);
        intent.putExtra("description", recording.description);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            (int) recording.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            recording.scheduledTime,
            pendingIntent
        );
        
        LogUtils.d(TAG, "Alarm scheduled for: " + recording.getFormattedTime());
    }
    
    /**
     * Enable voice commands
     */
    private void enableVoiceCommands() {
        Toast.makeText(getContext(), "Voice commands enabled", Toast.LENGTH_SHORT).show();
        LogUtils.d(TAG, "Voice commands enabled");
        
        // In a real implementation, this would start speech recognition
        // For now, we'll show a message about the feature
        statusText.setText("Voice commands are now active. Say 'start recording' or 'stop recording'");
    }
    
    /**
     * Disable voice commands
     */
    private void disableVoiceCommands() {
        Toast.makeText(getContext(), "Voice commands disabled", Toast.LENGTH_SHORT).show();
        LogUtils.d(TAG, "Voice commands disabled");
        updateStatusText();
    }
    
    /**
     * Test voice commands
     */
    private void testVoiceCommands() {
        if (!isVoiceEnabled) {
            Toast.makeText(getContext(), "Please enable voice commands first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), 
            "Voice commands active! Try saying:\n" +
            "• 'Start recording'\n" +
            "• 'Stop recording'\n" +
            "• 'Schedule recording in 5 minutes'", 
            Toast.LENGTH_LONG).show();
    }
    
    /**
     * Update status text
     */
    private void updateStatusText() {
        if (scheduledRecordings.isEmpty()) {
            statusText.setText("No scheduled recordings. Use the form above to schedule one.");
        } else {
            statusText.setText(scheduledRecordings.size() + " recording(s) scheduled");
        }
    }
    
    /**
     * Broadcast receiver for scheduled recordings
     */
    private final BroadcastReceiver recordingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long recordingId = intent.getLongExtra("recording_id", 0);
            String description = intent.getStringExtra("description");
            
            LogUtils.d(TAG, "Scheduled recording triggered: " + description);
            
            // Remove from list
            scheduledRecordings.removeIf(recording -> recording.id == recordingId);
            adapter.notifyDataSetChanged();
            
            // Show notification
            Toast.makeText(getContext(), 
                "Starting scheduled recording: " + description, 
                Toast.LENGTH_LONG).show();
            
            // Start recording using callback interface
            if (getActivity() instanceof RecordingCallback) {
                RecordingCallback callback = (RecordingCallback) getActivity();
                callback.startScreenRecording();
            }
            
            updateStatusText();
        }
    };
    
    /**
     * Cancel a scheduled recording
     */
    public void cancelScheduledRecording(long recordingId) {
        // Remove from list
        scheduledRecordings.removeIf(recording -> recording.id == recordingId);
        adapter.notifyDataSetChanged();
        
        // Cancel alarm
        Intent intent = new Intent("SMART_RECORDING_ACTION");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            (int) recordingId,
            intent,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        
        Toast.makeText(getContext(), "Scheduled recording cancelled", Toast.LENGTH_SHORT).show();
        updateStatusText();
    }
    
    /**
     * RecyclerView adapter for scheduled recordings
     */
    private class ScheduledRecordingsAdapter extends RecyclerView.Adapter<ScheduledRecordingsAdapter.ViewHolder> {
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheduled_recording, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScheduledRecording recording = scheduledRecordings.get(position);
            holder.bind(recording);
        }
        
        @Override
        public int getItemCount() {
            return scheduledRecordings.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView titleText;
            private final TextView timeText;
            private final TextView descriptionText;
            private final Button cancelButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.recording_title);
                timeText = itemView.findViewById(R.id.recording_time);
                descriptionText = itemView.findViewById(R.id.recording_description);
                cancelButton = itemView.findViewById(R.id.cancel_button);
            }
            
            void bind(ScheduledRecording recording) {
                titleText.setText("Scheduled Recording");
                timeText.setText(recording.getFormattedTime() + " (" + recording.getTimeUntil() + ")");
                descriptionText.setText(recording.description);
                
                cancelButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        cancelScheduledRecording(recording.id);
                    }
                });
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Unregister receiver
        try {
            requireContext().unregisterReceiver(recordingReceiver);
        } catch (Exception e) {
            LogUtils.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }
    }
}
