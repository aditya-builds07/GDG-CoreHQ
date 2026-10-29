package com.gdgcorehq.app;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivitySplashBinding;
import com.gdgcorehq.app.model.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight Video Splash Screen Activity per Rule R08 & R09.
 * Acts as the primary launcher entry point for GDG CoreHQ.
 * Plays full-screen muted branded splash animation and transitions
 * seamlessly based on active session state upon completion.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SAFETY_TIMEOUT_MS = 6000;

    private ActivitySplashBinding binding;
    private final AtomicBoolean isNavigating = new AtomicBoolean(false);
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = this::transitionToNextScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide system bars for an immersive, edge-to-edge video presentation
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Pre-warm user session in background while splash animation plays (Rule R09)
        SessionManager.getInstance().restoreSession(null);

        // Arm safety timeout to ensure splash never freezes indefinitely
        timeoutHandler.postDelayed(timeoutRunnable, SAFETY_TIMEOUT_MS);

        setupVideoPlayback();
    }

    private void setupVideoPlayback() {
        // Ensure VideoView is completely un-interactive
        binding.videoViewSplash.setClickable(false);
        binding.videoViewSplash.setFocusable(false);
        binding.videoViewSplash.setFocusableInTouchMode(false);
        binding.videoViewSplash.setOnTouchListener((v, event) -> true);

        // Zero media controls
        binding.videoViewSplash.setMediaController(null);

        // Exact millisecond completion listener transitions to next screen
        binding.videoViewSplash.setOnCompletionListener(mp -> transitionToNextScreen());

        // Any playback error triggers immediate graceful transition
        binding.videoViewSplash.setOnErrorListener((mp, what, extra) -> {
            transitionToNextScreen();
            return true;
        });

        binding.videoViewSplash.setOnPreparedListener(mp -> {
            // Muted audio per requirements
            mp.setVolume(0f, 0f);

            // Scale type set to fill/center-crop
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

            mp.setOnVideoSizeChangedListener((player, videoWidth, videoHeight) -> {
                if (videoWidth > 0 && videoHeight > 0) {
                    adjustVideoViewCenterCrop(videoWidth, videoHeight);
                }
            });

            binding.videoViewSplash.start();
        });

        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
            binding.videoViewSplash.setVideoURI(videoUri);
        } catch (Throwable t) {
            transitionToNextScreen();
        }
    }

    /**
     * Dynamically adjusts VideoView layout dimensions to fill and center-crop the screen.
     */
    private void adjustVideoViewCenterCrop(int videoWidth, int videoHeight) {
        if (binding == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        float videoAspect = (float) videoWidth / videoHeight;
        float screenAspect = (float) screenWidth / screenHeight;

        ViewGroup.LayoutParams params = binding.videoViewSplash.getLayoutParams();
        if (videoAspect > screenAspect) {
            // Video is wider than screen: match height, crop sides
            params.height = screenHeight;
            params.width = (int) (screenHeight * videoAspect);
        } else {
            // Video is taller than screen: match width, crop top/bottom
            params.width = screenWidth;
            params.height = (int) (screenWidth / videoAspect);
        }
        binding.videoViewSplash.setLayoutParams(params);
    }

    /**
     * Smart session-based routing per PRD §3.1 and requirements.
     * Guaranteed to execute at most once and finish SplashActivity immediately.
     */
    private void transitionToNextScreen() {
        if (!isNavigating.compareAndSet(false, true) || isFinishing()) {
            return;
        }

        timeoutHandler.removeCallbacks(timeoutRunnable);

        try {
            if (binding != null && binding.videoViewSplash != null) {
                binding.videoViewSplash.stopPlayback();
            }
        } catch (Throwable ignored) {}

        // Check active session via FirebaseAuth and SessionManager
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean hasLiveFirebaseUser = false;
        try {
            if (FirebaseApp.getInstance() != null) {
                FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                if (fbUser != null) {
                    hasLiveFirebaseUser = true;
                }
            }
        } catch (Throwable ignored) {}

        Intent intent;
        if (currentUser != null) {
            if (currentUser.isSuperAdmin()) {
                intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, MemberDashboardActivity.class);
            }
        } else if (hasLiveFirebaseUser && SessionManager.getInstance().isLoggedIn()) {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user != null && user.isSuperAdmin()) {
                intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, MemberDashboardActivity.class);
            }
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding != null && binding.videoViewSplash != null && binding.videoViewSplash.isPlaying()) {
            binding.videoViewSplash.pause();
        }
    }

    @Override
    protected void onDestroy() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (binding != null && binding.videoViewSplash != null) {
            try {
                binding.videoViewSplash.stopPlayback();
            } catch (Throwable ignored) {}
        }
        super.onDestroy();
        binding = null;
    }
}
