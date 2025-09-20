package com.example.privacyscope;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

/**
 * The definitive data model for online alternative apps.
 * It now includes the URL for the app's real icon.
 */
public class AlternativeApp {
    private final String appName;
    private final String packageName;
    private final String iconUrl; // <<< NEW

    public AlternativeApp(String appName, String packageName, String iconUrl) {
        this.appName = appName;
        this.packageName = packageName;
        this.iconUrl = iconUrl;
    }

    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public String getIconUrl() { return iconUrl; } // <<< NEW

    /**
     * This method is no longer used but kept for stability.
     */
    public Drawable getIcon(Context context) {
        return ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round);
    }
}

