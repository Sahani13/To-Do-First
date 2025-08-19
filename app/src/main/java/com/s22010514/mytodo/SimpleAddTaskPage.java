package com.s22010514.mytodo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class SimpleAddTaskPage extends AppCompatActivity {

    // Define UI elements
    private EditText taskTitleInput;
    private EditText taskDescriptionInput;
    private Button chooseDateButton;
    private TextView selectedDateTextView;
    private MaterialButton saveTaskButton;
    private MaterialButton cancelTaskButton;

    // Priority buttons
    private MaterialButton lowPriorityBtn;
    private MaterialButton mediumPriorityBtn;
    private MaterialButton highPriorityBtn;

    private int taskId = -1;

    // Define DatabaseHelper instance
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // Variable to store selected date in milliseconds
    private long selectedDateInMillis;

    // Variable to store selected priority
    private String selectedPriority = "Medium"; // Default priority

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Intent intent = new Intent(SimpleAddTaskPage.this, LoginPage.class);
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

        // Set default priority selection (Medium)
        selectPriorityButton(mediumPriorityBtn, "Medium");

        // Set up priority button listeners
        setupPriorityButtons();

        // Check if an update is required
        checkForEditIntent();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        taskTitleInput = findViewById(R.id.taskTitleInput);
        taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        chooseDateButton = findViewById(R.id.chooseDateButton);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        saveTaskButton = findViewById(R.id.saveTaskButton);
        cancelTaskButton = findViewById(R.id.cancelTaskButton);

        // Initialize priority buttons
        lowPriorityBtn = findViewById(R.id.lowPriorityBtn);
        mediumPriorityBtn = findViewById(R.id.mediumPriorityBtn);
        highPriorityBtn = findViewById(R.id.highPriorityBtn);
    }

    private void setupClickListeners() {
        // Set an OnClickListener for the date button
        chooseDateButton.setOnClickListener(v -> showDatePickerDialog());

        // Set an OnClickListener for the cancel button
        cancelTaskButton.setOnClickListener(v -> finish());

        // Set an OnClickListener for the save button
        saveTaskButton.setOnClickListener(v -> saveTask());
    }

    private void checkForEditIntent() {
        if (getIntent().hasExtra("taskId")) {
            taskId = getIntent().getIntExtra("taskId", -1);
            String taskTitle = getIntent().getStringExtra("taskTitle");
            String taskDescription = getIntent().getStringExtra("taskDescription");
            selectedDateInMillis = getIntent().getLongExtra("taskDate", 0);

            // Set the retrieved data to the UI elements
            if (taskTitle != null) taskTitleInput.setText(taskTitle);
            if (taskDescription != null) taskDescriptionInput.setText(taskDescription);
            if (selectedDateInMillis != 0) selectedDateTextView.setText(formatDate(selectedDateInMillis));

            // Update button text for editing
            saveTaskButton.setText("Update Task");
        }
    }

    private void saveTask() {
        // Ensure user is authenticated before saving data
        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        String taskTitle = taskTitleInput.getText().toString().trim();
        String taskDescription = taskDescriptionInput.getText().toString().trim();

        // Validation
        if (taskTitle.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateInMillis == 0) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean result;
        if (taskId == -1) {
            // New task - include user ID for security
            result = dbHelper.insertTask(taskTitle, taskDescription, selectedDateInMillis, currentUserId);
            if (result) {
                Toast.makeText(this, "✅ Task saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "❌ Failed to save task", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing task - include user ID for security
            result = dbHelper.updateTaskById(taskId, taskTitle, taskDescription, selectedDateInMillis, currentUserId);
            if (result) {
                Toast.makeText(this, "✅ Task updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "❌ Failed to update task or unauthorized access", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(SimpleAddTaskPage.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    // Method to setup priority button click listeners
    private void setupPriorityButtons() {
        lowPriorityBtn.setOnClickListener(v -> selectPriorityButton(lowPriorityBtn, "Low"));
        mediumPriorityBtn.setOnClickListener(v -> selectPriorityButton(mediumPriorityBtn, "Medium"));
        highPriorityBtn.setOnClickListener(v -> selectPriorityButton(highPriorityBtn, "High"));
    }

    // Method to handle priority button selection
    private void selectPriorityButton(MaterialButton selectedButton, String priority) {
        // Reset all buttons to default state
        resetPriorityButtons();

        // Set selected priority
        selectedPriority = priority;

        // Update selected button appearance
        switch (priority) {
            case "Low":
                selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
                selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
            case "Medium":
                selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_orange_light));
                selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
            case "High":
                selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_light));
                selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
        }
    }

    // Method to reset all priority buttons to default state
    private void resetPriorityButtons() {
        // Reset Low priority button
        lowPriorityBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        lowPriorityBtn.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));

        // Reset Medium priority button
        mediumPriorityBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        mediumPriorityBtn.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));

        // Reset High priority button
        highPriorityBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        highPriorityBtn.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    // Method to show a date picker dialog
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(selectedYear, selectedMonth, selectedDay);
            selectedDateInMillis = selectedDate.getTimeInMillis();
            selectedDateTextView.setText(formatDate(selectedDateInMillis));
        }, year, month, day);

        datePickerDialog.show();
    }

    // Method to format date from milliseconds to a readable format
    private String formatDate(long dateInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return day + "/" + month + "/" + year;
    }
}
