package com.s22010514.mytodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;
    private DatabaseHelper dbHelper;
    private OnTaskDeletedListener onTaskDeletedListener;
    private FirebaseAuth mAuth;

    public interface OnTaskDeletedListener {
        void onTaskDeleted();
    }

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void setOnTaskDeletedListener(OnTaskDeletedListener listener) {
        this.onTaskDeletedListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, position);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Delete a task from the database with user authentication check
    private void deleteTask(Task task, int position) {
        // Check user authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "❌ Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Show modern confirmation dialog
        new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("🗑️ Delete Task")
                .setMessage("Are you sure you want to delete this task?\n\n\"" + task.getTitle() + "\"\n\nThis action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    // Use secure delete method with user ID verification
                    boolean isDeleted = dbHelper.deleteTaskById(task.getId(), currentUserId);
                    if (isDeleted) {
                        // Remove the task from the list and notify the adapter
                        taskList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, taskList.size());

                        // Notify the activity about the deletion
                        if (onTaskDeletedListener != null) {
                            onTaskDeletedListener.onTaskDeleted();
                        }

                        // Show a modern success message
                        Toast.makeText(context, "✅ Task deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        // Show a modern error message
                        Toast.makeText(context, "❌ Failed to delete task or unauthorized access", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Starts the AddTaskPage activity to update the task
    private void editTask(Task task) {
        Intent intent = new Intent(context, AddTaskPage.class);
        // Pass the task details to the AddTaskPage activity
        intent.putExtra("taskId", task.getId());
        intent.putExtra("taskTitle", task.getTitle());
        intent.putExtra("taskDescription", task.getDescription());
        intent.putExtra("taskDate", task.getDateInMillis());
        // Start the AddTaskPage activity
        context.startActivity(intent);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private TextView taskTitle;
        private TextView taskDescription;
        private TextView taskDate;
        private TextView taskDescriptionExpanded;
        private MaterialButton editTaskBtn;
        private MaterialButton deleteTaskBtn;
        private MaterialButton completeTaskBtn;
        private TextView completionStatusIcon;
        private boolean isExpanded = false;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskDate = itemView.findViewById(R.id.taskDate);
            taskDescriptionExpanded = itemView.findViewById(R.id.taskDescriptionExpanded);
            editTaskBtn = itemView.findViewById(R.id.editTaskBtn);
            deleteTaskBtn = itemView.findViewById(R.id.deleteTaskBtn);
            completeTaskBtn = itemView.findViewById(R.id.completeTaskBtn);
            completionStatusIcon = itemView.findViewById(R.id.completionStatusIcon);
        }

        public void bind(Task task, int position) {
            // Set task title with completion styling
            taskTitle.setText(task.getTitle());
            taskDate.setText(formatDate(task.getDateInMillis()));

            // Update completion status display
            updateCompletionStatus(task);

            // Handle description visibility
            String description = task.getDescription();
            if (description != null && !description.trim().isEmpty()) {
                taskDescription.setText(description);
                taskDescription.setVisibility(View.VISIBLE);
                taskDescriptionExpanded.setText(description);

                // Set up click listener for expanding/collapsing description
                itemView.setOnClickListener(v -> toggleDescription());
            } else {
                taskDescription.setVisibility(View.GONE);
                taskDescriptionExpanded.setVisibility(View.GONE);
                // Remove click listener if no description
                itemView.setOnClickListener(null);
            }

            // Reset expanded state when recycling views
            isExpanded = false;
            taskDescriptionExpanded.setVisibility(View.GONE);

            // Set up click listeners for action buttons
            editTaskBtn.setOnClickListener(v -> editTask(task));
            deleteTaskBtn.setOnClickListener(v -> deleteTask(task, position));
            completeTaskBtn.setOnClickListener(v -> toggleTaskCompletion(task, position));
        }

        private void updateCompletionStatus(Task task) {
            if (task.isCompleted()) {
                // Task is completed - show checkmark and style
                completionStatusIcon.setText("✅");
                completionStatusIcon.setVisibility(View.VISIBLE);
                taskTitle.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                taskTitle.setPaintFlags(taskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                completeTaskBtn.setText("Undo");
                completeTaskBtn.setIconResource(android.R.drawable.ic_menu_revert);
            } else {
                // Task is not completed - normal styling
                completionStatusIcon.setText("⏳");
                completionStatusIcon.setVisibility(View.VISIBLE);
                taskTitle.setTextColor(context.getResources().getColor(android.R.color.black));
                taskTitle.setPaintFlags(taskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                completeTaskBtn.setText("Complete");
                completeTaskBtn.setIconResource(android.R.drawable.ic_menu_save);
            }
        }

        private void toggleTaskCompletion(Task task, int position) {
            // Check user authentication
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(context, "❌ Authentication error. Please login again.", Toast.LENGTH_LONG).show();
                return;
            }

            String currentUserId = currentUser.getUid();
            boolean newCompletionStatus = !task.isCompleted();

            // Update in database with user ID verification
            boolean success = dbHelper.updateTaskCompletionStatus(task.getId(), newCompletionStatus, currentUserId);

            if (success) {
                // Update task object
                task.setCompleted(newCompletionStatus);

                // Update UI
                updateCompletionStatus(task);

                // Show feedback
                String message = newCompletionStatus ? "✅ Task completed!" : "📝 Task marked as incomplete";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                // Notify adapter to refresh the list (for proper ordering)
                notifyDataSetChanged();
            } else {
                Toast.makeText(context, "❌ Failed to update task status or unauthorized access", Toast.LENGTH_SHORT).show();
            }
        }

        private void toggleDescription() {
            if (isExpanded) {
                // Collapse
                taskDescriptionExpanded.setVisibility(View.GONE);
                taskDescription.setVisibility(View.VISIBLE);
                isExpanded = false;
            } else {
                // Expand
                taskDescription.setVisibility(View.GONE);
                taskDescriptionExpanded.setVisibility(View.VISIBLE);
                isExpanded = true;
            }
        }

        private String formatDate(long dateInMillis) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dateInMillis);

            Calendar today = Calendar.getInstance();
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);

            // Check for today, tomorrow, yesterday
            if (isSameDay(calendar, today)) {
                return "Today";
            } else if (isSameDay(calendar, tomorrow)) {
                return "Tomorrow";
            } else if (isSameDay(calendar, yesterday)) {
                return "Yesterday";
            } else {
                // Format as regular date
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                return day + "/" + month + "/" + year;
            }
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
    }
}
