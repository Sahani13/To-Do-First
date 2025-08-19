package com.s22010514.mytodo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class NotesPage extends AppCompatActivity implements NoteAdapter.OnNoteDeletedListener {

    private RecyclerView notesRecyclerView;
    private NoteAdapter noteAdapter;
    private List<Note> noteList;
    private DatabaseHelper dbHelper;
    private TextView noteCountTxt;
    private LinearLayout emptyStateLayout;
    private MaterialButton addNewNoteBtn;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Intent intent = new Intent(NotesPage.this, LoginPage.class);
            startActivity(intent);
            finish();
            return;
        }

        // Get current user ID for data filtering
        currentUserId = currentUser.getUid();

        // Initialize UI elements
        initializeViews();

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Initialize the list to hold Notes
        noteList = new ArrayList<>();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up click listeners
        setupClickListeners();

        // Load notes from database
        loadNotesFromDatabase();
        updateUI();
    }

    private void initializeViews() {
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        noteCountTxt = findViewById(R.id.noteCountTxt);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        addNewNoteBtn = findViewById(R.id.addNewNoteBtn);
    }

    private void setupRecyclerView() {
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(noteList, this);
        noteAdapter.setOnNoteDeletedListener(this);
        notesRecyclerView.setAdapter(noteAdapter);
    }

    private void setupClickListeners() {
        addNewNoteBtn.setOnClickListener(v -> {
            Intent intent = new Intent(NotesPage.this, AddNotePage.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the notes list when returning from AddNotePage
        refreshNotesList();
    }

    @Override
    public void onNoteDeleted() {
        // This method is called when a note is deleted from the adapter
        updateUI();
    }

    // Method to refresh the notes list
    private void refreshNotesList() {
        noteList.clear();
        loadNotesFromDatabase();
        noteAdapter.notifyDataSetChanged();
        updateUI();
    }

    // Method to load notes from the database with user filtering
    private void loadNotesFromDatabase() {
        // Ensure user is authenticated before loading data
        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        // Get a cursor to notes for the current user only
        Cursor cursor = dbHelper.getAllNotes(currentUserId);

        // Check if the cursor has data
        if (cursor.moveToFirst()) {
            // Iterate through the cursor and populate the notes list
            do {
                int colNoteId = cursor.getColumnIndex(DatabaseHelper.COL_1);
                int colNoteTitle = cursor.getColumnIndex(DatabaseHelper.COL_2);
                int colNoteBody = cursor.getColumnIndex(DatabaseHelper.COL_3);

                if (colNoteId != -1 && colNoteTitle != -1 && colNoteBody != -1) {
                    int id = cursor.getInt(colNoteId);
                    String title = cursor.getString(colNoteTitle);
                    String body = cursor.getString(colNoteBody);
                    Note note = new Note(id, title, body);
                    noteList.add(note);
                }
            } while (cursor.moveToNext());
        }

        // Close the cursor to free resources
        cursor.close();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(NotesPage.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    // Method to update UI based on notes list state
    private void updateUI() {
        int noteCount = noteList.size();

        if (noteCount == 0) {
            noteCountTxt.setText("No notes");
            emptyStateLayout.setVisibility(View.VISIBLE);
            notesRecyclerView.setVisibility(View.GONE);
        } else {
            noteCountTxt.setText(noteCount + (noteCount == 1 ? " note" : " notes"));
            emptyStateLayout.setVisibility(View.GONE);
            notesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
