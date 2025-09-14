package com.example.privacyscope;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

public class AppDetailActivity extends AppCompatActivity {

    // --- UI Components ---
    private ImageView appIconImageView;
    private TextView appNameTextView, appVersionTextView, riskScoreTextView, riskLevelTextView;
    private ProgressBar riskProgressBar;
    private LinearLayout permissionsContainer, trackersContainer, insightsContainer, recommendationsContainer;
    private Button managePermissionsButton;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        initializeViews();

        // --- !! CRASH FIX !! ---
        // Safely get the package name from the intent that started this activity.
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("PACKAGE_NAME")) {
            String packageName = intent.getStringExtra("PACKAGE_NAME");
            if (packageName != null && !packageName.isEmpty()) {
                loadAppDetails(packageName);
            } else {
                // Handle the case where package name is invalid
                Toast.makeText(this, "Error: Invalid package name.", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity
            }
        } else {
            // Handle the case where the intent is missing necessary data
            Toast.makeText(this, "Error: Could not load app details.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
        }


        backButton.setOnClickListener(v -> finish());

        managePermissionsButton.setOnClickListener(v -> {
            String packageName = intent.getStringExtra("PACKAGE_NAME");
            if (packageName != null) {
                Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", packageName, null);
                settingsIntent.setData(uri);
                startActivity(settingsIntent);
            }
        });
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
    }

    private void loadAppDetails(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS);
            AppInfo appInfo = new AppInfo(packageInfo, pm);

            // Populate UI
            appIconImageView.setImageDrawable(appInfo.getIcon());
            appNameTextView.setText(appInfo.getAppName());
            appVersionTextView.setText("Version " + packageInfo.versionName);

            // Display Risk Score and Heatmap
            riskProgressBar.setProgress(appInfo.getRiskScore());
            riskLevelTextView.setText(appInfo.getRiskLevel().toString());
            updateRiskColors(appInfo.getRiskLevel());

            displayPermissions(appInfo.getDangerousPermissions());
            displayTrackers(appInfo.getDetectedTrackers());
            displayInsightsAndRecommendations(appInfo);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "App information not found.", Toast.LENGTH_SHORT).show();
            finish();
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
            default: // LOW
                colorRes = R.color.risk_low;
                progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_low_risk);
                break;
        }
        riskLevelTextView.setTextColor(ContextCompat.getColor(this, colorRes));
        riskProgressBar.setProgressDrawable(progressDrawable);
    }

    private void displayPermissions(List<String> permissions) {
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

            permissionName.setText(permission.substring(permission.lastIndexOf('.') + 1));
            permissionDescription.setText(getPermissionExplanation(permission));
            permissionsContainer.addView(permissionView);
        }
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

        // Insights Logic
        if (appInfo.getRiskLevel() == AppInfo.RiskLevel.HIGH) {
            addInsight("This app poses a maximum privacy risk.");
        }
        if (appInfo.getDangerousPermissions().size() > 4) {
            addInsight("Accesses a wide range of device capabilities.");
        }
        if (appInfo.getDetectedTrackers().size() > 3) {
            addInsight("Engages in extensive cross-platform tracking.");
        }

        // Recommendations Logic
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

