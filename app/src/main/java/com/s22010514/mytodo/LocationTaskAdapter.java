package com.s22010514.mytodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
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

import java.util.List;
import java.util.Locale;

public class LocationTaskAdapter extends RecyclerView.Adapter<LocationTaskAdapter.LocationTaskViewHolder> {

    private List<LocationTask> locationTaskList;
    private Context context;
    private DatabaseHelper dbHelper;
    private OnLocationTaskDeletedListener onLocationTaskDeletedListener;
    private Location currentUserLocation;
    private FirebaseAuth mAuth;

    public interface OnLocationTaskDeletedListener {
        void onLocationTaskDeleted();
    }

    public LocationTaskAdapter(List<LocationTask> locationTaskList, Context context) {
        this.locationTaskList = locationTaskList;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void setOnLocationTaskDeletedListener(OnLocationTaskDeletedListener listener) {
        this.onLocationTaskDeletedListener = listener;
    }

    public void setCurrentUserLocation(Location location) {
        this.currentUserLocation = location;
        notifyDataSetChanged(); // Update distance calculations
    }

    @NonNull
    @Override
    public LocationTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_task_item, parent, false);
        return new LocationTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationTaskViewHolder holder, int position) {
        LocationTask locationTask = locationTaskList.get(position);
        holder.bind(locationTask, position);
    }

    @Override
    public int getItemCount() {
        return locationTaskList.size();
    }

    private void deleteLocationTask(LocationTask locationTask, int position) {
        // Check user authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "❌ Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        new AlertDialog.Builder(context)
                .setTitle("🗑️ Delete Location Task")
                .setMessage("Are you sure you want to delete this location task?\n\n\"" + locationTask.getTitle() + "\"\n\nThis action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    // Use secure delete method with user ID verification
                    boolean isDeleted = dbHelper.deleteLocationTaskById(locationTask.getId(), currentUserId);
                    if (isDeleted) {
                        locationTaskList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, locationTaskList.size());

                        if (onLocationTaskDeletedListener != null) {
                            onLocationTaskDeletedListener.onLocationTaskDeleted();
                        }

                        Toast.makeText(context, "✅ Location task deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "❌ Failed to delete location task or unauthorized access", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void editLocationTask(LocationTask locationTask) {
        Intent intent = new Intent(context, MapPage.class);
        intent.putExtra("locationTaskId", locationTask.getId());
        intent.putExtra("locationTaskTitle", locationTask.getTitle());
        intent.putExtra("locationTaskDescription", locationTask.getDescription());
        intent.putExtra("locationAddress", locationTask.getLocationAddress());
        intent.putExtra("latitude", locationTask.getLatitude());
        intent.putExtra("longitude", locationTask.getLongitude());
        intent.putExtra("notificationRadius", locationTask.getNotificationRadius());
        intent.putExtra("notificationEnabled", locationTask.isNotificationEnabled());
        context.startActivity(intent);
    }

    private void navigateToLocation(LocationTask locationTask) {
        String uri = String.format(Locale.ENGLISH, "google.navigation:q=%.6f,%.6f",
                locationTask.getLatitude(), locationTask.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to general maps intent
            String fallbackUri = String.format(Locale.ENGLISH, "geo:0,0?q=%.6f,%.6f",
                    locationTask.getLatitude(), locationTask.getLongitude());
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri));
            context.startActivity(fallbackIntent);
        }
    }

    private String calculateDistance(LocationTask locationTask) {
        if (currentUserLocation == null) {
            return "--km away";
        }

        float[] results = new float[1];
        Location.distanceBetween(
                currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                locationTask.getLatitude(), locationTask.getLongitude(),
                results
        );

        float distanceInMeters = results[0];
        if (distanceInMeters < 1000) {
            return String.format("%.0fm away", distanceInMeters);
        } else {
            return String.format("%.1fkm away", distanceInMeters / 1000);
        }
    }

    class LocationTaskViewHolder extends RecyclerView.ViewHolder {

        private TextView locationTaskTitle;
        private TextView locationAddress;
        private MaterialButton notificationToggleBtn;
        private TextView distanceText;
        private TextView locationTaskDescription;
        private MaterialButton navigateBtn;
        private MaterialButton editLocationTaskBtn;
        private MaterialButton deleteLocationTaskBtn;
        private boolean isExpanded = false;

        public LocationTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTaskTitle = itemView.findViewById(R.id.locationTaskTitle);
            locationAddress = itemView.findViewById(R.id.locationAddress);
            notificationToggleBtn = itemView.findViewById(R.id.notificationToggleBtn);
            distanceText = itemView.findViewById(R.id.distanceText);
            locationTaskDescription = itemView.findViewById(R.id.locationTaskDescription);
            navigateBtn = itemView.findViewById(R.id.navigateBtn);
            editLocationTaskBtn = itemView.findViewById(R.id.editLocationTaskBtn);
            deleteLocationTaskBtn = itemView.findViewById(R.id.deleteLocationTaskBtn);
        }

        public void bind(LocationTask locationTask, int position) {
            locationTaskTitle.setText(locationTask.getTitle());
            locationAddress.setText(locationTask.getLocationAddress());
            distanceText.setText("📏 " + calculateDistance(locationTask));

            // Handle description
            String description = locationTask.getDescription();
            if (description != null && !description.trim().isEmpty()) {
                locationTaskDescription.setText(description);
                locationTaskDescription.setVisibility(View.VISIBLE);

                // Set up click listener for expanding/collapsing description
                itemView.setOnClickListener(v -> toggleDescription());
            } else {
                locationTaskDescription.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
            }

            // Handle notification toggle button
            updateNotificationToggleButton(locationTask);

            // Reset expanded state when recycling views
            isExpanded = false;
            locationTaskDescription.setVisibility(View.GONE);

            // Set up click listeners for action buttons
            navigateBtn.setOnClickListener(v -> navigateToLocation(locationTask));
            editLocationTaskBtn.setOnClickListener(v -> editLocationTask(locationTask));
            deleteLocationTaskBtn.setOnClickListener(v -> deleteLocationTask(locationTask, position));

            // Set up notification toggle click listener
            notificationToggleBtn.setOnClickListener(v -> toggleNotification(locationTask, position));
        }

        private void updateNotificationToggleButton(LocationTask locationTask) {
            if (locationTask.isNotificationEnabled()) {
                notificationToggleBtn.setText("🔔 " + locationTask.getNotificationRadius() + "m");
                notificationToggleBtn.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white));
                notificationToggleBtn.setBackgroundTintList(
                        androidx.core.content.ContextCompat.getColorStateList(context, android.R.color.holo_green_light));
            } else {
                notificationToggleBtn.setText("🔕 OFF");
                notificationToggleBtn.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white));
                notificationToggleBtn.setBackgroundTintList(
                        androidx.core.content.ContextCompat.getColorStateList(context, android.R.color.darker_gray));
            }
        }

        private void toggleNotification(LocationTask locationTask, int position) {
            // Check user authentication
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(context, "❌ Authentication error. Please login again.", Toast.LENGTH_LONG).show();
                return;
            }

            String currentUserId = currentUser.getUid();
            boolean newNotificationState = !locationTask.isNotificationEnabled();

            // Update in database with user ID verification
            boolean updated = dbHelper.updateLocationTaskById(
                    locationTask.getId(),
                    locationTask.getTitle(),
                    locationTask.getDescription(),
                    locationTask.getLocationAddress(),
                    locationTask.getLatitude(),
                    locationTask.getLongitude(),
                    locationTask.getNotificationRadius(),
                    newNotificationState,
                    currentUserId
            );

            if (updated) {
                // Update the model
                locationTask.setNotificationEnabled(newNotificationState);

                // Update UI
                updateNotificationToggleButton(locationTask);

                // Show feedback
                if (newNotificationState) {
                    Toast.makeText(context, "🔔 Notifications enabled for " + locationTask.getTitle(),
                            Toast.LENGTH_SHORT).show();
                    // Start location monitoring for this task
                    startLocationMonitoring(locationTask);
                } else {
                    Toast.makeText(context, "🔕 Notifications disabled for " + locationTask.getTitle(),
                            Toast.LENGTH_SHORT).show();
                    // Stop location monitoring for this task
                    stopLocationMonitoring(locationTask);
                }
            } else {
                Toast.makeText(context, "❌ Failed to update notification settings or unauthorized access",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void startLocationMonitoring(LocationTask locationTask) {
            // Start the location notification service
            Intent serviceIntent = new Intent(context, LocationNotificationService.class);
            serviceIntent.putExtra("locationTaskId", locationTask.getId());
            serviceIntent.putExtra("locationTaskTitle", locationTask.getTitle());
            serviceIntent.putExtra("latitude", locationTask.getLatitude());
            serviceIntent.putExtra("longitude", locationTask.getLongitude());
            serviceIntent.putExtra("radius", locationTask.getNotificationRadius());
            serviceIntent.putExtra("taskUserId", locationTask.getUserId()); // Add user ID
            context.startService(serviceIntent);
        }

        private void stopLocationMonitoring(LocationTask locationTask) {
            // Stop the location notification service for this task
            Intent serviceIntent = new Intent(context, LocationNotificationService.class);
            serviceIntent.putExtra("stopTaskId", locationTask.getId());
            context.startService(serviceIntent);
        }

        private void toggleDescription() {
            if (isExpanded) {
                locationTaskDescription.setVisibility(View.GONE);
                isExpanded = false;
            } else {
                locationTaskDescription.setVisibility(View.VISIBLE);
                isExpanded = true;
            }
        }
    }
}
