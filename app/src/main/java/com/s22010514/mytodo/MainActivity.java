package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    // Declare sensor management light class
    private sensorManagerHelper sensorManagerHelper;
    private FirebaseAuth mAuth;
    private LinearLayout authButtonsLayout;
    private MaterialButton getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        authButtonsLayout = findViewById(R.id.authButtonsLayout);
        getStartedButton = findViewById(R.id.button);

        // Initialize the sensorManagerHelper instance
        sensorManagerHelper = sensorManagerHelper.getInstance(this);

        // Check if user is already signed in and update UI accordingly
        checkUserAuthentication();
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is already signed in
            android.util.Log.d("MainActivity", "User already authenticated: " + currentUser.getEmail());

            // Hide login/signup buttons
            authButtonsLayout.setVisibility(View.GONE);

            // Update Get Started button to go directly to home
            getStartedButton.setText("🚀 Continue to App");
            getStartedButton.setOnClickListener(v -> {
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                navigateToHome();
            });

            // Show welcome back message
            Toast.makeText(this, "Welcome back! Tap 'Continue to App' to proceed.", Toast.LENGTH_LONG).show();

        } else {
            // User is not signed in, show normal authentication flow
            android.util.Log.d("MainActivity", "No user authenticated, showing auth buttons");

            // Show login/signup buttons
            authButtonsLayout.setVisibility(View.VISIBLE);

            // Keep original Get Started button functionality
            getStartedButton.setText("Get Started");
            getStartedButton.setOnClickListener(v -> gotoSign(v));
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close MainActivity so user can't go back
    }

    // Override onResume to register the listener
    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener when the activity resumes
        if (sensorManagerHelper != null) {
            sensorManagerHelper.registerListener();
        }
    }




    // Override onPause to unregister the listener
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener when the activity pauses
        if (sensorManagerHelper != null) {
            sensorManagerHelper.unregisterListener();
        }
    }

    // Method to handle start button click
    public void gotoSign(View view) {
        // Navigate to the Signup page
        Intent intent = new Intent(this, SignupPage.class);
        startActivity(intent);
    }

    // Method to handle login button click
    public void gotoLogin(View view) {
        // Navigate to the Login page
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
    }

    // Method to handle signup button click
    public void gotoSignup(View view) {
        // Navigate to the Signup page
        Intent intent = new Intent(this, SignupPage.class);
        startActivity(intent);
    }
}
