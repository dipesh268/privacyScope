package com.example.privacyscope;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComparisonActivity extends AppCompatActivity {

    // --- UI Components ---
    // Selection View
    private LinearLayout selectionLayout;
    private MaterialCardView selectApp1Button, selectApp2Button;
    private ImageView app1Icon, app2Icon;
    private TextView app1Name, app2Name;
    private Button compareButton;
    private ProgressBar loadingIndicator;

    // Result View
    private View comparisonResultLayout;
    private TextView recommendationText;
    private ImageView resultApp1Icon, resultApp2Icon;
    private TextView resultApp1Name, resultApp2Name, resultApp1Score, resultApp2Score;
    private LinearLayout app1PermissionsLayout, app2PermissionsLayout;
    private LinearLayout app1TrackersLayout, app2TrackersLayout;
    private Button compareNewAppsButton;


    // --- Data ---
    private final List<AppInfo> allApps = new ArrayList<>();
    private AppInfo appInfo1, appInfo2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison);

        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        loadAllApps(); // Load apps in the background
    }

    private void initializeViews() {
        // Selection View
        selectionLayout = findViewById(R.id.selectionLayout);
        selectApp1Button = findViewById(R.id.selectApp1Button);
        selectApp2Button = findViewById(R.id.selectApp2Button);
        app1Icon = findViewById(R.id.app1Icon);
        app2Icon = findViewById(R.id.app2Icon);
        app1Name = findViewById(R.id.app1Name);
        app2Name = findViewById(R.id.app2Name);
        compareButton = findViewById(R.id.compareButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);


        // Result View
        comparisonResultLayout = findViewById(R.id.comparisonResultLayout);
        recommendationText = findViewById(R.id.recommendationText);
        resultApp1Icon = findViewById(R.id.resultApp1Icon);
        resultApp2Icon = findViewById(R.id.resultApp2Icon);
        resultApp1Name = findViewById(R.id.resultApp1Name);
        resultApp2Name = findViewById(R.id.resultApp2Name);
        resultApp1Score = findViewById(R.id.resultApp1Score);
        resultApp2Score = findViewById(R.id.resultApp2Score);
        app1PermissionsLayout = findViewById(R.id.app1PermissionsLayout);
        app2PermissionsLayout = findViewById(R.id.app2PermissionsLayout);
        app1TrackersLayout = findViewById(R.id.app1TrackersLayout);
        app2TrackersLayout = findViewById(R.id.app2TrackersLayout);
        compareNewAppsButton = findViewById(R.id.compareNewAppsButton);
    }

    private void setupClickListeners() {
        selectApp1Button.setOnClickListener(v -> showAppSelectionDialog(1));
        selectApp2Button.setOnClickListener(v -> showAppSelectionDialog(2));

        compareButton.setOnClickListener(v -> {
            if (appInfo1 != null && appInfo2 != null) {
                selectionLayout.setVisibility(View.GONE);
                comparisonResultLayout.setVisibility(View.VISIBLE);
                performComparison();
            } else {
                Toast.makeText(this, "Please select two apps to compare.", Toast.LENGTH_SHORT).show();
            }
        });

        // Add listener for the new button
        compareNewAppsButton.setOnClickListener(v -> {
            // Reset the view to the initial selection state
            selectionLayout.setVisibility(View.VISIBLE);
            comparisonResultLayout.setVisibility(View.GONE);
            // Clear selections
            appInfo1 = null;
            appInfo2 = null;
            app1Name.setText("Select App 1");
            app2Name.setText("Select App 2");
            app1Icon.setImageResource(R.drawable.ic_add);
            app2Icon.setImageResource(R.drawable.ic_add);
        });
    }


    private void loadAllApps() {
        // Show loading indicator and disable buttons
        loadingIndicator.setVisibility(View.VISIBLE);
        selectApp1Button.setEnabled(false);
        selectApp2Button.setEnabled(false);

        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS);
            allApps.clear();
            for (PackageInfo packageInfo : packages) {
                if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    allApps.add(new AppInfo(packageInfo, pm));
                }
            }
            Collections.sort(allApps, (a1, a2) -> a1.getAppName().compareToIgnoreCase(a2.getAppName()));

            // Update UI on the main thread when loading is complete
            runOnUiThread(() -> {
                loadingIndicator.setVisibility(View.GONE);
                selectApp1Button.setEnabled(true);
                selectApp2Button.setEnabled(true);
            });
        }).start();
    }

    private void showAppSelectionDialog(int requestCode) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_app_selection);
        RecyclerView recyclerView = dialog.findViewById(R.id.dialogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AppDialogAdapter dialogAdapter = new AppDialogAdapter(allApps, app -> {
            if (requestCode == 1) {
                appInfo1 = app;
                app1Name.setText(app.getAppName());
                app1Icon.setImageDrawable(app.getIcon());
            } else {
                appInfo2 = app;
                app2Name.setText(app.getAppName());
                app2Icon.setImageDrawable(app.getIcon());
            }
            dialog.dismiss();
        });
        recyclerView.setAdapter(dialogAdapter);
        dialog.show();
    }


    private void performComparison() {
        // --- Populate Header Info ---
        resultApp1Icon.setImageDrawable(appInfo1.getIcon());
        resultApp1Name.setText(appInfo1.getAppName());
        resultApp1Score.setText("Risk: " + appInfo1.getRiskScore());

        resultApp2Icon.setImageDrawable(appInfo2.getIcon());
        resultApp2Name.setText(appInfo2.getAppName());
        resultApp2Score.setText("Risk: " + appInfo2.getRiskScore());

        // --- Clear previous results ---
        app1PermissionsLayout.removeAllViews();
        app2PermissionsLayout.removeAllViews();
        app1TrackersLayout.removeAllViews();
        app2TrackersLayout.removeAllViews();

        // --- Compare Permissions ---
        Set<String> app2Permissions = new HashSet<>(appInfo2.getDangerousPermissions());
        for (String permission : appInfo1.getDangerousPermissions()) {
            boolean isUnique = !app2Permissions.contains(permission);
            addTextViewToList(app1PermissionsLayout, permission, isUnique);
        }

        Set<String> app1Permissions = new HashSet<>(appInfo1.getDangerousPermissions());
        for (String permission : appInfo2.getDangerousPermissions()) {
            boolean isUnique = !app1Permissions.contains(permission);
            addTextViewToList(app2PermissionsLayout, permission, isUnique);
        }

        // --- Compare Trackers ---
        Set<String> app2Trackers = new HashSet<>(appInfo2.getDetectedTrackers());
        for (String tracker : appInfo1.getDetectedTrackers()) {
            boolean isUnique = !app2Trackers.contains(tracker);
            addTextViewToList(app1TrackersLayout, tracker, isUnique);
        }

        Set<String> app1Trackers = new HashSet<>(appInfo1.getDetectedTrackers());
        for (String tracker : appInfo2.getDetectedTrackers()) {
            boolean isUnique = !app1Trackers.contains(tracker);
            addTextViewToList(app2TrackersLayout, tracker, isUnique);
        }

        // --- Generate Recommendation ---
        generateComparisonRecommendation(appInfo1, appInfo2);
    }

    private void generateComparisonRecommendation(AppInfo app1, AppInfo app2) {
        String recommendation;
        if (app1.getRiskScore() < app2.getRiskScore()) {
            recommendation = app1.getAppName() + " appears to be safer than " + app2.getAppName() + ".";
        } else if (app2.getRiskScore() < app1.getRiskScore()) {
            recommendation = app2.getAppName() + " appears to be safer than " + app1.getAppName() + ".";
        } else {
            // Scores are equal, decide based on trackers
            if (app1.getDetectedTrackers().size() < app2.getDetectedTrackers().size()) {
                recommendation = app1.getAppName() + " may be safer as it has fewer trackers.";
            } else if (app2.getDetectedTrackers().size() < app1.getDetectedTrackers().size()) {
                recommendation = app2.getAppName() + " may be safer as it has fewer trackers.";
            } else {
                recommendation = "Both apps have a similar privacy risk profile.";
            }
        }
        recommendationText.setText(recommendation);
    }

    private void addTextViewToList(LinearLayout layout, String text, boolean isHighlighted) {
        View view = getLayoutInflater().inflate(R.layout.list_item_comparison, layout, false);
        TextView textView = view.findViewById(R.id.itemName);

        // Clean up permission string for display
        if (text.contains(".")) {
            text = text.substring(text.lastIndexOf('.') + 1);
        }
        textView.setText(text);

        if (isHighlighted) {
            textView.setBackgroundResource(R.drawable.item_background_highlight);
            textView.setTextColor(ContextCompat.getColor(this, R.color.risk_high));
        }
        layout.addView(view);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_comparison);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_app_list) {
                startActivity(new Intent(getApplicationContext(), AppListActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_comparison) {
                return true;
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(getApplicationContext(), ReportsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}

