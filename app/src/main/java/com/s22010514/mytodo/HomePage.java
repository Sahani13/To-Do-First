package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomePage extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to the home pg layout
        setContentView(R.layout.activity_home_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize CardView variables locally since they're only used in onCreate
        CardView addTaskCard = findViewById(R.id.addTaskCard);
        CardView taskListCard = findViewById(R.id.taskListCard);
        CardView noteCard = findViewById(R.id.noteCard);
        CardView mapCard = findViewById(R.id.mapCard);
        CardView savedPlaceCard = findViewById(R.id.savedPlaceCard);
        CardView accountCard = findViewById(R.id.accountCard);

        // Initialize Tutorial FloatingActionButton with error handling
        FloatingActionButton tutorialFab = findViewById(R.id.tutorialFab);

        if (tutorialFab == null) {
            // Log error if button is not found
            android.util.Log.e("HomePage", "Tutorial FAB not found in layout!");
            Toast.makeText(this, "Tutorial button not found", Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.d("HomePage", "Tutorial FAB found successfully");
            // Set tutorial button click listener with error handling
            tutorialFab.setOnClickListener(v -> {
                try {
                    android.util.Log.d("HomePage", "Tutorial button clicked, navigating to TutorialPage");
                    Toast.makeText(HomePage.this, "🔄 Loading Tutorial Page...", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(HomePage.this, TutorialPage.class);
                    // Add flags to ensure proper navigation
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    android.util.Log.d("HomePage", "Tutorial page navigation started");
                } catch (Exception e) {
                    android.util.Log.e("HomePage", "Error navigating to tutorial: " + e.getMessage());
                    Toast.makeText(HomePage.this, "Error opening tutorial: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // Set onClickListeners using lambda expressions (modern approach)
        addTaskCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, AddTaskPage.class);
            startActivity(intent);
        });

        taskListCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, TaskListPage.class);
            startActivity(intent);
        });

        noteCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, NotesPage.class);
            startActivity(intent);
        });

        mapCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, MapPage.class);
            startActivity(intent);
        });

        savedPlaceCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, SavedPlacePage.class);
            startActivity(intent);
        });

        accountCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, AccountPage.class);
            startActivity(intent);
        });

        showWelcomeNotification();
    }

    private void showWelcomeNotification() {
        String channelId = "home_page_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                "Home Page Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
            channel.setSound(soundUri, null);
            notificationManager.createNotificationChannel(channel);
        }

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Welcome to MyToDo!")
            .setContentText("Your homepage is ready.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri);
        notificationManager.notify(100, builder.build());
    }

    // Method to handle logout functionality
    public void logoutUser(View view) {
        // Stop all location monitoring when user logs out
        Intent stopServiceIntent = new Intent(this, LocationNotificationService.class);
        stopServiceIntent.putExtra("stopAllTasks", true);
        startService(stopServiceIntent);

        // Sign out from Firebase
        mAuth.signOut();

        // Show logout confirmation message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to MainActivity (welcome page)
        Intent intent = new Intent(HomePage.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
