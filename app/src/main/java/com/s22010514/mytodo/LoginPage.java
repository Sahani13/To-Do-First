package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button loginButton;

    @Override
    public void onStart() {
        super.onStart();
        // Initialize Firebase Auth first
        mAuth = FirebaseAuth.getInstance();

        // Check if user is signed in (non-null) and update UI
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If user is already signed in, navigate to the home page
            Intent intent = new Intent(LoginPage.this, HomePage.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find views
        EditText emailEditText = findViewById(R.id.login_nameTxt);
        EditText passwordEditText = findViewById(R.id.login_pswdTxt);
        loginButton = findViewById(R.id.login);
        TextView forgotPasswordTextView = findViewById(R.id.forgotPswdTxt);

        // Set click listener for login button
        loginButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();

            // Validate inputs
            if (!validateInputs(email, password)) {
                return;
            }

            // Show loading state
            setLoadingState(true);

            // Sign in user with email and password
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            // Login successful
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Welcome message with user name (removed email verification check)
                                String displayName = user.getDisplayName();
                                String welcomeMessage = displayName != null ?
                                    "✅ Welcome back, " + displayName + "!" :
                                    "✅ Login successful!";
                                Toast.makeText(LoginPage.this, welcomeMessage, Toast.LENGTH_SHORT).show();
                            }

                            // Navigate to home page directly
                            Intent intent = new Intent(LoginPage.this, HomePage.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // Login failed - show specific error
                            String errorMessage = "Login failed. Please try again.";
                            if (task.getException() != null) {
                                String firebaseError = task.getException().getMessage();
                                errorMessage = getReadableErrorMessage(firebaseError);
                            }
                            Toast.makeText(LoginPage.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Set click listener for forgot password text
        forgotPasswordTextView.setOnClickListener(view -> {
            Intent intent = new Intent(LoginPage.this, ForgotPassword.class);
            startActivity(intent);
        });
    }

    private boolean validateInputs(String email, String password) {
        // Check if email is empty
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if password is empty
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getReadableErrorMessage(String firebaseError) {
        if (firebaseError == null) return "Login failed. Please try again.";

        if (firebaseError.contains("user-not-found")) {
            return "❌ No account found with this email. Please sign up first.";
        } else if (firebaseError.contains("wrong-password")) {
            return "❌ Incorrect password. Please try again or reset your password.";
        } else if (firebaseError.contains("invalid-email")) {
            return "❌ Invalid email address format.";
        } else if (firebaseError.contains("user-disabled")) {
            return "❌ This account has been disabled. Contact support.";
        } else if (firebaseError.contains("too-many-requests")) {
            return "❌ Too many failed attempts. Please try again later.";
        } else if (firebaseError.contains("network-request-failed")) {
            return "❌ Network error. Please check your internet connection.";
        } else {
            return "❌ " + firebaseError;
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (loginButton != null) {
            loginButton.setEnabled(!isLoading);
            loginButton.setText(isLoading ? "Signing In..." : "Sign In");
        }
    }

    // Method to handle signup navigation (for XML onClick)
    public void gotoSignupPage(android.view.View view) {
        Intent intent = new Intent(LoginPage.this, SignupPage.class);
        startActivity(intent);
    }
}
