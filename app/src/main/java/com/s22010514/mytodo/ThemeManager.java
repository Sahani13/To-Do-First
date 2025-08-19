package com.s22010514.mytodo;

import android.app.Activity;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    // Method to apply dark theme
    public static void applyDarkTheme(Activity activity) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        Log.d("dark theme", "Theme : Dark"); // Log the theme change
    }

    // Method to apply light theme
    public static void applyLightTheme(Activity activity) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Log.d("light theme", "Theme : Light"); // Log the theme change
    }
}
