package com.example.privacyscope;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppDetailActivity extends AppCompatActivity {

    private static final int USAGE_STATS_REQUEST_CODE = 101;

    private ImageView appIconImageView;
    private TextView appNameTextView, appVersionTextView, riskLevelTextView, appLastUsedTextView;
    private ProgressBar riskProgressBar;
    private LinearLayout permissionsContainer, trackersContainer, insightsContainer, recommendationsContainer;
    private Button managePermissionsButton;
    private ImageView backButton;
    private View usagePermissionPrompt;

    private String currentPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        initializeViews();
        setupClickListeners();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("PACKAGE_NAME")) {
            currentPackageName = intent.getStringExtra("PACKAGE_NAME");
            if (currentPackageName != null && !currentPackageName.isEmpty()) {
                loadAppDetails(currentPackageName);
            } else {
                Toast.makeText(this, "Error: Invalid package name.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Error: Could not load app details.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USAGE_STATS_REQUEST_CODE) {
            loadAppDetails(currentPackageName);
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        appIconImageView = findViewById(R.id.appIcon);
        appNameTextView = findViewById(R.id.appName);
        appVersionTextView = findViewById(R.id.appVersion);
        riskProgressBar = findViewById(R.id.riskProgressBar);
        riskLevelTextView = findViewById(R.id.riskLevelTextView);
        permissionsContainer = findViewById(R.id.permissionsContainer);
        trackersContainer = findViewById(R.id.trackersContainer);
        insightsContainer = findViewById(R.id.insightsContainer);
        recommendationsContainer = findViewById(R.id.recommendationsContainer);
        managePermissionsButton = findViewById(R.id.managePermissionsButton);
        appLastUsedTextView = findViewById(R.id.appLastUsed);
        usagePermissionPrompt = findViewById(R.id.usagePermissionPrompt);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        managePermissionsButton.setOnClickListener(v -> {
            if (currentPackageName != null) {
                Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", currentPackageName, null);
                settingsIntent.setData(uri);
                startActivity(settingsIntent);
            }
        });
    }

    private void loadAppDetails(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS);
            AppInfo appInfo = new AppInfo(packageInfo, pm);

            appIconImageView.setImageDrawable(appInfo.getIcon());
            appNameTextView.setText(appInfo.getAppName());
            appVersionTextView.setText("Version " + packageInfo.versionName);

            updateRiskColors(appInfo.getRiskLevel());
            riskProgressBar.setProgress(appInfo.getRiskScore());
            riskLevelTextView.setText(appInfo.getRiskLevel().toString());

            if (hasUsageStatsPermission()) {
                usagePermissionPrompt.setVisibility(View.GONE);
                fetchAndDisplayAppLastUsedTime(appInfo);
                appInfo.fetchPermissionUsage(this);
            } else {
                appLastUsedTextView.setVisibility(View.GONE);
                addPermissionPrompt();
            }

            displayPermissions(appInfo.getDangerousPermissions(), appInfo.getPermissionUsage());
            displayTrackers(appInfo.getDetectedTrackers());
            displayInsightsAndRecommendations(appInfo);

        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "App information not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchAndDisplayAppLastUsedTime(AppInfo appInfo) {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        long startTime = time - (1000L * 3600 * 24 * 30);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, time);

        if (appList != null && !appList.isEmpty()) {
            long lastTimeUsed = 0;
            for (UsageStats usageStats : appList) {
                if (usageStats.getPackageName().equals(currentPackageName)) {
                    if (usageStats.getLastTimeUsed() > lastTimeUsed) {
                        lastTimeUsed = usageStats.getLastTimeUsed();
                    }
                }
            }
            appInfo.setLastTimeUsed(lastTimeUsed);
        }

        if (appInfo.getLastTimeUsed() > 0) {
            appLastUsedTextView.setText("App last used: " + formatTimeAgo(appInfo.getLastTimeUsed()));
            appLastUsedTextView.setVisibility(View.VISIBLE);
        } else {
            appLastUsedTextView.setText("App not used in the last 30 days");
            appLastUsedTextView.setVisibility(View.VISIBLE);
        }
    }

    private void addPermissionPrompt() {
        if (usagePermissionPrompt != null) {
            usagePermissionPrompt.setVisibility(View.VISIBLE);
            Button grantButton = usagePermissionPrompt.findViewById(R.id.grantUsagePermissionButton);
            grantButton.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, USAGE_STATS_REQUEST_CODE);
            });
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private String formatTimeAgo(long timestamp) {
        if (timestamp == 0) return "Not used recently";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        if (seconds < 60) return "Just now";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes < 60) return minutes + " min ago";
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 24) return hours + " hours ago";
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return days + " days ago";
    }

    private void displayPermissions(List<String> permissions, Map<String, Long> usageMap) {
        permissionsContainer.removeAllViews();
        if (permissions.isEmpty()) {
            TextView noItems = new TextView(this);
            noItems.setText("No dangerous permissions found.");
            noItems.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary));
            noItems.setPadding(16, 16, 16, 16);
            permissionsContainer.addView(noItems);
            return;
        }

        for (String permission : permissions) {
            View permissionView = getLayoutInflater().inflate(R.layout.list_item_permission, permissionsContainer, false);
            TextView permissionName = permissionView.findViewById(R.id.permissionName);
            TextView permissionDescription = permissionView.findViewById(R.id.permissionDescription);
            TextView permissionLastUsed = permissionView.findViewById(R.id.permissionLastUsed);

            permissionName.setText(permission.substring(permission.lastIndexOf('.') + 1));
            permissionDescription.setText(getPermissionExplanation(permission));

            if(hasUsageStatsPermission()) {
                Long lastUsedTimestamp = usageMap.get(permission);
                if (lastUsedTimestamp != null && lastUsedTimestamp > 0) {
                    permissionLastUsed.setText("Last used: " + formatTimeAgo(lastUsedTimestamp));
                    permissionLastUsed.setVisibility(View.VISIBLE);
                } else {
                    permissionLastUsed.setText("Usage data not available from Android OS");
                    permissionLastUsed.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary));
                    permissionLastUsed.setVisibility(View.VISIBLE);
                }
            } else {
                permissionLastUsed.setVisibility(View.GONE);
            }

            permissionsContainer.addView(permissionView);
        }
    }

    private void updateRiskColors(AppInfo.RiskLevel level) {
        int colorRes;
        Drawable progressDrawable;

        switch (level) {
            case HIGH:
                colorRes = R.color.risk_high;
                progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_high_risk);
                break;
            case MEDIUM:
                colorRes = R.color.risk_medium;
                progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_medium_risk);
                break;
            default:
                colorRes = R.color.risk_low;
                progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_low_risk);
                break;
        }
        riskLevelTextView.setTextColor(ContextCompat.getColor(this, colorRes));
        riskProgressBar.setProgressDrawable(progressDrawable);
    }

    private void displayTrackers(List<String> trackers) {
        trackersContainer.removeAllViews();
        if (trackers.isEmpty()) {
            TextView noItems = new TextView(this);
            noItems.setText("No trackers detected.");
            noItems.setPadding(16, 16, 16, 16);
            trackersContainer.addView(noItems);
            return;
        }

        for (String tracker : trackers) {
            View trackerView = getLayoutInflater().inflate(R.layout.list_item_tracker, trackersContainer, false);
            TextView trackerName = trackerView.findViewById(R.id.trackerName);
            trackerName.setText(tracker);
            trackersContainer.addView(trackerView);
        }
    }

    private void displayInsightsAndRecommendations(AppInfo appInfo) {
        insightsContainer.removeAllViews();
        recommendationsContainer.removeAllViews();

        if (appInfo.getRiskLevel() == AppInfo.RiskLevel.HIGH) {
            addInsight("This app poses a maximum privacy risk.");
        }
        if (appInfo.getDangerousPermissions().size() > 4) {
            addInsight("Accesses a wide range of device capabilities.");
        }
        if (appInfo.getDetectedTrackers().size() > 3) {
            addInsight("Engages in extensive cross-platform tracking.");
        }

        if (appInfo.getRiskScore() > 80) {
            addRecommendation("Strongly consider uninstalling.");
        }
        if (appInfo.getDetectedTrackers().size() > 0) {
            addRecommendation("Use a web version if available to limit tracking.");
        }
        if (appInfo.getDangerousPermissions().size() > 0) {
            addRecommendation("Revoke all unnecessary permissions.");
        }
    }

    private void addInsight(String text) {
        View insightView = getLayoutInflater().inflate(R.layout.list_item_insight, insightsContainer, false);
        TextView insightText = insightView.findViewById(R.id.insightText);
        insightText.setText(text);
        insightsContainer.addView(insightView);
    }

    private void addRecommendation(String text) {
        View recommendationView = getLayoutInflater().inflate(R.layout.list_item_insight, recommendationsContainer, false);
        TextView insightText = recommendationView.findViewById(R.id.insightText);
        insightText.setText(text);
        recommendationsContainer.addView(recommendationView);
    }

    private String getPermissionExplanation(String permission) {
        switch (permission) {
            case "android.permission.READ_SMS": return "Allows the app to read your text messages.";
            case "android.permission.ACCESS_FINE_LOCATION": return "Allows the app to get your precise location using GPS.";
            case "android.permission.CAMERA": return "Allows the app to take pictures and record video.";
            case "android.permission.RECORD_AUDIO": return "Allows the app to record audio with the microphone.";
            case "android.permission.WRITE_EXTERNAL_STORAGE": return "Allows the app to read and write files on your device's storage.";
            case "android.permission.READ_PHONE_STATE": return "Allows the app to access your phone number and device IDs.";
            case "android.permission.READ_CONTACTS": return "Allows the app to read your contacts list.";
            default: return "This permission grants access to sensitive system features.";
        }
    }
}
