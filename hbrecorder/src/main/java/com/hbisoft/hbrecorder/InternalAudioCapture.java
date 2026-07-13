package com.hbisoft.hbrecorder;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Captures device/system audio (Android 10+ AudioPlaybackCapture), optionally
 * mixed with the microphone, encodes it to AAC and writes it to an .m4a file.
 * The result is muxed with the video track after recording stops.
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public class InternalAudioCapture {
    private static final String TAG = "InternalAudioCapture";
    private static final int CHANNELS = 2;
    private static final int FRAMES_PER_CHUNK = 1024; // stereo frames per encoder buffer

    private final MediaProjection mediaProjection;
    private final boolean includeMic;
    private final int sampleRate;
    private final int bitrate;
    private final File outputFile;

    private AudioRecord playbackRecord;
    private AudioRecord micRecord;
    private MediaCodec encoder;
    private MediaMuxer muxer;
    private Thread captureThread;

    private volatile boolean running = false;
    private volatile boolean paused = false;
    private boolean muxerStarted = false;
    private int muxerTrack = -1;
    private long totalFrames = 0;
    private boolean hasWrittenAudio = false;

    public InternalAudioCapture(MediaProjection mediaProjection, boolean includeMic,
                                int sampleRate, int bitrate, File outputFile) {
        this.mediaProjection = mediaProjection;
        this.includeMic = includeMic;
        this.sampleRate = sampleRate > 0 ? sampleRate : 44100;
        this.bitrate = bitrate > 0 ? bitrate : 128000;
        this.outputFile = outputFile;
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    public void start() throws Exception {
        AudioPlaybackCaptureConfiguration captureConfig =
                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build();

        AudioFormat playbackFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build();

        int playbackMinBuf = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        try {
            playbackRecord = new AudioRecord.Builder()
                    .setAudioFormat(playbackFormat)
                    .setBufferSizeInBytes(Math.max(playbackMinBuf * 2, 64 * 1024))
                    .setAudioPlaybackCaptureConfig(captureConfig)
                    .build();
        } catch (SecurityException e) {
            throw new IllegalStateException("RECORD_AUDIO permission is required for system audio capture", e);
        }

        if (includeMic) {
            int micMinBuf = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            try {
                micRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(micMinBuf * 2, 32 * 1024));
            } catch (SecurityException e) {
                Log.w(TAG, "Mic capture not permitted; recording system audio only");
                micRecord = null;
            }
            if (micRecord != null && micRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "Mic AudioRecord failed to initialize; recording system audio only");
                micRecord.release();
                micRecord = null;
            }
        }

        if (playbackRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("Playback-capture AudioRecord failed to initialize");
        }

        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, CHANNELS);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FRAMES_PER_CHUNK * CHANNELS * 2 * 2);
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        playbackRecord.startRecording();
        if (micRecord != null) {
            micRecord.startRecording();
        }

        running = true;
        captureThread = new Thread(this::captureLoop, "InternalAudioCapture");
        captureThread.start();
        Log.d(TAG, "Internal audio capture started (mic mixed: " + (micRecord != null) + ")");
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    /** Stops capture and finalizes the audio file. Blocks until done. */
    public void stop() {
        running = false;
        if (captureThread != null) {
            try {
                captureThread.join(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }
        release();
    }

    /** True when at least one audio sample made it into the file. */
    public boolean hasAudio() {
        return hasWrittenAudio && outputFile.exists() && outputFile.length() > 0;
    }

    private void captureLoop() {
        short[] playbackBuf = new short[FRAMES_PER_CHUNK * CHANNELS];
        short[] micBuf = new short[FRAMES_PER_CHUNK]; // mono
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        try {
            while (running) {
                int shortsRead = readFully(playbackRecord, playbackBuf);
                if (shortsRead <= 0) continue;

                if (micRecord != null) {
                    int micShorts = readFully(micRecord, micBuf, shortsRead / CHANNELS);
                    for (int frame = 0; frame < micShorts; frame++) {
                        int left = playbackBuf[frame * 2] + micBuf[frame];
                        int right = playbackBuf[frame * 2 + 1] + micBuf[frame];
                        playbackBuf[frame * 2] = clamp(left);
                        playbackBuf[frame * 2 + 1] = clamp(right);
                    }
                }

                if (paused) {
                    // Keep draining the hardware buffers but drop the data so
                    // audio stays aligned with the paused video timeline.
                    continue;
                }

                int inputIndex = encoder.dequeueInputBuffer(10_000);
                if (inputIndex >= 0) {
                    ByteBuffer input = encoder.getInputBuffer(inputIndex);
                    if (input != null) {
                        input.clear();
                        input.asShortBuffer().put(playbackBuf, 0, shortsRead);
                        long pts = totalFrames * 1_000_000L / sampleRate;
                        encoder.queueInputBuffer(inputIndex, 0, shortsRead * 2, pts, 0);
                        totalFrames += shortsRead / CHANNELS;
                    }
                }
                drainEncoder(info, false);
            }

            // Signal end of stream and drain what's left
            int inputIndex = encoder.dequeueInputBuffer(10_000);
            if (inputIndex >= 0) {
                long pts = totalFrames * 1_000_000L / sampleRate;
                encoder.queueInputBuffer(inputIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
            drainEncoder(info, true);
        } catch (Exception e) {
            Log.e(TAG, "Capture loop failed: " + e.getMessage());
        }
    }

    private void drainEncoder(MediaCodec.BufferInfo info, boolean untilEos) {
        while (true) {
            int outIndex = encoder.dequeueOutputBuffer(info, untilEos ? 10_000 : 0);
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                muxerTrack = muxer.addTrack(encoder.getOutputFormat());
                muxer.start();
                muxerStarted = true;
            } else if (outIndex >= 0) {
                ByteBuffer out = encoder.getOutputBuffer(outIndex);
                if (out != null && info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && muxerStarted) {
                    muxer.writeSampleData(muxerTrack, out, info);
                    hasWrittenAudio = true;
                }
                boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                encoder.releaseOutputBuffer(outIndex, false);
                if (eos) return;
            } else {
                if (!untilEos) return;
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // keep waiting for EOS a little while; bail if encoder is idle
                    return;
                }
            }
        }
    }

    private int readFully(AudioRecord record, short[] buffer) {
        return readFully(record, buffer, buffer.length);
    }

    private int readFully(AudioRecord record, short[] buffer, int count) {
        int total = 0;
        while (total < count && running) {
            int read = record.read(buffer, total, count - total);
            if (read <= 0) break;
            total += read;
        }
        return total;
    }

    private static short clamp(int value) {
        if (value > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE) return Short.MIN_VALUE;
        return (short) value;
    }

    private void release() {
        try {
            if (playbackRecord != null) {
                playbackRecord.stop();
                playbackRecord.release();
                playbackRecord = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing playback record: " + e.getMessage());
        }
        try {
            if (micRecord != null) {
                micRecord.stop();
                micRecord.release();
                micRecord = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing mic record: " + e.getMessage());
        }
        try {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
                encoder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing encoder: " + e.getMessage());
        }
        try {
            if (muxer != null) {
                if (muxerStarted) {
                    muxer.stop();
                }
                muxer.release();
                muxer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing muxer: " + e.getMessage());
            hasWrittenAudio = false;
        }
    }
}
