package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ActionCodeSettings;
import android.app.AlertDialog;

public class ForgotPassword extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText emailInput;
    private MaterialButton resetPasswordBtn;
    private MaterialButton backToLoginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views with null checks
        try {
            initializeViews();
            setupClickListeners();
        } catch (Exception e) {
            android.util.Log.e("ForgotPassword", "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error loading page. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailResetInput);
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn);
        backToLoginBtn = findViewById(R.id.backToLoginBtn);

        // Verify views were found
        if (emailInput == null || resetPasswordBtn == null || backToLoginBtn == null) {
            android.util.Log.e("ForgotPassword", "One or more views not found in layout");
            throw new RuntimeException("Views not found in layout");
        }
    }

    private void setupClickListeners() {
        resetPasswordBtn.setOnClickListener(v -> sendPasswordResetEmail());
        backToLoginBtn.setOnClickListener(v -> goBackToLogin());
    }

    private void sendPasswordResetEmail() {
        if (emailInput == null || resetPasswordBtn == null) {
            Toast.makeText(this, "Error: Views not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";

        // Enhanced validation
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        resetPasswordBtn.setEnabled(false);
        resetPasswordBtn.setText("Sending...");

        // Enhanced debugging
        android.util.Log.d("ForgotPassword", "=== EMAIL RESET DEBUG START ===");
        android.util.Log.d("ForgotPassword", "Email: " + email);
        android.util.Log.d("ForgotPassword", "Testing email domain: " + email.substring(email.indexOf("@")));

        // SKIP user registration check for now - proceed directly to email sending
        // This avoids the "email not registered" issue
        android.util.Log.d("ForgotPassword", "🚀 Proceeding directly to email services (skipping registration check)");
        tryEmailJSFirst(email);
    }

    // Add a separate method for manual user check if needed
    private void checkUserRegistrationOptional(String email) {
        android.util.Log.d("ForgotPassword", "🔍 Checking if user is registered...");

        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean userExists = task.getResult().getSignInMethods().size() > 0;
                        android.util.Log.d("ForgotPassword", "User registration status: " + userExists);

                        if (!userExists) {
                            android.util.Log.w("ForgotPassword", "⚠️ Warning: Email might not be registered, but proceeding anyway");
                        }
                    } else {
                        android.util.Log.w("ForgotPassword", "⚠️ Could not verify user registration: " +
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void tryEmailJSFirst(String email) {
        // Optional: Check user registration in background (doesn't block email sending)
        checkUserRegistrationOptional(email);

        // Try EmailJS service first (more reliable)
        EmailService emailService = new EmailService();
        String resetLink = "https://mytodoapp.page.link/reset?email=" + email;

        android.util.Log.d("ForgotPassword", "🚀 Attempting EmailJS for: " + email);

        emailService.sendPasswordResetEmail(email, resetLink, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("ForgotPassword", "✅ EmailJS SUCCESS for: " + email);
                if (resetPasswordBtn != null) {
                    resetPasswordBtn.setEnabled(true);
                    resetPasswordBtn.setText("🔄 Send Reset Link");
                }
                showEmailJSSuccessDialog(email);
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("ForgotPassword", "❌ EmailJS FAILED for: " + email);
                android.util.Log.e("ForgotPassword", "❌ EmailJS Error details: " + error);

                // Always try Firebase as fallback regardless of EmailJS error
                android.util.Log.d("ForgotPassword", "🔄 Trying Firebase Auth as fallback...");
                sendFirebasePasswordReset(email);
            }
        });
    }

    private void sendFirebasePasswordReset(String email) {
        android.util.Log.d("ForgotPassword", "🔥 Attempting Firebase Auth for: " + email);

        // Use Firebase Auth directly - it will handle user existence check internally
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Reset button state
                    if (resetPasswordBtn != null) {
                        resetPasswordBtn.setEnabled(true);
                        resetPasswordBtn.setText("🔄 Send Reset Link");
                    }

                    if (task.isSuccessful()) {
                        android.util.Log.d("ForgotPassword", "✅ Firebase SUCCESS: Email sent to " + email);
                        showReliableSuccessDialog(email);
                    } else {
                        String errorMsg = task.getException() != null ?
                            task.getException().getMessage() : "Unknown error";
                        android.util.Log.e("ForgotPassword", "❌ Firebase ERROR: " + errorMsg);

                        // Check if it's a "user not found" error
                        if (errorMsg.contains("user-not-found") || errorMsg.contains("not found")) {
                            showUserNotFoundDialog(email);
                        } else {
                            showReliableErrorDialog(errorMsg, email);
                        }
                    }
                    android.util.Log.d("ForgotPassword", "=== EMAIL RESET DEBUG END ===");
                })
                .addOnFailureListener(e -> {
                    if (resetPasswordBtn != null) {
                        resetPasswordBtn.setEnabled(true);
                        resetPasswordBtn.setText("🔄 Send Reset Link");
                    }
                    android.util.Log.e("ForgotPassword", "❌ Firebase FAILURE: " + e.getMessage());
                    showReliableErrorDialog("Email service error: " + e.getMessage(), email);
                });
    }

    private void showEmailJSSuccessDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("📧 Reset Email Sent via EmailJS!")
                .setMessage("✅ Professional email service confirmed: Email sent to\n📧 " + email +
                           "\n\n🔍 CHECK YOUR EMAIL:\n" +
                           "• Primary inbox (most likely location)\n" +
                           "• EmailJS has better deliverability than Firebase\n" +
                           "• Should arrive within 2-5 minutes\n\n" +
                           "⏱️ TIMING:\n" +
                           "• Usually arrives quickly (2-5 minutes)\n" +
                           "• Professional email service\n" +
                           "• Less likely to be marked as spam\n\n" +
                           "💡 TIPS:\n" +
                           "• Look for sender: MyToDo Support\n" +
                           "• Click the reset link in the email\n" +
                           "• Link will redirect to password reset page")
                .setPositiveButton("📱 Open Email App", (dialog, which) -> openEmailApp())
                .setNeutralButton("🔄 Send Again", (dialog, which) -> sendPasswordResetEmail())
                .setNegativeButton("✅ Got It", null)
                .setCancelable(false)
                .show();
    }

    private void showReliableSuccessDialog(String email) {
        String emailProvider = email.substring(email.indexOf("@") + 1).toLowerCase();
        String specificInstructions = getEmailProviderInstructions(emailProvider);

        new AlertDialog.Builder(this)
                .setTitle("📧 Password Reset Email Sent!")
                .setMessage("✅ Firebase confirmed: Email sent to\n📧 " + email +
                           "\n\n🔍 CHECK THESE LOCATIONS:\n" +
                           specificInstructions +
                           "\n\n⏱️ TIMING:\n" +
                           "• Can take 2-10 minutes to arrive\n" +
                           "• Sometimes up to 30 minutes\n" +
                           "• Check every few minutes\n\n" +
                           "🚨 IMPORTANT:\n" +
                           "• 90% of missing emails are in SPAM folder\n" +
                           "• Search for 'password reset' in your email\n" +
                           "• Look for sender: noreply@*.firebaseapp.com")
                .setPositiveButton("📱 Open Email App", (dialog, which) -> openEmailApp())
                .setNeutralButton("🔍 Search Email", (dialog, which) -> searchForEmail(email))
                .setNegativeButton("🔄 Send Again", (dialog, which) -> sendPasswordResetEmail())
                .setCancelable(false)
                .show();
    }

    private String getEmailProviderInstructions(String domain) {
        switch (domain) {
            case "gmail.com":
                return "Gmail Users:\n" +
                       "• Check 'Promotions' tab ⭐\n" +
                       "• Check 'Spam' folder ⚠️\n" +
                       "• Check 'All Mail' folder\n" +
                       "• Use search: 'password reset'";
            case "yahoo.com":
            case "yahoo.co.uk":
                return "Yahoo Users:\n" +
                       "• Check 'Spam' folder FIRST ⚠️\n" +
                       "• Check 'Bulk' folder\n" +
                       "• Yahoo often blocks Firebase emails\n" +
                       "• Try different email if persistent";
            case "outlook.com":
            case "hotmail.com":
            case "live.com":
                return "Microsoft Users:\n" +
                       "• Check 'Junk Email' folder ⚠️\n" +
                       "• Check 'Deleted Items'\n" +
                       "• Check 'Clutter' folder\n" +
                       "• Microsoft often blocks automated emails";
            default:
                return "General Instructions:\n" +
                       "• Check SPAM/Junk folder FIRST ⚠️\n" +
                       "• Check all email folders\n" +
                       "• Search for 'password reset'\n" +
                       "• Contact your email provider if missing";
        }
    }

    private void showReliableErrorDialog(String errorMessage, String email) {
        String solution = getErrorSolution(errorMessage);

        new AlertDialog.Builder(this)
                .setTitle("❌ Email Delivery Problem")
                .setMessage("Failed to send reset email to:\n📧 " + email +
                           "\n\n🔍 Error Details:\n" + errorMessage +
                           "\n\n" + solution)
                .setPositiveButton("🔄 Try Again", (dialog, which) -> sendPasswordResetEmail())
                .setNeutralButton("✏️ Try Different Email", (dialog, which) -> {
                    emailInput.setText("");
                    emailInput.requestFocus();
                })
                .setNegativeButton("🏠 Back to Login", (dialog, which) -> goBackToLogin())
                .show();
    }

    private String getErrorSolution(String error) {
        if (error.contains("user-not-found") || error.contains("not found")) {
            return "💡 SOLUTION:\n" +
                   "• This email is not registered\n" +
                   "• Double-check spelling\n" +
                   "• Try a different email\n" +
                   "• Sign up for new account";
        } else if (error.contains("too-many-requests")) {
            return "💡 SOLUTION:\n" +
                   "• Wait 15-30 minutes\n" +
                   "• Check if email already arrived\n" +
                   "• Try from different device";
        } else {
            return "💡 SOLUTIONS:\n" +
                   "• Check internet connection\n" +
                   "• Try again in 5 minutes\n" +
                   "• Use different email provider\n" +
                   "• Contact support if persistent";
        }
    }

    private void openEmailApp() {
        try {
            // Create intent with proper flags for better back navigation
            Intent emailIntent = new Intent(Intent.ACTION_MAIN);
            emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Try to start email app with chooser
            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(emailIntent, "Choose Email App"));
            } else {
                // Fallback: Try specific Gmail intent
                openGmailSpecifically();
            }
        } catch (Exception e) {
            android.util.Log.e("ForgotPassword", "Failed to open email app: " + e.getMessage());
            openGmailSpecifically();
        }
    }

    private void openGmailSpecifically() {
        try {
            // Try Gmail-specific intent first
            Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            if (gmailIntent != null) {
                gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(gmailIntent);

                // Show helpful message
                Toast.makeText(this, "📧 Opening Gmail - Check your Spam folder!\n👈 Use Android back button to return to app",
                    Toast.LENGTH_LONG).show();
            } else {
                // Gmail not installed, try web Gmail
                openWebGmail();
            }
        } catch (Exception e) {
            android.util.Log.e("ForgotPassword", "Failed to open Gmail: " + e.getMessage());
            openWebGmail();
        }
    }

    private void openWebGmail() {
        try {
            // Open Gmail in web browser as last resort
            Intent webIntent = new Intent(Intent.ACTION_VIEW);
            webIntent.setData(android.net.Uri.parse("https://mail.google.com"));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(webIntent);

            Toast.makeText(this, "📧 Opening Gmail in browser - Check your Spam folder!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Final fallback
            Toast.makeText(this, "📧 Please manually open your email app and check Spam folder",
                Toast.LENGTH_LONG).show();
        }
    }

    // Enhanced search method with better navigation
    private void searchForEmail(String email) {
        try {
            // Show instruction dialog first
            new AlertDialog.Builder(this)
                .setTitle("🔍 How to Search for Reset Email")
                .setMessage("To find your password reset email:\n\n" +
                           "1️⃣ Open your email app\n" +
                           "2️⃣ Check SPAM folder first\n" +
                           "3️⃣ Search for: 'password reset'\n" +
                           "4️⃣ Look for sender: MyToDo Support\n\n" +
                           "💡 Use Android back button (◀) to return to this app")
                .setPositiveButton("📱 Open Email App", (dialog, which) -> {
                    openEmailApp();
                })
                .setNegativeButton("✅ Got It", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Please search 'password reset' in your email app", Toast.LENGTH_LONG).show();
        }
    }

    // Override back button handling to ensure proper navigation
    @Override
    public void onBackPressed() {
        // Show confirmation dialog before going back
        new AlertDialog.Builder(this)
            .setTitle("🔙 Go Back?")
            .setMessage("Do you want to go back to the login page?")
            .setPositiveButton("Yes", (dialog, which) -> {
                super.onBackPressed();
            })
            .setNegativeButton("Stay Here", null)
            .show();
    }

    private void goBackToLogin() {
        Intent intent = new Intent(ForgotPassword.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    private void showUserNotFoundDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("❌ Email Not Registered")
                .setMessage("The email address is not registered:\n📧 " + email +
                           "\n\n💡 SOLUTIONS:\n" +
                           "• Double-check the email spelling\n" +
                           "• Try a different email address\n" +
                           "• Sign up for a new account\n" +
                           "• Contact support if you believe this is an error")
                .setPositiveButton("✏️ Try Different Email", (dialog, which) -> {
                    emailInput.setText("");
                    emailInput.requestFocus();
                })
                .setNeutralButton("📝 Sign Up", (dialog, which) -> {
                    Intent intent = new Intent(ForgotPassword.this, SignupPage.class);
                    startActivity(intent);
                })
                .setNegativeButton("🏠 Back to Login", (dialog, which) -> goBackToLogin())
                .show();
    }

    private void showEmailJSConfigError(String email, String error) {
        new AlertDialog.Builder(this)
                .setTitle("⚙️ EmailJS Configuration Issue")
                .setMessage("There's an issue with the EmailJS configuration:\n\n" +
                           "📧 Email: " + email + "\n\n" +
                           "🔍 Error: " + error + "\n\n" +
                           "💡 SOLUTIONS:\n" +
                           "• EmailJS service might need configuration\n" +
                           "• Template variables might not match\n" +
                           "• Service limits might be reached\n" +
                           "• Trying Firebase Auth as backup...")
                .setPositiveButton("🔄 Try Firebase", (dialog, which) -> {
                    sendFirebasePasswordReset(email);
                })
                .setNeutralButton("✏️ Try Different Email", (dialog, which) -> {
                    emailInput.setText("");
                    emailInput.requestFocus();
                })
                .setNegativeButton("🏠 Back to Login", (dialog, which) -> goBackToLogin())
                .show();
    }

}