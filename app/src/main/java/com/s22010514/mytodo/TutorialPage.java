package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TutorialPage extends AppCompatActivity {

    private MaterialButton getStartedBtn;
    private MaterialButton skipBtn;
    private MaterialButton watchVideoBtn;
    private MaterialButton playPauseBtn;
    private ImageView tutorialImage;
    private VideoView tutorialVideoView;
    private SeekBar videoSeekBar;
    private TextView videoTimeText;
    private ProgressBar videoLoadingProgress;
    private FirebaseAuth mAuth;

    private boolean isPlaying = false;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("TutorialPage", "TutorialPage onCreate() called");

        try {
            setContentView(R.layout.activity_tutorial_page);
            android.util.Log.d("TutorialPage", "Layout set successfully");

            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();


            android.util.Log.d("TutorialPage", "About to initialize views");

            // Initialize views
            initializeViews();

            // Set up click listeners
            setupClickListeners();

            // Set up modern back button handling
            setupBackPressedCallback();

            // Welcome the user
            welcomeUser();

            android.util.Log.d("TutorialPage", "TutorialPage setup complete");

        } catch (Exception e) {
            android.util.Log.e("TutorialPage", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error loading tutorial: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupBackPressedCallback() {
        // Modern way to handle back button press
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new androidx.appcompat.app.AlertDialog.Builder(TutorialPage.this)
                        .setTitle("🔙 Exit Tutorial?")
                        .setMessage("Are you sure you want to skip the tutorial and go to the app?")
                        .setPositiveButton("Yes, Skip", (dialog, which) -> navigateToHome())
                        .setNegativeButton("Continue Tutorial", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeViews() {
        // Use safe findViewById calls with null checks
        getStartedBtn = findViewById(R.id.getStartedBtn);
        skipBtn = findViewById(R.id.skipBtn);
        watchVideoBtn = findViewById(R.id.watchVideoBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        tutorialImage = findViewById(R.id.tutorialImage);
        tutorialVideoView = findViewById(R.id.tutorialVideoView);
        videoSeekBar = findViewById(R.id.videoSeekBar);
        videoTimeText = findViewById(R.id.videoTimeText);
        videoLoadingProgress = findViewById(R.id.videoLoadingProgress);

        // Log which views were found/not found for debugging
        android.util.Log.d("TutorialPage", "getStartedBtn found: " + (getStartedBtn != null));
        android.util.Log.d("TutorialPage", "skipBtn found: " + (skipBtn != null));
        android.util.Log.d("TutorialPage", "watchVideoBtn found: " + (watchVideoBtn != null));
        android.util.Log.d("TutorialPage", "playPauseBtn found: " + (playPauseBtn != null));
        android.util.Log.d("TutorialPage", "tutorialImage found: " + (tutorialImage != null));
        android.util.Log.d("TutorialPage", "tutorialVideoView found: " + (tutorialVideoView != null));
        android.util.Log.d("TutorialPage", "videoSeekBar found: " + (videoSeekBar != null));
        android.util.Log.d("TutorialPage", "videoTimeText found: " + (videoTimeText != null));
        android.util.Log.d("TutorialPage", "videoLoadingProgress found: " + (videoLoadingProgress != null));

        // Setup video player if found
        if (tutorialVideoView != null) {
            setupVideoPlayer();
        }
    }

    private void setupVideoPlayer() {
        try {
            // Load video from raw folder
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.video_script_202505310532;
            Uri videoUri = Uri.parse(videoPath);

            tutorialVideoView.setVideoURI(videoUri);

            // Prepare the video asynchronously
            tutorialVideoView.setOnPreparedListener(mp -> {
                int duration = tutorialVideoView.getDuration();
                videoSeekBar.setMax(duration);
                videoTimeText.setText("00:00 / " + formatTime(duration));
                // Removed unwanted "Video ready to play!" popup message

                // Enable controls after video is prepared
                playPauseBtn.setEnabled(true);
                videoSeekBar.setEnabled(true);
                videoLoadingProgress.setVisibility(View.GONE); // Hide loading indicator
            });

            // Set up video completion listener
            tutorialVideoView.setOnCompletionListener(mp -> {
                playPauseBtn.setText("▶️ Play");
                isPlaying = false;
                videoSeekBar.setProgress(0);
                videoTimeText.setText("00:00 / " + formatTime(tutorialVideoView.getDuration()));
                // Removed unwanted "Video completed!" popup message
            });

            // Set up error listener with better error handling
            tutorialVideoView.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("TutorialPage", "Video error: what=" + what + ", extra=" + extra);
                String errorMsg = "Video error: ";
                switch (what) {
                    case android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        errorMsg += "Unknown error";
                        break;
                    case android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        errorMsg += "Server connection died";
                        break;
                    default:
                        errorMsg += "Error code " + what;
                }
                Toast.makeText(this, "❌ " + errorMsg, Toast.LENGTH_LONG).show();

                // Disable controls on error
                playPauseBtn.setEnabled(false);
                videoSeekBar.setEnabled(false);
                videoLoadingProgress.setVisibility(View.GONE); // Hide loading indicator
                return true;
            });

            // Initially disable controls until video is prepared
            playPauseBtn.setEnabled(false);
            videoSeekBar.setEnabled(false);

            // Setup seek bar listener
            if (videoSeekBar != null) {
                videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && tutorialVideoView != null) {
                            tutorialVideoView.seekTo(progress);
                            videoTimeText.setText(formatTime(progress) + " / " + formatTime(tutorialVideoView.getDuration()));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Pause video while seeking
                        if (isPlaying && tutorialVideoView != null) {
                            tutorialVideoView.pause();
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Resume video after seeking if it was playing
                        if (isPlaying && tutorialVideoView != null) {
                            tutorialVideoView.start();
                        }
                    }
                });
            }

            // Start preparing the video
            android.util.Log.d("TutorialPage", "Starting video preparation...");

        } catch (Exception e) {
            android.util.Log.e("TutorialPage", "Error setting up video player: " + e.getMessage());
            Toast.makeText(this, "Error setting up video player: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Disable controls on setup error
            if (playPauseBtn != null) playPauseBtn.setEnabled(false);
            if (videoSeekBar != null) videoSeekBar.setEnabled(false);
        }
    }

    private void setupClickListeners() {
        // Only set click listeners if buttons exist
        if (getStartedBtn != null) {
            getStartedBtn.setOnClickListener(v -> {
                Toast.makeText(this, "🚀 Welcome to MyToDo! Let's get productive!", Toast.LENGTH_SHORT).show();
                navigateToHome();
            });
        }

        if (skipBtn != null) {
            skipBtn.setOnClickListener(v -> {
                Toast.makeText(this, "✅ Tutorial skipped. You can always access help later!", Toast.LENGTH_SHORT).show();
                navigateToHome();
            });
        }

        if (watchVideoBtn != null) {
            watchVideoBtn.setOnClickListener(v -> playTutorialVideo());
        }

        if (tutorialImage != null) {
            tutorialImage.setOnClickListener(v -> playTutorialVideo());
        }

        if (playPauseBtn != null) {
            playPauseBtn.setOnClickListener(v -> togglePlayPause());
        }

        // If no buttons are found, show tutorial steps dialog automatically after a short delay
        if (getStartedBtn == null && skipBtn == null && watchVideoBtn == null) {
            android.util.Log.d("TutorialPage", "No tutorial buttons found, showing tutorial dialog after delay");
            // Show tutorial dialog after a brief delay so the page loads properly
            new android.os.Handler().postDelayed(() -> {
                showTutorialStepsDialog();
            }, 500);
        }
    }

    private void welcomeUser() {
        // Get current user info for personalized welcome
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String welcomeMessage;

            if (displayName != null && !displayName.trim().isEmpty()) {
                welcomeMessage = "🎉 Welcome " + displayName + "! Ready to get organized?";
            } else {
                welcomeMessage = "🎉 Welcome to MyToDo! Ready to boost your productivity?";
            }

            Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void playTutorialVideo() {
        try {
            // Option 1: If you have a local video file in res/raw
            // Intent intent = new Intent(this, VideoPlayerActivity.class);
            // startActivity(intent);

            // Option 2: If you have a YouTube video or online video
            String videoUrl = "https://www.youtube.com/watch?v=YOUR_VIDEO_ID"; // Replace with your actual video URL
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Toast.makeText(this, "📺 Opening tutorial video...", Toast.LENGTH_SHORT).show();
            } else {
                // Fallback: Show tutorial steps dialog
                showTutorialStepsDialog();
            }

        } catch (Exception e) {
            android.util.Log.e("TutorialPage", "Error playing video: " + e.getMessage());
            showTutorialStepsDialog();
        }
    }

    private void showTutorialStepsDialog() {
        // Show a dialog with tutorial steps if video can't be played
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📚 Quick Tutorial Steps")
                .setMessage("Here's how to use MyToDo:\n\n" +
                        "1️⃣ **Create Tasks**: Tap the + button to add new tasks\n\n" +
                        "2️⃣ **Set Dates**: Choose due dates for your tasks\n\n" +
                        "3️⃣ **Location Reminders**: Add location-based notifications\n\n" +
                        "4️⃣ **Take Notes**: Capture ideas in the Notes section\n\n" +
                        "5️⃣ **Stay Organized**: Mark tasks complete as you finish them\n\n" +
                        "💡 **Tip**: Your data is private and synced across devices!")
                .setPositiveButton("🚀 Got It, Let's Start!", (dialog, which) -> {
                    navigateToHome();
                })
                .setNegativeButton("📖 Read More", (dialog, which) -> {
                    // Could open help documentation
                    Toast.makeText(this, "Help documentation coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close tutorial page so user can't go back to it
    }

    private void togglePlayPause() {
        if (isPlaying) {
            // Pause the video
            tutorialVideoView.pause();
            playPauseBtn.setText("▶️ Play");
            isPlaying = false;
            // Removed unwanted "Video paused" popup message
        } else {
            // Play the video
            tutorialVideoView.start();
            playPauseBtn.setText("⏸️ Pause");
            isPlaying = true;
            // Removed unwanted "Video playing" popup message

            // Update seek bar and time text
            updateSeekBar();
        }
    }


    private void updateSeekBar() {
        // Update the seek bar and time text while video is playing
        if (isPlaying) {
            int currentPosition = tutorialVideoView.getCurrentPosition();
            int duration = tutorialVideoView.getDuration();

            videoSeekBar.setMax(duration);
            videoSeekBar.setProgress(currentPosition);

            // Update time text
            videoTimeText.setText(formatTime(currentPosition) + " / " + formatTime(duration));

            // Repeat this task every second
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private String formatTime(int milliseconds) {
        // Convert milliseconds to mm:ss format
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


    // Legacy method for XML onClick compatibility
    public void gotoHome(View view) {
        navigateToHome();
    }
}