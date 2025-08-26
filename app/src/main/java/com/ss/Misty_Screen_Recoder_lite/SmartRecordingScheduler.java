package com.ss.Misty_Screen_Recoder_lite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.core.content.ContextCompat;

import com.hbisoft.hbrecorder.HBRecorder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Smart Recording Scheduler with Voice Commands
 * 
 * Features:
 * - Voice command recognition for hands-free recording control
 * - Scheduled recording with custom time intervals
 * - Voice feedback for recording status
 * 
 * This is a unique feature not available in other free screen recorder apps.
 */
public class SmartRecordingScheduler {
    private static final String TAG = "SmartRecordingScheduler";
    
    // Voice command keywords
    private static final String[] START_COMMANDS = {
        "start recording", "begin recording", "record screen", "start screen record",
        "start capture", "begin capture", "record now", "start now"
    };
    
    private static final String[] STOP_COMMANDS = {
        "stop recording", "end recording", "stop screen record", "end capture",
        "stop now", "end now", "pause recording", "pause capture"
    };
    
    private static final String[] SCHEDULE_COMMANDS = {
        "schedule recording", "record in", "start in", "begin in", "schedule for"
    };
    
    private static final String[] STATUS_COMMANDS = {
        "recording status", "is recording", "recording info", "status", "info"
    };
    
    // Context and dependencies
    private final Context context;
    private final RecordingCallback recordingCallback;
    private final HBRecorder hbRecorder;
    
    // Voice recognition components
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean isVoiceEnabled = false;
    
    // Scheduling components
    private AlarmManager alarmManager;
    private ScheduledExecutorService scheduler;
    private List<ScheduledRecording> scheduledRecordings;
    
    // Voice feedback
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    
    // Recording state
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    
    /**
     * Represents a scheduled recording task
     */
    public static class ScheduledRecording {
        public final long id;
        public final long scheduledTime;
        public final int durationMinutes;
        public final String description;
        
        public ScheduledRecording(long id, long scheduledTime, int durationMinutes, String description) {
            this.id = id;
            this.scheduledTime = scheduledTime;
            this.durationMinutes = durationMinutes;
            this.description = description;
        }
    }
    
    public SmartRecordingScheduler(Context context, RecordingCallback recordingCallback, HBRecorder hbRecorder) {
        this.context = context;
        this.recordingCallback = recordingCallback;
        this.hbRecorder = hbRecorder;
        
        initializeComponents();
    }
    
    /**
     * Initialize all components for voice recognition and scheduling
     */
    private void initializeComponents() {
        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            setupSpeechRecognizer();
        }
        
        // Initialize text-to-speech
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA &&
                             result != TextToSpeech.LANG_NOT_SUPPORTED);
            }
        });
        
        // Initialize alarm manager for scheduled recordings
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Initialize scheduler for background tasks
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize scheduled recordings list
        scheduledRecordings = new ArrayList<>();
        
        // Register broadcast receiver for scheduled recordings
        IntentFilter filter = new IntentFilter("SMART_RECORDING_ACTION");
        ContextCompat.registerReceiver(context, recordingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        
        LogUtils.d(TAG, "SmartRecordingScheduler initialized successfully");
    }
    
    /**
     * Setup speech recognizer with custom recognition listener
     */
    private void setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                LogUtils.d(TAG, "Ready for speech input");
                speakFeedback("Listening for commands");
            }
            
            @Override
            public void onBeginningOfSpeech() {
                LogUtils.d(TAG, "Speech started");
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {
                // Handle volume changes if needed
            }
            
            @Override
            public void onBufferReceived(byte[] buffer) {
                // Handle audio buffer
            }
            
            @Override
            public void onEndOfSpeech() {
                LogUtils.d(TAG, "Speech ended");
            }
            
            @Override
            public void onError(int error) {
                LogUtils.e(TAG, "Speech recognition error: " + error);
                isListening = false;
                
                // Restart listening after error
                if (isVoiceEnabled) {
                    scheduler.schedule(() -> startVoiceRecognition(), 2, TimeUnit.SECONDS);
                }
            }
            
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    LogUtils.d(TAG, "Voice command received: " + command);
                    processVoiceCommand(command);
                }
                
                // Continue listening if voice is enabled
                if (isVoiceEnabled) {
                    startVoiceRecognition();
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {
                // Handle partial results if needed
            }
            
            @Override
            public void onEvent(int eventType, Bundle params) {
                // Handle events
            }
        });
    }
    
    /**
     * Process voice commands and execute appropriate actions
     */
    private void processVoiceCommand(String command) {
        // Check for start recording commands
        for (String startCmd : START_COMMANDS) {
            if (command.contains(startCmd)) {
                if (!isRecording) {
                    speakFeedback("Starting screen recording");
                    startRecording();
                } else {
                    speakFeedback("Recording is already in progress");
                }
                return;
            }
        }
        
        // Check for stop recording commands
        for (String stopCmd : STOP_COMMANDS) {
            if (command.contains(stopCmd)) {
                if (isRecording) {
                    speakFeedback("Stopping screen recording");
                    stopRecording();
                } else {
                    speakFeedback("No recording is currently active");
                }
                return;
            }
        }
        
        // Check for status commands
        for (String statusCmd : STATUS_COMMANDS) {
            if (command.contains(statusCmd)) {
                provideRecordingStatus();
                return;
            }
        }
        
        // Check for schedule commands
        for (String scheduleCmd : SCHEDULE_COMMANDS) {
            if (command.contains(scheduleCmd)) {
                processScheduleCommand(command);
                return;
            }
        }
        
        // Unknown command
        speakFeedback("Command not recognized. Try saying start recording or stop recording");
    }
    
    /**
     * Process scheduling commands like "record in 5 minutes"
     */
    private void processScheduleCommand(String command) {
        try {
            // Extract time information from command
            String[] words = command.split("\\s+");
            int timeValue = 0;
            String timeUnit = "minutes";
            
            for (int i = 0; i < words.length; i++) {
                if (words[i].matches("\\d+")) {
                    timeValue = Integer.parseInt(words[i]);
                    if (i + 1 < words.length) {
                        String nextWord = words[i + 1].toLowerCase();
                        if (nextWord.contains("minute") || nextWord.contains("min")) {
                            timeUnit = "minutes";
                        } else if (nextWord.contains("hour") || nextWord.contains("hr")) {
                            timeUnit = "hours";
                            timeValue *= 60; // Convert to minutes
                        } else if (nextWord.contains("second") || nextWord.contains("sec")) {
                            timeUnit = "seconds";
                            timeValue = Math.max(1, timeValue / 60); // Convert to minutes (minimum 1)
                        }
                    }
                    break;
                }
            }
            
            if (timeValue > 0) {
                scheduleRecording(timeValue, "Voice scheduled recording");
                speakFeedback("Recording scheduled for " + timeValue + " " + timeUnit + " from now");
            } else {
                speakFeedback("Please specify a time, like record in 5 minutes");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Error processing schedule command: " + e.getMessage());
            speakFeedback("Sorry, I couldn't understand the schedule time");
        }
    }
    
    /**
     * Provide current recording status via voice
     */
    private void provideRecordingStatus() {
        if (isRecording) {
            long duration = System.currentTimeMillis() - recordingStartTime;
            long minutes = duration / (1000 * 60);
            long seconds = (duration / 1000) % 60;
            speakFeedback("Recording is active. Duration: " + minutes + " minutes " + seconds + " seconds");
        } else {
            speakFeedback("No recording is currently active");
        }
    }
    
    /**
     * Start voice recognition
     */
    public void startVoiceRecognition() {
        if (speechRecognizer == null || isListening) {
            return;
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        
        try {
            speechRecognizer.startListening(intent);
            isListening = true;
            LogUtils.d(TAG, "Voice recognition started");
        } catch (Exception e) {
            LogUtils.e(TAG, "Error starting voice recognition: " + e.getMessage());
        }
    }
    
    /**
     * Enable or disable voice commands
     */
    public void setVoiceEnabled(boolean enabled) {
        isVoiceEnabled = enabled;
        if (enabled) {
            startVoiceRecognition();
            speakFeedback("Voice commands enabled");
        } else {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
            isListening = false;
            speakFeedback("Voice commands disabled");
        }
    }
    
    /**
     * Schedule a recording for future execution
     */
    public void scheduleRecording(int delayMinutes, String description) {
        long scheduledTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L);
        long recordingId = System.currentTimeMillis();
        
        ScheduledRecording recording = new ScheduledRecording(recordingId, scheduledTime, 0, description);
        scheduledRecordings.add(recording);
        
        // Create alarm intent
        Intent intent = new Intent("SMART_RECORDING_ACTION");
        intent.putExtra("recording_id", recordingId);
        intent.putExtra("description", description);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            (int) recordingId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule the alarm
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            scheduledTime,
            pendingIntent
        );
        
        LogUtils.d(TAG, "Recording scheduled for " + new Date(scheduledTime) + ": " + description);
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
            speakFeedback("Starting scheduled recording");
            
            // Start recording on main thread
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (!isRecording) {
                        startRecording();
                    }
                });
            }
        }
    };
    
    /**
     * Start screen recording
     */
    private void startRecording() {
        if (isRecording) {
            return;
        }
        
        try {
            // Use the callback interface
            recordingCallback.startScreenRecording();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            LogUtils.d(TAG, "Screen recording started");
        } catch (Exception e) {
            LogUtils.e(TAG, "Error starting recording: " + e.getMessage());
            speakFeedback("Failed to start recording");
        }
    }
    
    /**
     * Stop screen recording
     */
    private void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        try {
            // Use the callback interface
            recordingCallback.stopScreenRecording();
            isRecording = false;
            long duration = System.currentTimeMillis() - recordingStartTime;
            LogUtils.d(TAG, "Screen recording stopped. Duration: " + duration + "ms");
            speakFeedback("Recording stopped");
        } catch (Exception e) {
            LogUtils.e(TAG, "Error stopping recording: " + e.getMessage());
            speakFeedback("Failed to stop recording");
        }
    }
    
    /**
     * Provide voice feedback using text-to-speech
     */
    private void speakFeedback(String message) {
        if (isTtsReady && textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "feedback");
        }
        LogUtils.d(TAG, "Voice feedback: " + message);
    }
    
    /**
     * Get current recording status
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Get recording duration in milliseconds
     */
    public long getRecordingDuration() {
        if (isRecording) {
            return System.currentTimeMillis() - recordingStartTime;
        }
        return 0;
    }
    
    /**
     * Get list of scheduled recordings
     */
    public List<ScheduledRecording> getScheduledRecordings() {
        return new ArrayList<>(scheduledRecordings);
    }
    
    /**
     * Cancel a scheduled recording
     */
    public void cancelScheduledRecording(long recordingId) {
        // Remove from list
        scheduledRecordings.removeIf(recording -> recording.id == recordingId);
        
        // Cancel alarm
        Intent intent = new Intent("SMART_RECORDING_ACTION");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            (int) recordingId, 
            intent, 
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        
        LogUtils.d(TAG, "Scheduled recording cancelled: " + recordingId);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        // Stop voice recognition
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        
        // Shutdown text-to-speech
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        
        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        
        // Cancel all scheduled recordings
        for (ScheduledRecording recording : scheduledRecordings) {
            cancelScheduledRecording(recording.id);
        }
        scheduledRecordings.clear();
        
        // Unregister receiver
        try {
            context.unregisterReceiver(recordingReceiver);
        } catch (Exception e) {
            LogUtils.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }
        
        LogUtils.d(TAG, "SmartRecordingScheduler cleaned up");
    }
}
