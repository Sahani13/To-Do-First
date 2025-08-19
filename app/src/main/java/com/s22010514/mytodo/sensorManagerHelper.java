package com.s22010514.mytodo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class sensorManagerHelper implements SensorEventListener {
    // Singleton instance of the class
    private static sensorManagerHelper instance;
    // Sensor manager to access sensors
    private SensorManager sensorManager;
    // Light sensor to detect ambient light
    private Sensor lightSensor;
    // Reference to the activity
    private Activity activity;

    // Private constructor to prevent direct instantiation
    private sensorManagerHelper(Activity activity) {
        this.activity = activity;
        // Initialize sensor manager and light sensor
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    // Method to get the singleton instance of the class
    public static sensorManagerHelper getInstance(Activity activity) {
        if (instance == null) {
            instance = new sensorManagerHelper(activity);
        }
        return instance;
    }

    // Method to register the sensor listener
    public void registerListener() {
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("registerSensor", "Listener registered for light sensor."); // Log registration
    }

    // Method to unregister the sensor listener
    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    // Method called when sensor values change
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lightLevel = event.values[0];
            Log.d("LightLevel", "Light Level : " + lightLevel);
            if (lightLevel < 20) {
                // Apply dark theme if light level is low
                ThemeManager.applyDarkTheme(activity);
                Log.d("applied", "Theme : dark");
            } else {
                // Apply light theme if light level is high
                ThemeManager.applyLightTheme(activity);
                Log.d("applied", "Theme : Light");
            }
        }
    }

    // Method called when sensor accuracy changes (not used)
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No implementation needed for this example
    }
}
