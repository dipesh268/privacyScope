package com.example.privacyscope;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, Long> permissionUsage;


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
        this.permissionUsage = new HashMap<>();
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
    public Map<String, Long> getPermissionUsage() { return permissionUsage; }


    /**
     * The definitive, final, and perfected method to get the last used time for each permission.
     * This uses Java Reflection to access hidden Android APIs, bypassing all build errors.
     */
    public void fetchPermissionUsage(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return;

        try {
            int uid = pm.getPackageUid(packageName, 0);
            for (String permission : dangerousPermissions) {
                String op = AppOpsManager.permissionToOp(permission);
                if (op == null) continue;

                // Use reflection to call getOpsForPackage
                Method getOpsForPackageMethod = AppOpsManager.class.getMethod("getOpsForPackage", int.class, String.class, String[].class);
                @SuppressWarnings("unchecked")
                List<Object> ops = (List<Object>) getOpsForPackageMethod.invoke(appOps, uid, packageName, new String[]{op});

                if (ops != null && !ops.isEmpty()) {
                    Object packageOps = ops.get(0);
                    // Use reflection to call getOps
                    Method getOpsMethod = packageOps.getClass().getMethod("getOps");
                    @SuppressWarnings("unchecked")
                    List<Object> opEntries = (List<Object>) getOpsMethod.invoke(packageOps);

                    if (opEntries != null && !opEntries.isEmpty()) {
                        Object opEntry = opEntries.get(0);
                        // Use reflection to call getLastAccessTime
                        Method getLastAccessTimeMethod = opEntry.getClass().getMethod("getLastAccessTime");
                        long lastTime = (long) getLastAccessTimeMethod.invoke(opEntry);

                        if(lastTime > 0) {
                            permissionUsage.put(permission, lastTime);
                            Log.d("PrivacyScope", "SUCCESS: Found usage for " + permission + " on " + appName);
                        }
                    } else {
                        Log.w("PrivacyScope", "OpEntries list is null or empty for " + permission);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PrivacyScope", "Reflection failed to get permission usage", e);
        }
    }


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
                    // Ignore
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

