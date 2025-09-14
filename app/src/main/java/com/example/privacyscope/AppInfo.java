package com.example.privacyscope;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppInfo implements Serializable {

    // --- Data Fields ---
    private final String appName;
    private final String packageName;
    private final transient Drawable icon;
    private final int riskScore;
    private final RiskLevel riskLevel;
    private final List<String> dangerousPermissions;
    private final List<String> detectedTrackers;

    // --- Static Risk Definitions ---
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public static final String[] DANGEROUS_PERMISSIONS = {
            "android.permission.READ_SMS", "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE",
            "android.permission.READ_CONTACTS"
    };

    // A list of known tracker signatures found in package/class names
    private static final String[][] TRACKER_SIGNATURES = {
            {"com.google.android.gms.analytics", "Google Analytics"},
            {"com.google.firebase.analytics", "Firebase Analytics"},
            {"com.facebook", "Facebook SDK"},
            {"com.appsflyer", "AppsFlyer"},
            {"com.adjust.sdk", "Adjust"},
            {"com.applovin", "AppLovin"},
            {"com.flurry.android", "Flurry Analytics"},
            {"com.unity3d.ads", "Unity Ads"},
            {"com.inmobi", "InMobi"},
            {"io.presage", "Ogury"}
    };

    // --- Constructor ---
    public AppInfo(PackageInfo packageInfo, PackageManager pm) {
        this.appName = packageInfo.applicationInfo.loadLabel(pm).toString();
        this.packageName = packageInfo.packageName;
        this.icon = packageInfo.applicationInfo.loadIcon(pm);
        this.dangerousPermissions = findDangerousPermissions(packageInfo);
        this.detectedTrackers = detectTrackers(packageInfo);
        this.riskScore = calculateRiskScore();
        this.riskLevel = calculateRiskLevel();
    }


    // --- Logic Methods ---

    private List<String> findDangerousPermissions(PackageInfo packageInfo) {
        List<String> foundPermissions = new ArrayList<>();
        if (packageInfo.requestedPermissions != null) {
            for (String permission : packageInfo.requestedPermissions) {
                for (String dangerous : DANGEROUS_PERMISSIONS) {
                    if (dangerous.equals(permission)) {
                        foundPermissions.add(permission);
                        break;
                    }
                }
            }
        }
        return foundPermissions;
    }

    /**
     * A more robust method for detecting trackers.
     * It scans the app's declared broadcast receivers for known tracker signatures.
     */
    private List<String> detectTrackers(PackageInfo packageInfo) {
        // Use a Set to avoid duplicate tracker names
        Set<String> trackers = new HashSet<>();

        if (packageInfo.receivers != null) {
            for (ActivityInfo receiver : packageInfo.receivers) {
                for (String[] signature : TRACKER_SIGNATURES) {
                    // Check if the receiver's class name contains a known tracker signature
                    if (receiver.name.contains(signature[0])) {
                        trackers.add(signature[1]); // Add the friendly name of the tracker
                    }
                }
            }
        }

        // Fallback check on package name for very common trackers
        if (packageName.contains("facebook")) trackers.add("Facebook SDK");
        if (packageName.contains("tiktok")) trackers.add("TikTok Analytics");

        return new ArrayList<>(trackers);
    }

    private int calculateRiskScore() {
        int score = this.dangerousPermissions.size() * 20; // 20 points per dangerous permission
        score += this.detectedTrackers.size() * 15; // 15 points per tracker
        return Math.min(score, 100); // Cap the score at 100
    }

    private RiskLevel calculateRiskLevel() {
        if (riskScore >= 60) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 30) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }


    // --- Public Getters ---
    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public Drawable getIcon() { return icon; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public List<String> getDangerousPermissions() { return dangerousPermissions; }
    public List<String> getDetectedTrackers() { return detectedTrackers; }
}

