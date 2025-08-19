package com.s22010514.mytodo;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Email delivery testing utility
 * Use this to test and debug email delivery issues
 */
public class EmailTestHelper {

    private static final String TAG = "EmailTestHelper";

    /**
     * Test Firebase email delivery with detailed logging
     */
    public static void testFirebaseEmailDelivery(String testEmail, Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        Log.d(TAG, "=== EMAIL DELIVERY TEST START ===");
        Log.d(TAG, "Test email: " + testEmail);
        Log.d(TAG, "Firebase project: " + context.getPackageName());

        auth.sendPasswordResetEmail(testEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Firebase reports: Email sent successfully");
                        Log.d(TAG, "📧 Check the following locations:");
                        Log.d(TAG, "   • Primary inbox");
                        Log.d(TAG, "   • Spam/Junk folder");
                        Log.d(TAG, "   • Promotions tab (Gmail)");
                        Log.d(TAG, "   • All Mail folder");
                    } else {
                        Log.e(TAG, "❌ Firebase reports: Email failed");
                        if (task.getException() != null) {
                            Log.e(TAG, "Error: " + task.getException().getMessage());
                        }
                    }
                    Log.d(TAG, "=== EMAIL DELIVERY TEST END ===");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Email sending failed with exception: " + e.getMessage());
                    Log.e(TAG, "Exception type: " + e.getClass().getSimpleName());
                    Log.d(TAG, "=== EMAIL DELIVERY TEST END ===");
                });
    }

    /**
     * Check common email delivery issues
     */
    public static void checkEmailDeliveryIssues(String email) {
        Log.d(TAG, "=== EMAIL DELIVERY DIAGNOSIS ===");

        // Check email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w(TAG, "⚠️ Invalid email format: " + email);
        } else {
            Log.d(TAG, "✅ Email format is valid");
        }

        // Check email provider
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        switch (domain) {
            case "gmail.com":
                Log.d(TAG, "📧 Gmail provider - Check Promotions/Spam tabs");
                break;
            case "yahoo.com":
            case "yahoo.co.uk":
                Log.d(TAG, "📧 Yahoo provider - Often blocks automated emails");
                break;
            case "outlook.com":
            case "hotmail.com":
            case "live.com":
                Log.d(TAG, "📧 Microsoft provider - Check Junk folder");
                break;
            default:
                Log.d(TAG, "📧 Other provider (" + domain + ") - Check spam folder");
                break;
        }

        Log.d(TAG, "=== DIAGNOSIS END ===");
    }
}
