package com.s22010514.mytodo;

public class LocationTask {
    private int id;
    private String title;
    private String description;
    private String locationAddress;
    private double latitude;
    private double longitude;
    private int notificationRadius; // in meters
    private boolean notificationEnabled;
    private long createdDate;
    private String userId; // Add userId field

    public LocationTask(int id, String title, String description, String locationAddress,
                       double latitude, double longitude, int notificationRadius,
                       boolean notificationEnabled, long createdDate, String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notificationRadius = notificationRadius;
        this.notificationEnabled = notificationEnabled;
        this.createdDate = createdDate;
        this.userId = userId; // Initialize userId
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocationAddress() { return locationAddress; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getNotificationRadius() { return notificationRadius; }
    public boolean isNotificationEnabled() { return notificationEnabled; }
    public long getCreatedDate() { return createdDate; }
    public String getUserId() { return userId; } // Add getter for userId

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setNotificationRadius(int notificationRadius) { this.notificationRadius = notificationRadius; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }
    public void setUserId(String userId) { this.userId = userId; } // Add setter for userId
}
