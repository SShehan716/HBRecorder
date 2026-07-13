package com.ss.Misty_Screen_Recoder_lite;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple video editor: pick a start/end range and save a lossless
 * trimmed copy next to the original recording.
 */
public class TrimActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_NAME = "extra_name";

    private ExoPlayer player;
    private RangeSlider rangeSlider;
    private TextView rangeLabel;
    private ProgressBar progressBar;
    private MaterialButton saveButton;

    private Uri sourceUri;
    private String sourceName;
    private long durationMs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim);

        Toolbar toolbar = findViewById(R.id.trim_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.trim_video);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        String uriString = getIntent().getStringExtra(EXTRA_URI);
        sourceName = getIntent().getStringExtra(EXTRA_NAME);
        if (uriString == null) {
            finish();
            return;
        }
        sourceUri = Uri.parse(uriString);

        rangeSlider = findViewById(R.id.trim_range_slider);
        rangeLabel = findViewById(R.id.trim_range_label);
        progressBar = findViewById(R.id.trim_progress);
        saveButton = findViewById(R.id.trim_save_button);
        PlayerView playerView = findViewById(R.id.trim_player_view);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(sourceUri));
        player.prepare();

        durationMs = readDurationMs();
        if (durationMs <= 0) {
            Toast.makeText(this, R.string.trim_load_failed, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        float durationSec = Math.max(1f, durationMs / 1000f);
        rangeSlider.setValueFrom(0f);
        rangeSlider.setValueTo(durationSec);
        rangeSlider.setValues(0f, durationSec);
        rangeSlider.setLabelFormatter(value -> DateUtils.formatElapsedTime((long) value));
        rangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            updateRangeLabel();
            if (fromUser && player != null) {
                // Preview from the start handle position.
                player.seekTo((long) (slider.getValues().get(0) * 1000));
            }
        });
        updateRangeLabel();

        saveButton.setOnClickListener(v -> saveTrimmed());
    }

    private long readDurationMs() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, sourceUri);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return duration != null ? Long.parseLong(duration) : 0;
        } catch (Exception e) {
            LogUtils.e("TrimActivity", "Duration read failed: " + e.getMessage());
            return 0;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private void updateRangeLabel() {
        float start = rangeSlider.getValues().get(0);
        float end = rangeSlider.getValues().get(1);
        rangeLabel.setText(getString(R.string.trim_range_format,
                DateUtils.formatElapsedTime((long) start),
                DateUtils.formatElapsedTime((long) end),
                DateUtils.formatElapsedTime((long) (end - start))));
    }

    private void saveTrimmed() {
        float startSec = rangeSlider.getValues().get(0);
        float endSec = rangeSlider.getValues().get(1);
        if (endSec - startSec < 1f) {
            Toast.makeText(this, R.string.trim_too_short, Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        if (player != null) player.pause();

        long startUs = (long) (startSec * 1_000_000L);
        long endUs = (long) (endSec * 1_000_000L);

        executor.execute(() -> {
            try {
                String savedName = TrimUtils.trim(this, sourceUri, startUs, endUs, sourceName);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.trim_saved, savedName), Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                LogUtils.e("TrimActivity", "Trim failed: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.trim_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        executor.shutdown();
    }
}
