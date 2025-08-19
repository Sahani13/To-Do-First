package com.s22010514.mytodo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPage extends AppCompatActivity implements LocationListener {

    // UI elements
    private TextInputEditText locationTaskTitleInput;
    private TextInputEditText locationAddressInput;
    private TextInputEditText locationTaskDescInput;
    private MaterialButton useCurrentLocationBtn;
    private MaterialButton saveLocationTaskBtn;
    private MaterialButton cancelLocationTaskBtn;
    private MaterialButton openInMapsBtn;
    private MaterialButton shareLocationBtn;
    private SwitchCompat notificationSwitch;
    private SeekBar radiusSeekBar;
    private TextView radiusText;
    private TextView selectedLocationText;
    private TextView gpsStatusIcon;

    // Services and helpers
    private DatabaseHelper dbHelper;
    private LocationManager locationManager;
    private Geocoder geocoder;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // Location data
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int GPS_ENABLE_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Intent intent = new Intent(MapPage.this, LoginPage.class);
            startActivity(intent);
            finish();
            return;
        }

        // Get current user ID for data operations
        currentUserId = currentUser.getUid();

        // Initialize UI elements
        initializeViews();

        // Initialize services
        dbHelper = new DatabaseHelper(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        geocoder = new Geocoder(this, Locale.getDefault());

        // Set up click listeners
        setupClickListeners();

        // Set up seekbar for notification radius
        radiusSeekBar.setMax(500); // Set max to 500m
        radiusSeekBar.setProgress(100); // Set initial value to 100m
        setupRadiusSeekBar();

        // Check if editing an existing location task
        checkForEditIntent();

        // Request location permissions
        requestLocationPermissions();
    }

    private void initializeViews() {
        locationTaskTitleInput = findViewById(R.id.locationTaskTitleInput);
        locationAddressInput = findViewById(R.id.locationAddressInput);
        locationTaskDescInput = findViewById(R.id.locationTaskDescInput);
        useCurrentLocationBtn = findViewById(R.id.useCurrentLocationBtn);
        saveLocationTaskBtn = findViewById(R.id.saveLocationTaskBtn);
        cancelLocationTaskBtn = findViewById(R.id.cancelLocationTaskBtn);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        radiusSeekBar = findViewById(R.id.radiusSeekBar);
        radiusText = findViewById(R.id.radiusText);
        selectedLocationText = findViewById(R.id.selectedLocationText);
        gpsStatusIcon = findViewById(R.id.gpsStatusIcon);
        openInMapsBtn = findViewById(R.id.openInMapsBtn);
        shareLocationBtn = findViewById(R.id.shareLocationBtn);
    }

    private void setupClickListeners() {
        useCurrentLocationBtn.setOnClickListener(v -> getCurrentLocation());
        saveLocationTaskBtn.setOnClickListener(v -> saveLocationTask());
        cancelLocationTaskBtn.setOnClickListener(v -> finish());
        openInMapsBtn.setOnClickListener(v -> openInGoogleMaps());
        shareLocationBtn.setOnClickListener(v -> shareLocation());

        // Address input listener for geocoding
        locationAddressInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String address = locationAddressInput.getText().toString().trim();
                if (!address.isEmpty()) {
                    geocodeAddress(address);
                }
            }
        });
    }

    private void setupRadiusSeekBar() {
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int radius = Math.max(1, progress); // Minimum 1m
                radiusText.setText(radius + "m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void saveLocationTask() {
        // Ensure user is authenticated before saving data
        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        String title = locationTaskTitleInput.getText().toString().trim();
        String description = locationTaskDescInput.getText().toString().trim();
        String address = locationAddressInput.getText().toString().trim();

        if (title.isEmpty()) {
            locationTaskTitleInput.setError("Title is required");
            return;
        }

        if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean notificationEnabled = notificationSwitch.isChecked();
        int notificationRadius = Math.max(1, radiusSeekBar.getProgress());

        // Check if this is an edit operation or a new task
        Intent intent = getIntent();
        boolean isEditOperation = intent.hasExtra("locationTaskId");
        boolean success;

        if (isEditOperation) {
            // Update existing task
            int locationTaskId = intent.getIntExtra("locationTaskId", -1);
            success = dbHelper.updateLocationTaskById(locationTaskId, title, description, address,
                selectedLatitude, selectedLongitude, notificationRadius, notificationEnabled, currentUserId);

            if (success) {
                // Stop monitoring the old task first
                Intent stopServiceIntent = new Intent(this, LocationNotificationService.class);
                stopServiceIntent.putExtra("stopTaskId", locationTaskId);
                startService(stopServiceIntent);

                // Start location monitoring service if notifications are enabled
                if (notificationEnabled) {
                    startLocationMonitoringService(title, selectedLatitude, selectedLongitude, notificationRadius, locationTaskId);
                }

                Toast.makeText(this, "✅ Location task updated!" +
                    (notificationEnabled ? "\n📍 Radius: " + notificationRadius + "m\n🔔 Location monitoring updated!" : "\n🔕 Notifications disabled"),
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "❌ Failed to update location task", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Create new task
            success = dbHelper.insertLocationTask(title, description, address,
                selectedLatitude, selectedLongitude, notificationRadius, notificationEnabled, currentUserId);

            if (success) {
                // Start location monitoring service if notifications are enabled
                if (notificationEnabled) {
                    startLocationMonitoringService(title, selectedLatitude, selectedLongitude, notificationRadius, -1);
                }

                Toast.makeText(this, "✅ Location task saved!" +
                    (notificationEnabled ? "\n📍 Radius: " + notificationRadius + "m\n🔔 Location monitoring started!" : "\n🔕 Notifications disabled"),
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "❌ Failed to save location task", Toast.LENGTH_SHORT).show();
            }
        }

        if (success) {
            finish();
        }
    }

    private void getCurrentLocation() {
        android.util.Log.d("MapPage", "getCurrentLocation() called");

        // Check location permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d("MapPage", "Location permission not granted, requesting permissions");
            Toast.makeText(this, "📍 Location permission required", Toast.LENGTH_SHORT).show();
            requestLocationPermissions();
            return;
        }

        // Check if GPS is enabled
        if (!isGpsEnabled()) {
            android.util.Log.d("MapPage", "GPS not enabled, showing enable dialog");
            Toast.makeText(this, "📍 Please enable GPS", Toast.LENGTH_SHORT).show();
            showGpsEnableDialog();
            return;
        }

        try {
            android.util.Log.d("MapPage", "Requesting location updates from GPS provider");
            Toast.makeText(this, "📍 Getting current location...", Toast.LENGTH_SHORT).show();

            // Request location updates with timeout
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            // Also try network provider as backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                android.util.Log.d("MapPage", "Also requesting from network provider");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }

            // Set a timeout to stop location requests after 30 seconds
            new android.os.Handler().postDelayed(() -> {
                locationManager.removeUpdates(this);
                if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
                    android.util.Log.d("MapPage", "Location request timed out");
                    Toast.makeText(this, "❌ Location request timed out. Please try again or enter address manually.", Toast.LENGTH_LONG).show();
                }
            }, 30000);

        } catch (SecurityException e) {
            android.util.Log.e("MapPage", "Security exception: " + e.getMessage());
            Toast.makeText(this, "❌ Location permission error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("MapPage", "Error getting location: " + e.getMessage());
            Toast.makeText(this, "❌ Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void geocodeAddress(String address) {
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                selectedLatitude = location.getLatitude();
                selectedLongitude = location.getLongitude();
                updateLocationDisplay();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to geocode address", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationDisplay() {
        String locationInfo = String.format("📍 Lat: %.6f\n📍 Lng: %.6f", selectedLatitude, selectedLongitude);
        selectedLocationText.setText(locationInfo);
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void showGpsEnableDialog() {
        new AlertDialog.Builder(this)
            .setTitle("GPS Required")
            .setMessage("GPS is not enabled. Would you like to enable it?")
            .setPositiveButton("Yes", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent, GPS_ENABLE_REQUEST_CODE);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void openInGoogleMaps() {
        if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            Uri gmmIntentUri = Uri.parse("geo:" + selectedLatitude + "," + selectedLongitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareLocation() {
        if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            String shareText = "Check out this location: https://maps.google.com/?q=" +
                selectedLatitude + "," + selectedLongitude;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share location"));
        } else {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForEditIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("locationTaskId")) {
            // This is an edit operation, load the data
            String title = intent.getStringExtra("locationTaskTitle");
            String description = intent.getStringExtra("locationTaskDescription");
            String address = intent.getStringExtra("locationAddress");
            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);
            int notificationRadius = intent.getIntExtra("notificationRadius", 100);
            boolean notificationEnabled = intent.getBooleanExtra("notificationEnabled", true);

            // Populate the form with existing data
            if (title != null) locationTaskTitleInput.setText(title);
            if (description != null) locationTaskDescInput.setText(description);
            if (address != null) locationAddressInput.setText(address);

            // Set location coordinates
            selectedLatitude = latitude;
            selectedLongitude = longitude;

            // Set notification settings
            notificationSwitch.setChecked(notificationEnabled);
            radiusSeekBar.setProgress(notificationRadius);
            radiusText.setText(notificationRadius + "m");

            updateLocationDisplay();
        }
    }

    private void startLocationMonitoringService(String title, double latitude, double longitude, int radius, int taskId) {
        try {
            // Check if user is authenticated
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "❌ Authentication error. Please login again.", Toast.LENGTH_LONG).show();
                return;
            }

            android.util.Log.d("MapPage", "Starting location monitoring service for: " + title);

            // Create intent to start the LocationNotificationService
            Intent serviceIntent = new Intent(this, LocationNotificationService.class);
            serviceIntent.putExtra("locationTaskId", taskId != -1 ? taskId : 1); // Use actual task ID if available
            serviceIntent.putExtra("locationTaskTitle", title);
            serviceIntent.putExtra("latitude", latitude);
            serviceIntent.putExtra("longitude", longitude);
            serviceIntent.putExtra("radius", radius);
            serviceIntent.putExtra("taskUserId", currentUser.getUid()); // Add user ID

            // Start the foreground service
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            android.util.Log.d("MapPage", "Location monitoring service started successfully");
            Toast.makeText(this, "🔔 Location monitoring started!\n📍 You'll be notified with sound when near this location", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            android.util.Log.e("MapPage", "Error starting location monitoring service: " + e.getMessage());
            Toast.makeText(this, "❌ Failed to start location monitoring: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MapPage.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    // LocationListener implementation
    @Override
    public void onLocationChanged(Location location) {
        android.util.Log.d("MapPage", "Location received: " + location.getLatitude() + ", " + location.getLongitude());

        selectedLatitude = location.getLatitude();
        selectedLongitude = location.getLongitude();
        updateLocationDisplay();
        locationManager.removeUpdates(this);

        // Perform reverse geocoding to get address from coordinates
        reverseGeocodeLocation(selectedLatitude, selectedLongitude);

        Toast.makeText(this, "✅ Current location obtained!", Toast.LENGTH_SHORT).show();
    }

    private void reverseGeocodeLocation(double latitude, double longitude) {
        try {
            android.util.Log.d("MapPage", "Starting reverse geocoding for: " + latitude + ", " + longitude);

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Build a readable address string
                StringBuilder addressBuilder = new StringBuilder();

                // Add street number and name
                if (address.getSubThoroughfare() != null) {
                    addressBuilder.append(address.getSubThoroughfare()).append(" ");
                }
                if (address.getThoroughfare() != null) {
                    addressBuilder.append(address.getThoroughfare()).append(", ");
                }

                // Add locality (city/town)
                if (address.getLocality() != null) {
                    addressBuilder.append(address.getLocality()).append(", ");
                }

                // Add admin area (state/province)
                if (address.getAdminArea() != null) {
                    addressBuilder.append(address.getAdminArea()).append(", ");
                }

                // Add country
                if (address.getCountryName() != null) {
                    addressBuilder.append(address.getCountryName());
                }

                String fullAddress = addressBuilder.toString();
                // Remove trailing comma and space if present
                if (fullAddress.endsWith(", ")) {
                    fullAddress = fullAddress.substring(0, fullAddress.length() - 2);
                }

                android.util.Log.d("MapPage", "Reverse geocoding successful: " + fullAddress);

                // Set the address in the input field
                locationAddressInput.setText(fullAddress);

                Toast.makeText(this, "📍 Address: " + fullAddress, Toast.LENGTH_LONG).show();

            } else {
                android.util.Log.d("MapPage", "No address found for coordinates");
                // If no address found, show coordinates as fallback
                String coordsAddress = "Lat: " + String.format("%.6f", latitude) + ", Lng: " + String.format("%.6f", longitude);
                locationAddressInput.setText(coordsAddress);
                Toast.makeText(this, "📍 Location found (coordinates only)", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            android.util.Log.e("MapPage", "Reverse geocoding failed: " + e.getMessage());
            // Fallback to coordinates if geocoding fails
            String coordsAddress = "Lat: " + String.format("%.6f", latitude) + ", Lng: " + String.format("%.6f", longitude);
            locationAddressInput.setText(coordsAddress);
            Toast.makeText(this, "📍 Location found (address lookup failed)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("MapPage", "Error in reverse geocoding: " + e.getMessage());
            Toast.makeText(this, "❌ Error getting address", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
