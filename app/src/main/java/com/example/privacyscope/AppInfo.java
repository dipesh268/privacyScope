package com.example.privacyscope;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppInfo {
    private final String appName;
    private final String packageName;
    private final Drawable icon;
    private final int riskScore;
    private final RiskLevel riskLevel;
    private final List<String> dangerousPermissions;
    private final List<String> detectedTrackers;
    private final PackageManager pm;
    private long lastTimeUsed;


    public AppInfo(PackageInfo packageInfo, PackageManager pm) {
        this.pm = pm;
        this.appName = packageInfo.applicationInfo.loadLabel(pm).toString();
        this.packageName = packageInfo.packageName;
        this.icon = packageInfo.applicationInfo.loadIcon(pm);
        this.dangerousPermissions = getDangerousPermissions(packageInfo);
        this.riskScore = calculateRiskScore(dangerousPermissions);
        this.riskLevel = calculateRiskLevel(riskScore);
        this.detectedTrackers = detectTrackers(packageInfo);
        this.lastTimeUsed = 0;
    }

    // --- Getters & Setters ---
    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public Drawable getIcon() { return icon; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public List<String> getDangerousPermissions() { return dangerousPermissions; }
    public List<String> getDetectedTrackers() { return detectedTrackers; }
    public long getLastTimeUsed() { return lastTimeUsed; }
    public void setLastTimeUsed(long lastTimeUsed) { this.lastTimeUsed = lastTimeUsed; }


    private int calculateRiskScore(List<String> permissions) {
        return Math.min(permissions.size() * 20, 100);
    }

    private RiskLevel calculateRiskLevel(int score) {
        if (score >= 60) return RiskLevel.HIGH;
        if (score >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private List<String> getDangerousPermissions(PackageInfo packageInfo) {
        List<String> permissions = new ArrayList<>();
        if (packageInfo.requestedPermissions != null) {
            for (String permission : packageInfo.requestedPermissions) {
                try {
                    PermissionInfo pInfo = pm.getPermissionInfo(permission, 0);
                    if (pInfo.getProtection() == PermissionInfo.PROTECTION_DANGEROUS) {
                        permissions.add(permission);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Ignore if a permission is not found
                }
            }
        }
        return permissions;
    }

    private List<String> detectTrackers(PackageInfo packageInfo) {
        List<String> trackers = new ArrayList<>();
        final String[] TRACKER_SIGNATURES = {
                "com.google.firebase.analytics", "com.google.android.gms.ads",
                "com.facebook.sdk", "io.branch.referral",
                "com.appsflyer", "com.adjust.sdk"
        };
        final String[] TRACKER_NAMES = {
                "Google Analytics", "Google Ads", "Facebook SDK",
                "Branch", "AppsFlyer", "Adjust"
        };

        if (packageInfo.receivers != null) {
            for (android.content.pm.ActivityInfo receiver : packageInfo.receivers) {
                if (receiver.name != null) {
                    for (int i = 0; i < TRACKER_SIGNATURES.length; i++) {
                        if (receiver.name.contains(TRACKER_SIGNATURES[i]) && !trackers.contains(TRACKER_NAMES[i])) {
                            trackers.add(TRACKER_NAMES[i]);
                        }
                    }
                }
            }
        }
        Collections.sort(trackers);
        return trackers;
    }


    public enum RiskLevel { HIGH, MEDIUM, LOW }
}

