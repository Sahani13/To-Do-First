package com.s22010514.mytodo;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * Alternative email service using EmailJS
 * This provides more reliable email delivery than Firebase Auth
 * Uses modern threading instead of deprecated AsyncTask
 */
public class EmailService {

    private static final String TAG = "EmailService";
    private static final String EMAILJS_SERVICE_ID = "service_0eo0oac";
    private static final String EMAILJS_TEMPLATE_ID = "template_n9qptc3";
    private static final String EMAILJS_PUBLIC_KEY = "BmASaUJh2XtpiY6Ll";
    private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";

    private final ExecutorService executor;
    private final Handler mainHandler;

    public EmailService() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Send password reset email using EmailJS service
     */
    public void sendPasswordResetEmail(String userEmail, String resetLink, EmailCallback callback) {
        Log.d(TAG, "=== EMAILJS DEBUG START ===");
        Log.d(TAG, "Sending reset email to: " + userEmail);
        Log.d(TAG, "Using service ID: " + EMAILJS_SERVICE_ID);
        Log.d(TAG, "Using template ID: " + EMAILJS_TEMPLATE_ID);

        executor.execute(() -> {
            String errorMessage = "";
            boolean success = false;

            try {
                // Create JSON payload
                JSONObject emailData = new JSONObject();
                emailData.put("service_id", EMAILJS_SERVICE_ID);
                emailData.put("template_id", EMAILJS_TEMPLATE_ID);
                emailData.put("user_id", EMAILJS_PUBLIC_KEY); // Correct field name

                // Match variables with your EmailJS template
                JSONObject templateParams = new JSONObject();
                templateParams.put("email", userEmail);  // matches {{email}}
                templateParams.put("link", resetLink);   // matches {{link}}
                templateParams.put("app_name", "MyToDo App");
                templateParams.put("from_name", "MyToDo Support");
                templateParams.put("message", "Click the link below to reset your password:");

                emailData.put("template_params", templateParams);

                Log.d(TAG, "JSON payload: " + emailData.toString());

                // Send HTTP request
                String response = sendHttpRequest(emailData.toString());
                success = (response != null && response.contains("OK"));

                if (!success) {
                    errorMessage = (response != null) ? response : "Unknown error";
                }

            } catch (Exception e) {
                Log.e(TAG, "Email preparation failed: " + e.getMessage());
                errorMessage = "Failed to prepare email: " + e.getMessage();
                success = false;
            }

            // Return result on main thread
            final boolean finalSuccess = success;
            final String finalError = errorMessage;
            mainHandler.post(() -> {
                if (callback != null) {
                    if (finalSuccess) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(finalError);
                    }
                }
            });

            Log.d(TAG, "=== EMAILJS DEBUG END ===");
        });
    }

    /**
     * Sends POST request to EmailJS API
     * Returns response body or null if network error
     */
    private String sendHttpRequest(String jsonData) {
        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "Connecting to EmailJS API...");
            URL url = new URL(EMAILJS_URL);
            connection = (HttpURLConnection) url.openConnection();

            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "MyToDo-Android-App");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // Send JSON data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            // Get response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "EmailJS response code: " + responseCode);

            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                response.append(responseLine.trim());
            }
            reader.close();

            Log.d(TAG, "EmailJS response: " + response.toString());
            return response.toString();

        } catch (IOException e) {
            Log.e(TAG, "❌ Network error: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Send welcome email to new users
     */
    public void sendWelcomeEmail(String userEmail, String userName, EmailCallback callback) {
        Log.d(TAG, "Welcome email would be sent to: " + userEmail);
        // For now, just return success. Implement if needed.
        if (callback != null) {
            callback.onSuccess();
        }
    }

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
