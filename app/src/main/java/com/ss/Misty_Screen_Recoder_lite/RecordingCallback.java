package com.ss.Misty_Screen_Recoder_lite;

/**
 * Callback interface for recording actions
 * This allows fragments to communicate with MainActivity for recording operations
 */
public interface RecordingCallback {
    /**
     * Start screen recording
     */
    void startScreenRecording();
    
    /**
     * Stop screen recording
     */
    void stopScreenRecording();
    
    /**
     * Check if recording is currently active
     * @return true if recording is active, false otherwise
     */
    boolean isRecording();
}
