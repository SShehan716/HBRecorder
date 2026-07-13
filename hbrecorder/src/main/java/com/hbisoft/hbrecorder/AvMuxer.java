package com.hbisoft.hbrecorder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines a video-only recording with a separately captured audio file
 * (see {@link InternalAudioCapture}) into a single MP4. Stream copy only —
 * no re-encoding, so this takes seconds even for long recordings.
 */
final class AvMuxer {
    private static final String TAG = "AvMuxer";

    private AvMuxer() {
    }

    @androidx.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.O)
    static void mux(String videoPath, String audioPath, FileDescriptor outFd) throws IOException {
        MediaMuxer muxer = new MediaMuxer(outFd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxAll(videoPath, audioPath, muxer);
    }

    static void mux(String videoPath, String audioPath, String outPath) throws IOException {
        MediaMuxer muxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxAll(videoPath, audioPath, muxer);
    }

    private static void muxAll(String videoPath, String audioPath, MediaMuxer muxer) throws IOException {
        List<MediaExtractor> extractors = new ArrayList<>();
        List<Integer> srcTracks = new ArrayList<>();
        List<Integer> dstTracks = new ArrayList<>();
        int maxInputSize = 1024 * 1024;

        try {
            maxInputSize = Math.max(maxInputSize,
                    addSource(videoPath, "video/", extractors, srcTracks, dstTracks, muxer));
            if (audioPath != null) {
                try {
                    maxInputSize = Math.max(maxInputSize,
                            addSource(audioPath, "audio/", extractors, srcTracks, dstTracks, muxer));
                } catch (Exception e) {
                    Log.e(TAG, "Skipping audio track (unreadable): " + e.getMessage());
                }
            }

            if (dstTracks.isEmpty()) {
                throw new IOException("No tracks found to mux");
            }

            muxer.start();
            ByteBuffer buffer = ByteBuffer.allocate(maxInputSize);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            for (int i = 0; i < extractors.size(); i++) {
                MediaExtractor extractor = extractors.get(i);
                int dstTrack = dstTracks.get(i);
                while (true) {
                    int size = extractor.readSampleData(buffer, 0);
                    if (size < 0) break;
                    info.offset = 0;
                    info.size = size;
                    info.presentationTimeUs = extractor.getSampleTime();
                    info.flags = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                            ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                    muxer.writeSampleData(dstTrack, buffer, info);
                    extractor.advance();
                }
            }

            muxer.stop();
        } finally {
            for (MediaExtractor extractor : extractors) {
                try {
                    extractor.release();
                } catch (Exception ignored) {
                }
            }
            try {
                muxer.release();
            } catch (Exception ignored) {
            }
        }
    }

    /** Selects the first track whose mime starts with mimePrefix; returns its max input size. */
    private static int addSource(String path, String mimePrefix, List<MediaExtractor> extractors,
                                 List<Integer> srcTracks, List<Integer> dstTracks, MediaMuxer muxer) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(path);
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(mimePrefix)) {
                extractor.selectTrack(i);
                extractors.add(extractor);
                srcTracks.add(i);
                dstTracks.add(muxer.addTrack(format));
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    return format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                }
                return 0;
            }
        }
        extractor.release();
        throw new IOException("No " + mimePrefix + " track in " + path);
    }
}
