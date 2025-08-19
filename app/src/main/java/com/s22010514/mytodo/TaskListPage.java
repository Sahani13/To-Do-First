package com.s22010514.mytodo;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class TaskListPage extends AppCompatActivity implements TaskAdapter.OnTaskDeletedListener {

    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private DatabaseHelper dbHelper;
    private TextView taskCountTxt;
    private LinearLayout emptyStateLayout;
    private MaterialButton addTaskBtn;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Intent intent = new Intent(TaskListPage.this, LoginPage.class);
            startActivity(intent);
            finish();
            return;
        }

        // Get current user ID for data filtering
        currentUserId = currentUser.getUid();

        // Initialize UI elements
        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        taskCountTxt = findViewById(R.id.taskCountTxt);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        addTaskBtn = findViewById(R.id.addTaskBtn);

        // Set up RecyclerView
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Initialize the list to hold Tasks
        taskList = new ArrayList<>();

        // Set up the RecyclerView and adapter
        taskAdapter = new TaskAdapter(taskList, this);
        taskAdapter.setOnTaskDeletedListener(this);
        taskRecyclerView.setAdapter(taskAdapter);

        // Set up the add task button
        addTaskBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TaskListPage.this, AddTaskPage.class);
            startActivity(intent);
        });

        // Load tasks from database
        loadTasksFromDatabase();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the task list when returning from AddTaskPage
        refreshTaskList();
    }

    @Override
    public void onTaskDeleted() {
        // This method is called when a task is deleted from the adapter
        updateUI();
    }

    // Method to refresh the task list
    private void refreshTaskList() {
        taskList.clear();
        loadTasksFromDatabase();
        taskAdapter.notifyDataSetChanged();
        updateUI();
    }

    // Method to load tasks from the database with user filtering
    private void loadTasksFromDatabase() {
        // Ensure user is authenticated before loading data
        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        // Get a cursor to tasks for the current user only
        Cursor cursor = dbHelper.getAllTasks(currentUserId);

        // Check if the cursor has data
        if (cursor.moveToFirst()) {
            // Iterate through the cursor and populate the task list
            do {
                int colTaskId = cursor.getColumnIndex(DatabaseHelper.TASK_COL_1);
                int colTaskTitle = cursor.getColumnIndex(DatabaseHelper.TASK_COL_2);
                int colTaskDescription = cursor.getColumnIndex(DatabaseHelper.TASK_COL_3);
                int colTaskDate = cursor.getColumnIndex(DatabaseHelper.TASK_COL_4);
                int colTaskCompleted = cursor.getColumnIndex(DatabaseHelper.TASK_COL_5);

                // Check if column indices are valid
                if (colTaskId != -1 && colTaskTitle != -1 && colTaskDescription != -1 && colTaskDate != -1) {
                    int taskId = cursor.getInt(colTaskId);
                    String taskTitle = cursor.getString(colTaskTitle);
                    String taskDescription = cursor.getString(colTaskDescription);
                    long taskDate = cursor.getLong(colTaskDate);
                    boolean isCompleted = colTaskCompleted != -1 && cursor.getInt(colTaskCompleted) == 1;

                    // Create a Task object and add it to the list
                    Task task = new Task(taskId, taskTitle, taskDescription, taskDate, isCompleted);
                    taskList.add(task);
                }
            } while (cursor.moveToNext());
        }

        // Close the cursor to free resources
        cursor.close();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(TaskListPage.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    // Method to update UI based on task list state
    private void updateUI() {
        int taskCount = taskList.size();

        // Update task count text
        if (taskCount == 0) {
            taskCountTxt.setText("No tasks");
            emptyStateLayout.setVisibility(View.VISIBLE);
            taskRecyclerView.setVisibility(View.GONE);
        } else {
            taskCountTxt.setText(taskCount + (taskCount == 1 ? " task" : " tasks"));
            emptyStateLayout.setVisibility(View.GONE);
            taskRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
