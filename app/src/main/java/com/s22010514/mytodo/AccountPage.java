package com.s22010514.mytodo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.SharedPreferences;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class AccountPage extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String PREFS_NAME = "UserProfile";
    private static final String PROFILE_IMAGE_KEY = "profile_image_path";

    private FirebaseAuth mAuth;
    private TextView userDisplayName, userEmailDisplay;
    private TextInputEditText nameInput, emailInput;
    private ImageView profileImageView;
    private String currentPhotoPath;

    // Add TextViews for statistics
    private TextView tasksCount, notesCount, placesCount;
    private DatabaseHelper dbHelper;
    private String currentUserId;

    // Modern Activity Result API
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) {
                // Image captured successfully, load and display it
                setPic();
            }
        }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        isGranted -> {
            if (isGranted) {
                // Permission granted, open camera
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission required to take profile picture",
                    Toast.LENGTH_SHORT).show();
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Initialize views
        userDisplayName = findViewById(R.id.userDisplayName);
        userEmailDisplay = findViewById(R.id.userEmailDisplay);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        profileImageView = findViewById(R.id.profileImageView);
        CardView editImageBtn = findViewById(R.id.editImageBtn);

        // Initialize statistics TextViews
        tasksCount = findViewById(R.id.tasksCount);
        notesCount = findViewById(R.id.notesCount);
        placesCount = findViewById(R.id.placesCount);

        // Set click listener for camera button
        editImageBtn.setOnClickListener(v -> openCamera());

        // Load user data and statistics
        loadUserData();
        loadUserStatistics();
    }

    private void openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission using modern API
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            // Permission already granted, open camera
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                    "com.s22010514.mytodo.fileprovider",
                    photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPic() {
        if (currentPhotoPath == null) return;

        // Get the dimensions of the View
        int targetW = profileImageView.getWidth();
        int targetH = profileImageView.getHeight();

        if (targetW == 0 || targetH == 0) {
            // View not measured yet, set default target size
            targetW = 300;
            targetH = 300;
        }

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);

            // Save profile picture to user-specific location
            saveProfilePicture(bitmap);

            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfilePicture(Bitmap bitmap) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        try {
            // Create user-specific directory
            File profileDir = new File(getFilesDir(), "profiles");
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }

            // Create user-specific filename using user UID
            String fileName = "profile_" + currentUser.getUid() + ".jpg";
            File profileFile = new File(profileDir, fileName);

            // Save bitmap to internal storage
            FileOutputStream fos = new FileOutputStream(profileFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();

            // Save the file path in SharedPreferences with user-specific key
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROFILE_IMAGE_KEY + "_" + currentUser.getUid(), profileFile.getAbsolutePath());
            editor.apply();

            Toast.makeText(this, "✅ Profile picture saved successfully", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "❌ Failed to save profile picture", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Get user email
            String email = currentUser.getEmail();
            if (email != null) {
                userEmailDisplay.setText(email);
                emailInput.setText(email);
            }

            // Get user display name (if available)
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userDisplayName.setText(displayName);
                nameInput.setText(displayName);
            } else {
                // If no display name, extract name from email
                if (email != null) {
                    String nameFromEmail = email.substring(0, email.indexOf("@"));
                    userDisplayName.setText(nameFromEmail);
                    nameInput.setText(nameFromEmail);
                }
            }

            // Load saved profile picture
            loadSavedProfilePicture();

        } else {
            // No user logged in, redirect to main activity
            Intent intent = new Intent(AccountPage.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void loadSavedProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        try {
            // Get saved profile picture path from SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String savedPath = prefs.getString(PROFILE_IMAGE_KEY + "_" + currentUser.getUid(), null);

            if (savedPath != null) {
                File imageFile = new File(savedPath);
                if (imageFile.exists()) {
                    // Load and display the saved profile picture
                    Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                    if (bitmap != null) {
                        profileImageView.setImageBitmap(bitmap);
                        currentPhotoPath = savedPath; // Update current path
                    }
                } else {
                    // File doesn't exist, remove from preferences
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove(PROFILE_IMAGE_KEY + "_" + currentUser.getUid());
                    editor.apply();
                }
            }
        } catch (Exception e) {
            // If loading fails, just use default image
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) throw new IOException("No user logged in");

        // Create user-specific directory
        File profileDir = new File(getFilesDir(), "profiles");
        if (!profileDir.exists()) {
            profileDir.mkdirs();
        }

        // Create an image file name with timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "profile_temp_" + currentUser.getUid() + "_" + timeStamp;

        File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            profileDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Method to handle logout functionality
    public void logoutUser(View view) {
        // Sign out from Firebase
        mAuth.signOut();

        // Show logout confirmation message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to MainActivity (welcome page)
        Intent intent = new Intent(AccountPage.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Method to update user profile
    public void updateProfile(View view) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && nameInput.getText() != null) {
            String newName = nameInput.getText().toString().trim();

            if (!newName.isEmpty()) {
                // Update display name in Firebase
                currentUser.updateProfile(
                    new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                ).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userDisplayName.setText(newName);
                        Toast.makeText(AccountPage.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AccountPage.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserStatistics() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        currentUserId = currentUser.getUid();

        // Load real counts from database
        loadTasksCount();
        loadNotesCount();
        loadPlacesCount();
    }

    private void loadTasksCount() {
        try {
            Cursor cursor = dbHelper.getAllTasks(currentUserId);
            int count = cursor.getCount();
            cursor.close();
            tasksCount.setText(String.valueOf(count));
        } catch (Exception e) {
            tasksCount.setText("0");
            e.printStackTrace();
        }
    }

    private void loadNotesCount() {
        try {
            Cursor cursor = dbHelper.getAllNotes(currentUserId);
            int count = cursor.getCount();
            cursor.close();
            notesCount.setText(String.valueOf(count));
        } catch (Exception e) {
            notesCount.setText("0");
            e.printStackTrace();
        }
    }

    private void loadPlacesCount() {
        try {
            Cursor cursor = dbHelper.getAllLocationTasks(currentUserId);
            int count = cursor.getCount();
            cursor.close();
            placesCount.setText(String.valueOf(count));
        } catch (Exception e) {
            placesCount.setText("0");
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this page
        if (currentUserId != null) {
            loadUserStatistics();
        }
    }
}
