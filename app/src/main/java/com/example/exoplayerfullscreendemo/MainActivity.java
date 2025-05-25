package com.example.exoplayerfullscreendemo;


import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private ImageView fullscreenButton;
    private boolean isFullscreen = false;

    // Original dimensions and layout params
    private ViewGroup.LayoutParams originalPlayerViewLayoutParams;
    private ViewGroup.LayoutParams originalPlayerViewParentLayoutParams;
    private ViewGroup playerViewParent; // This will be the ConstraintLayout holding the player view
    private ConstraintLayout rootLayout; // The very root layout of the activity

    // Views to hide/show during fullscreen
    private AppBarLayout appbarLayout;
    private NestedScrollView nestedScrollView; // The NestedScrollView
    private Button enrollButton; // The enroll button

    // Player state variables for saving/restoring
    private long playbackPosition = 0;
    private int currentWindow = 0;
    private boolean playWhenReady = true;

    // --- IMPORTANT: Replace with your actual video URL ---
    private static final String VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    // Example HLS: private static final String VIDEO_URL = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8";
    // Example DASH: private static final String VIDEO_URL = "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Find all necessary views
        rootLayout = findViewById(R.id.root_layout); // Get the root layout
        appbarLayout = findViewById(R.id.app_bar_layout);
        playerView = findViewById(R.id.player_vieww);
        nestedScrollView = findViewById(R.id.nestedsvd);
        enrollButton = findViewById(R.id.enroll);



    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(VIDEO_URL);
            player.setMediaItem(mediaItem);
            player.prepare();

            // Set the restored state
            player.seekTo(currentWindow, playbackPosition);
            player.setPlayWhenReady(playWhenReady);

            // Add a listener for debugging playback state and errors
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_IDLE:
                            Log.d("ExoPlayer", "Playback State: IDLE");
                            // Toast.makeText(MainActivity.this, "Player State: IDLE", Toast.LENGTH_SHORT).show();
                            break;
                        case Player.STATE_BUFFERING:
                            Log.d("ExoPlayer", "Playback State: BUFFERING");
                            // Toast.makeText(MainActivity.this, "Player State: BUFFERING", Toast.LENGTH_SHORT).show();
                            break;
                        case Player.STATE_READY:
                            Log.d("ExoPlayer", "Playback State: READY");
                            // Toast.makeText(MainActivity.this, "Player State: READY (Playing: " + player.getPlayWhenReady() + ")", Toast.LENGTH_SHORT).show();
                            break;
                        case Player.STATE_ENDED:
                            Log.d("ExoPlayer", "Playback State: ENDED");
                            // Toast.makeText(MainActivity.this, "Player State: ENDED", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e("ExoPlayer", "Playback Error: " + error.getMessage(), error);
                    Toast.makeText(MainActivity.this, "Error playing video: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void releasePlayer() {
        if (player != null) {
            // Save state before releasing
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            playWhenReady = player.getPlayWhenReady();

            player.release();
            player = null;
        }
    }

    //region Activity Lifecycle Callbacks
    @Override
    protected void onStart() {
        super.onStart();
        // Initialize player when activity starts and is visible
        initializePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure playback continues if player was paused
        if (player != null) {
            player.setPlayWhenReady(true);
            // If player was null (e.g., first start or after release due to memory), re-initialize
            if (player.getPlaybackState() == Player.STATE_IDLE) {
                player.prepare();
            }
        } else {
            initializePlayer(); // Re-initialize if for some reason it's null here
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause playback when activity goes to background
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Release player resources when activity is no longer visible
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // releasePlayer(); // Redundant if called in onStop, but harmless.
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save player state only if player is not null
        if (player != null) {
            outState.putLong("playbackPosition", player.getCurrentPosition());
            outState.putBoolean("playWhenReady", player.getPlayWhenReady());
            outState.putInt("currentWindow", player.getCurrentMediaItemIndex());
        } else {
            // If player is null, save default or last known values if any
            outState.putLong("playbackPosition", playbackPosition);
            outState.putBoolean("playWhenReady", playWhenReady);
            outState.putInt("currentWindow", currentWindow);
        }
        outState.putBoolean("isFullscreen", isFullscreen);
    }

}