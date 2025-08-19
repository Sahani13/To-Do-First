package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button signupButton;

    @Override
    public void onStart() {
        super.onStart();
        // Initialize Firebase Auth first
        mAuth = FirebaseAuth.getInstance();

        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If user is already signed in navigate to the home page
            Intent intent = new Intent(SignupPage.this, HomePage.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find views
        TextView haveAccountTextView = findViewById(R.id.haveAccountTxt);
        EditText userNameEditText = findViewById(R.id.nameInput);
        EditText emailEditText = findViewById(R.id.emailSignupPgInput);
        EditText passwordEditText = findViewById(R.id.passwordSignupPgInput);
        EditText retypePasswordEditText = findViewById(R.id.reenterPswdSignupPgInput);
        signupButton = findViewById(R.id.signupBtn);

        // Set click listener for sign up button
        signupButton.setOnClickListener(v -> {
            // Get user inputs
            String username = userNameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            String retypePassword = retypePasswordEditText.getText().toString();

            // Validate inputs
            if (!validateInputs(username, email, password, retypePassword)) {
                return;
            }

            // Show loading state
            setLoadingState(true);

            // Create user with email and password
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            // Sign up successful
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Update user profile with display name
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Toast.makeText(SignupPage.this,
                                                    "✅ Account created successfully! Welcome " + username,
                                                    Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                // Send email verification
                                user.sendEmailVerification()
                                        .addOnCompleteListener(emailTask -> {
                                            if (emailTask.isSuccessful()) {
                                                Toast.makeText(SignupPage.this,
                                                    "📧 Verification email sent to " + email,
                                                    Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }

                            // Navigate to tutorial page
                            Intent intent = new Intent(SignupPage.this, TutorialPage.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // Sign up failed - show specific error
                            String errorMessage = "Sign up failed. Please try again.";
                            if (task.getException() != null) {
                                String firebaseError = task.getException().getMessage();
                                errorMessage = getReadableErrorMessage(firebaseError);
                            }
                            Toast.makeText(SignupPage.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Set click listener for already have an account txt
        haveAccountTextView.setOnClickListener(view -> {
            Intent intent = new Intent(SignupPage.this, LoginPage.class);
            startActivity(intent);
        });
    }

    private boolean validateInputs(String username, String email, String password, String retypePassword) {
        // Check if any field is empty
        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (retypePassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate password strength
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if passwords match
        if (!password.equals(retypePassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate username length
        if (username.length() < 2) {
            Toast.makeText(this, "Name must be at least 2 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getReadableErrorMessage(String firebaseError) {
        if (firebaseError == null) return "Sign up failed. Please try again.";

        if (firebaseError.contains("email-already-in-use")) {
            return "❌ This email is already registered. Try logging in instead.";
        } else if (firebaseError.contains("weak-password")) {
            return "❌ Password is too weak. Please use a stronger password.";
        } else if (firebaseError.contains("invalid-email")) {
            return "❌ Invalid email address format.";
        } else if (firebaseError.contains("network-request-failed")) {
            return "❌ Network error. Please check your internet connection.";
        } else {
            return "❌ " + firebaseError;
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (signupButton != null) {
            signupButton.setEnabled(!isLoading);
            signupButton.setText(isLoading ? "Creating Account..." : "Create Account");
        }
    }

    // Method to handle navigation to login page (for XML onClick)
    public void gotoLoginPage(View view) {
        Intent intent = new Intent(SignupPage.this, LoginPage.class);
        startActivity(intent);
    }
}
