package com.s22010514.mytodo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class LocationNotificationService extends Service implements LocationListener {

    private static final String CHANNEL_ID = "LocationTaskChannel";
    private static final int NOTIFICATION_ID = 1001;

    private LocationManager locationManager;
    private Map<Integer, LocationTask> monitoredTasks = new HashMap<>();
    private Map<Integer, Boolean> taskTriggeredMap = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No user logged in, stop the service
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            if (intent.hasExtra("stopTaskId")) {
                // Stop monitoring specific task
                int taskId = intent.getIntExtra("stopTaskId", -1);
                stopMonitoringTask(taskId);
            } else if (intent.hasExtra("stopAllTasks")) {
                // Stop all monitoring when user logs out
                stopAllTasks();
            } else if (intent.hasExtra("locationTaskId")) {
                // Start monitoring new task - but only if it belongs to current user
                String taskUserId = intent.getStringExtra("taskUserId");
                if (taskUserId != null && taskUserId.equals(currentUser.getUid())) {
                    LocationTask task = new LocationTask(
                        intent.getIntExtra("locationTaskId", -1),
                        intent.getStringExtra("locationTaskTitle"),
                        "",
                        "",
                        intent.getDoubleExtra("latitude", 0.0),
                        intent.getDoubleExtra("longitude", 0.0),
                        intent.getIntExtra("radius", 100),
                        true,
                        System.currentTimeMillis(),
                        taskUserId  // Add userId as the 10th parameter
                    );
                    startMonitoringTask(task);
                }
            }
        }
        return START_STICKY;
    }

    private void startMonitoringTask(LocationTask task) {
        // Double-check user authentication before starting monitoring
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || !currentUser.getUid().equals(task.getUserId())) {
            return;
        }

        monitoredTasks.put(task.getId(), task);
        taskTriggeredMap.put(task.getId(), false);
        startLocationUpdates();
    }

    private void stopMonitoringTask(int taskId) {
        monitoredTasks.remove(taskId);
        taskTriggeredMap.remove(taskId);

        if (monitoredTasks.isEmpty()) {
            stopLocationUpdates();
            stopSelf();
        }
    }

    private void stopAllTasks() {
        monitoredTasks.clear();
        taskTriggeredMap.clear();
        stopLocationUpdates();
        stopSelf();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    this
                );
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    10000,
                    10,
                    this
                );
            } catch (SecurityException e) {
                // Handle permission error
            }
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Check if user is still logged in before processing location updates
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            stopAllTasks();
            return;
        }
        checkProximityToTasks(location);
    }

    private void checkProximityToTasks(Location currentLocation) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        for (Map.Entry<Integer, LocationTask> entry : monitoredTasks.entrySet()) {
            int taskId = entry.getKey();
            LocationTask task = entry.getValue();

            // Verify task belongs to current user
            if (task.getUserId() == null || !currentUser.getUid().equals(task.getUserId())) {
                continue;
            }

            float[] results = new float[1];
            Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                task.getLatitude(), task.getLongitude(),
                results
            );

            float distanceInMeters = results[0];
            boolean wasTriggered = taskTriggeredMap.get(taskId);

            if (distanceInMeters <= task.getNotificationRadius()) {
                if (!wasTriggered) {
                    triggerLocationNotification(task, distanceInMeters);
                    taskTriggeredMap.put(taskId, true);
                }
            } else {
                if (wasTriggered && distanceInMeters > task.getNotificationRadius() + 50) {
                    taskTriggeredMap.put(taskId, false);
                }
            }
        }
    }

    private void triggerLocationNotification(LocationTask task, float distance) {
        // Final check - only show notification if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || task.getUserId() == null || !currentUser.getUid().equals(task.getUserId())) {
            return;
        }

        String title = "📍 Location Reminder";
        String message = "You're near: " + task.getTitle() + " (" + Math.round(distance) + "m away)";

        Intent intent = new Intent(this, MapPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID + task.getId(), builder.build());

        // Play sound and vibrate
        playNotificationSound();
        if (vibrator != null) {
            vibrator.vibrate(1000);
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = MediaPlayer.create(getApplicationContext(), notification);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Location Task Notifications";
            String description = "Notifications for location-based tasks";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MapPage.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tasks Active")
                .setContentText("Monitoring your location tasks")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
