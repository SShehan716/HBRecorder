package com.ss.Misty_Screen_Recoder_lite;

import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/**
 * Full-screen video player backed by Media3 ExoPlayer.
 * Pass the video URI via {@link #EXTRA_URI} and an optional title via {@link #EXTRA_TITLE}.
 */
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_TITLE = "extra_title";

    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.player_view);

        String uriString = getIntent().getStringExtra(EXTRA_URI);
        if (uriString == null) {
            Toast.makeText(this, "No video to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) {
            setTitle(title);
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)));
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerErrorChanged(androidx.media3.common.PlaybackException error) {
                if (error != null) {
                    Toast.makeText(VideoPlayerActivity.this,
                            "Playback error: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
                }
            }
        });
        player.prepare();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
