package com.s22010514.mytodo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class SavedPlacePage extends AppCompatActivity implements LocationTaskAdapter.OnLocationTaskDeletedListener, LocationListener {

    private RecyclerView locationTasksRecyclerView;
    private LocationTaskAdapter locationTaskAdapter;
    private List<LocationTask> locationTaskList;
    private DatabaseHelper dbHelper;
    private TextView locationCountTxt;
    private LinearLayout emptyStateLayout;
    private MaterialButton addLocationTaskBtn;
    private LocationManager locationManager;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_place_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Intent intent = new Intent(SavedPlacePage.this, LoginPage.class);
            startActivity(intent);
            finish();
            return;
        }

        // Get current user ID for data filtering
        currentUserId = currentUser.getUid();

        // Initialize UI elements
        initializeViews();

        // Initialize services
        dbHelper = new DatabaseHelper(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Initialize the list
        locationTaskList = new ArrayList<>();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up click listeners
        setupClickListeners();

        // Load location tasks from database
        loadLocationTasksFromDatabase();
        updateUI();

        // Get current location for distance calculations
        getCurrentLocationForDistances();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the location task list when returning from MapPage
        refreshLocationTaskList();
    }

    private void initializeViews() {
        locationTasksRecyclerView = findViewById(R.id.locationTasksRecyclerView);
        locationCountTxt = findViewById(R.id.locationCountTxt);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        addLocationTaskBtn = findViewById(R.id.addLocationTaskBtn);
    }

    private void setupRecyclerView() {
        locationTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationTaskAdapter = new LocationTaskAdapter(locationTaskList, this);
        locationTaskAdapter.setOnLocationTaskDeletedListener(this);
        locationTasksRecyclerView.setAdapter(locationTaskAdapter);
    }

    private void setupClickListeners() {
        addLocationTaskBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlacePage.this, MapPage.class);
            startActivity(intent);
        });
    }

    @Override
    public void onLocationTaskDeleted() {
        updateUI();
    }

    private void refreshLocationTaskList() {
        locationTaskList.clear();
        loadLocationTasksFromDatabase();
        locationTaskAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void loadLocationTasksFromDatabase() {
        // Ensure user is authenticated before loading data
        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        Cursor cursor = dbHelper.getAllLocationTasks(currentUserId);

        if (cursor.moveToFirst()) {
            do {
                int colId = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_1);
                int colTitle = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_2);
                int colDescription = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_3);
                int colLocationAddress = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_4);
                int colLatitude = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_5);
                int colLongitude = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_6);
                int colNotificationRadius = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_7);
                int colNotificationEnabled = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_8);
                int colCreatedDate = cursor.getColumnIndex(DatabaseHelper.LOC_TASK_COL_9);

                // Check if column indices are valid
                if (colId == -1 || colTitle == -1 || colDescription == -1 ||
                    colLocationAddress == -1 || colLatitude == -1 || colLongitude == -1 ||
                    colNotificationRadius == -1 || colNotificationEnabled == -1 || colCreatedDate == -1) {
                    Toast.makeText(this, "Error: Invalid column index in location tasks", Toast.LENGTH_SHORT).show();
                    cursor.close();
                    return;
                }

                int id = cursor.getInt(colId);
                String title = cursor.getString(colTitle);
                String description = cursor.getString(colDescription);
                String locationAddress = cursor.getString(colLocationAddress);
                double latitude = cursor.getDouble(colLatitude);
                double longitude = cursor.getDouble(colLongitude);
                int notificationRadius = cursor.getInt(colNotificationRadius);
                boolean notificationEnabled = cursor.getInt(colNotificationEnabled) == 1;
                long createdDate = cursor.getLong(colCreatedDate);

                LocationTask locationTask = new LocationTask(id, title, description, locationAddress,
                        latitude, longitude, notificationRadius, notificationEnabled, createdDate, currentUserId);
                locationTaskList.add(locationTask);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(SavedPlacePage.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    private void updateUI() {
        int locationTaskCount = locationTaskList.size();

        if (locationTaskCount == 0) {
            locationCountTxt.setText("No location tasks");
            emptyStateLayout.setVisibility(View.VISIBLE);
            locationTasksRecyclerView.setVisibility(View.GONE);
        } else {
            locationCountTxt.setText(locationTaskCount + (locationTaskCount == 1 ? " location task" : " location tasks"));
            emptyStateLayout.setVisibility(View.GONE);
            locationTasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void getCurrentLocationForDistances() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (lastKnownLocation != null) {
                    locationTaskAdapter.setCurrentUserLocation(lastKnownLocation);
                }

                // Request location updates for real-time distance calculation
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    60000, 100, this); // Update every minute, 100m minimum distance

            } catch (SecurityException e) {
                // Handle silently - distance calculation will show "--km away"
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Update adapter with new location for distance calculations
        locationTaskAdapter.setCurrentUserLocation(location);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // Unused LocationListener methods
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
