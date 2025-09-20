package com.example.privacyscope;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import java.util.stream.Collectors;

public class ComparisonActivity extends AppCompatActivity {

    // UI Components for Selection
    private MaterialCardView selectApp1Button, selectApp2Button;
    private ImageView app1Icon, app2Icon;
    private TextView app1Name, app2Name;
    private Button compareButton;
    private LinearLayout selectionLayout;
    private ProgressBar loadingIndicator;
    private Button compareNewButton;

    // UI Components for Results
    private View comparisonResultLayout;
    private TextView recommendationText;
    private TextView app1ResultName, app2ResultName, app1ResultScore, app2ResultScore;
    private ImageView app1ResultIcon, app2ResultIcon;
    private LinearLayout app1PermissionsLayout, app2PermissionsLayout;
    private LinearLayout app1TrackersLayout, app2TrackersLayout;

    // Data
    private final List<AppInfo> allApps = new ArrayList<>();
    private AppInfo appInfo1, appInfo2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison);

        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        loadAllApps();
    }

    private void initializeViews() {
        // Selection
        selectApp1Button = findViewById(R.id.selectApp1Button);
        selectApp2Button = findViewById(R.id.selectApp2Button);
        app1Icon = findViewById(R.id.app1Icon);
        app2Icon = findViewById(R.id.app2Icon);
        app1Name = findViewById(R.id.app1Name);
        app2Name = findViewById(R.id.app2Name);
        compareButton = findViewById(R.id.compareButton);
        selectionLayout = findViewById(R.id.selectionLayout);
        loadingIndicator = findViewById(R.id.loadingIndicator);


        // --- THIS IS THE FIX ---
        // Results - Using corrected IDs from your new XML layout
        comparisonResultLayout = findViewById(R.id.comparisonResultLayout);
        recommendationText = findViewById(R.id.recommendationText);
        app1ResultName = findViewById(R.id.resultApp1Name); // Corrected ID
        app2ResultName = findViewById(R.id.resultApp2Name); // Corrected ID
        app1ResultScore = findViewById(R.id.resultApp1Score); // Corrected ID
        app2ResultScore = findViewById(R.id.resultApp2Score); // Corrected ID
        app1ResultIcon = findViewById(R.id.resultApp1Icon); // Corrected ID
        app2ResultIcon = findViewById(R.id.resultApp2Icon); // Corrected ID
        app1PermissionsLayout = findViewById(R.id.app1PermissionsLayout);
        app2PermissionsLayout = findViewById(R.id.app2PermissionsLayout);
        app1TrackersLayout = findViewById(R.id.app1TrackersLayout);
        app2TrackersLayout = findViewById(R.id.app2TrackersLayout);
        compareNewButton = findViewById(R.id.compareNewAppsButton); // Corrected ID
        // --- END OF FIX ---
    }

    private void setupClickListeners() {
        selectApp1Button.setOnClickListener(v -> showAppSelectionDialog(1));
        selectApp2Button.setOnClickListener(v -> showAppSelectionDialog(2));
        compareNewButton.setOnClickListener(v -> resetComparisonView());

        compareButton.setOnClickListener(v -> {
            if (appInfo1 != null && appInfo2 != null) {
                selectionLayout.setVisibility(View.GONE);
                comparisonResultLayout.setVisibility(View.VISIBLE);
                performComparison();
            } else {
                Toast.makeText(this, "Please select two apps to compare.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllApps() {
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

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        RecyclerView recyclerView = dialog.findViewById(R.id.dialogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AppDialogAdapter dialogAdapter = new AppDialogAdapter(allApps, app -> {
            if (requestCode == 1) {
                appInfo1 = app;
                app1Name.setText(app.getAppName());
                app1Icon.setImageDrawable(app.getIcon());
                app1Icon.setImageTintList(null);
            } else {
                appInfo2 = app;
                app2Name.setText(app.getAppName());
                app2Icon.setImageDrawable(app.getIcon());
                app2Icon.setImageTintList(null);
            }
            dialog.dismiss();
        });
        recyclerView.setAdapter(dialogAdapter);
        dialog.show();
    }

    private void performComparison() {
        // Set header info
        app1ResultIcon.setImageDrawable(appInfo1.getIcon());
        app2ResultIcon.setImageDrawable(appInfo2.getIcon());
        app1ResultName.setText(appInfo1.getAppName());
        app2ResultName.setText(appInfo2.getAppName());
        app1ResultScore.setText("Risk: " + appInfo1.getRiskScore());
        app2ResultScore.setText("Risk: " + appInfo2.getRiskScore());

        // Compare permissions
        // Compare permissions
        Set<String> app1Perms = appInfo1.getDangerousPermissions().stream()
                .map(detail -> detail.name)
                .collect(Collectors.toSet());

        Set<String> app2Perms = appInfo2.getDangerousPermissions().stream()
                .map(detail -> detail.name)
                .collect(Collectors.toSet());compareAndDisplayLists(app1PermissionsLayout, app1Perms, app2Perms);
        compareAndDisplayLists(app2PermissionsLayout, app2Perms, app1Perms);

        // Compare trackers
        Set<String> app1Trackers = new HashSet<>(appInfo1.getDetectedTrackers());
        Set<String> app2Trackers = new HashSet<>(appInfo2.getDetectedTrackers());
        compareAndDisplayLists(app1TrackersLayout, app1Trackers, app2Trackers);
        compareAndDisplayLists(app2TrackersLayout, app2Trackers, app1Trackers);

        // Set recommendation
        if (appInfo1.getRiskScore() < appInfo2.getRiskScore()) {
            recommendationText.setText(appInfo1.getAppName() + " is safer than " + appInfo2.getAppName());
        } else if (appInfo2.getRiskScore() < appInfo1.getRiskScore()) {
            recommendationText.setText(appInfo2.getAppName() + " is safer than " + appInfo1.getAppName());
        } else {
            recommendationText.setText("Both apps have a similar risk profile.");
        }
    }

    private void compareAndDisplayLists(LinearLayout layout, Set<String> list1, Set<String> list2) {
        layout.removeAllViews();
        List<String> sortedList = new ArrayList<>(list1);
        Collections.sort(sortedList);

        for (String item : sortedList) {
            boolean isUnique = !list2.contains(item);
            String displayName = item.contains(".") ? item.substring(item.lastIndexOf('.') + 1) : item;
            addTextViewToList(layout, displayName, isUnique);
        }
    }


    private void addTextViewToList(LinearLayout layout, String text, boolean isHighlighted) {
        View view = getLayoutInflater().inflate(R.layout.list_item_comparison, layout, false);
        TextView textView = view.findViewById(R.id.itemName);
        textView.setText(text);

        if (isHighlighted) {
            textView.setBackgroundResource(R.drawable.item_background_highlight);
            textView.setTextColor(ContextCompat.getColor(this, R.color.risk_high));
        }

        layout.addView(view);
    }

    private void resetComparisonView() {
        comparisonResultLayout.setVisibility(View.GONE);
        selectionLayout.setVisibility(View.VISIBLE);

        appInfo1 = null;
        appInfo2 = null;

        app1Name.setText("Select App 1");
        app2Name.setText("Select App 2");
        app1Icon.setImageResource(R.drawable.ic_add);
        app2Icon.setImageResource(R.drawable.ic_add);
        app1Icon.setImageTintList(ContextCompat.getColorStateList(this, R.color.risk_low));
        app2Icon.setImageTintList(ContextCompat.getColorStateList(this, R.color.risk_low));
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
            else if (itemId == R.id.nav_alternatives) {
                startActivity(new Intent(getApplicationContext(), AlternativeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}

