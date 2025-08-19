package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AddNotePage extends AppCompatActivity {

    private EditText noteTitleInput;
    private EditText noteBodyInput;
    private MaterialButton saveNoteButton;
    private MaterialButton cancelNoteButton;
    private DatabaseHelper dbHelper;
    private int noteId = -1;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Intent intent = new Intent(AddNotePage.this, LoginPage.class);
            startActivity(intent);
            finish();
            return;
        }

        // Get current user ID for data operations
        currentUserId = currentUser.getUid();

        // Initialize UI elements
        initializeViews();

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Check if editing an existing note
        checkForEditIntent();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        noteTitleInput = findViewById(R.id.noteTitleInput);
        noteBodyInput = findViewById(R.id.noteBodyInput);
        saveNoteButton = findViewById(R.id.saveNoteButton);
        cancelNoteButton = findViewById(R.id.cancelNoteButton);
    }

    private void checkForEditIntent() {
        if (getIntent().hasExtra("noteId")) {
            noteId = getIntent().getIntExtra("noteId", -1);
            String noteTitle = getIntent().getStringExtra("noteTitle");
            String noteBody = getIntent().getStringExtra("noteBody");

            // Set the retrieved data to the UI elements
            if (noteTitle != null) noteTitleInput.setText(noteTitle);
            if (noteBody != null) noteBodyInput.setText(noteBody);

            // Update button text for editing
            saveNoteButton.setText("Update Note");
        }
    }

    private void setupClickListeners() {
        saveNoteButton.setOnClickListener(v -> saveNote());
        cancelNoteButton.setOnClickListener(v -> finish());
    }

    private void saveNote() {
        // Ensure user is authenticated before saving data
        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        String title = noteTitleInput.getText().toString().trim();
        String body = noteBodyInput.getText().toString().trim();

        // Validation
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (body.isEmpty()) {
            Toast.makeText(this, "Please enter note content", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean result;
        if (noteId == -1) {
            // New note - include user ID
            result = dbHelper.insertNote(title, body, currentUserId);
            if (result) {
                Toast.makeText(this, "✅ Note saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "❌ Failed to save note", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing note - include user ID for security
            result = dbHelper.updateNoteById(noteId, title, body, currentUserId);
            if (result) {
                Toast.makeText(this, "✅ Note updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "❌ Failed to update note or unauthorized access", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(AddNotePage.this, LoginPage.class);
        startActivity(intent);
        finish();
    }
}
