package com.example.privacyscope;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

    private AppInfo app1Info, app2Info;
    private final List<AppInfo> allApps = new ArrayList<>();

    // --- UI Components ---
    // Selection Part
    private View selectionLayout;
    private MaterialCardView selectApp1Button, selectApp2Button;
    private ImageView app1Icon, app2Icon;
    private TextView app1Name, app2Name;
    private Button compareButton;

    // Result Part
    private View comparisonResultLayout;
    private TextView recommendationText;
    private ImageView resultApp1Icon, resultApp2Icon;
    private TextView resultApp1Name, resultApp2Name;
    private TextView resultApp1Score, resultApp2Score;
    private LinearLayout app1PermissionsLayout, app2PermissionsLayout, app1TrackersLayout, app2TrackersLayout;


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
        // Selection Views
        selectionLayout = findViewById(R.id.selectionLayout);
        selectApp1Button = findViewById(R.id.selectApp1Button);
        selectApp2Button = findViewById(R.id.selectApp2Button);
        app1Icon = findViewById(R.id.app1Icon);
        app2Icon = findViewById(R.id.app2Icon);
        app1Name = findViewById(R.id.app1Name);
        app2Name = findViewById(R.id.app2Name);
        compareButton = findViewById(R.id.compareButton);

        // Result Views
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
    }

    private void setupClickListeners() {
        selectApp1Button.setOnClickListener(v -> showAppSelectionDialog(1));
        selectApp2Button.setOnClickListener(v -> showAppSelectionDialog(2));

        compareButton.setOnClickListener(v -> {
            if (app1Info != null && app2Info != null) {
                selectionLayout.setVisibility(View.GONE);
                comparisonResultLayout.setVisibility(View.VISIBLE);
                performComparison();
            } else {
                Toast.makeText(this, "Please select two apps to compare.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAppSelectionDialog(int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_app_selection, null);
        builder.setView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final AlertDialog dialog = builder.create();

        AppDialogAdapter adapter = new AppDialogAdapter(allApps, app -> {
            if (app1Info != null && app.getPackageName().equals(app1Info.getPackageName()) ||
                    app2Info != null && app.getPackageName().equals(app2Info.getPackageName())) {
                Toast.makeText(this, "This app is already selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == 1) {
                app1Info = app;
                app1Icon.setImageDrawable(app.getIcon());
                app1Name.setText(app.getAppName());
            } else {
                app2Info = app;
                app2Icon.setImageDrawable(app.getIcon());
                app2Name.setText(app.getAppName());
            }
            compareButton.setEnabled(app1Info != null && app2Info != null);
            dialog.dismiss();
        });

        recyclerView.setAdapter(adapter);
        dialog.show();
    }

    private void loadAllApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            allApps.clear();
            for (PackageInfo packageInfo : packages) {
                if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    allApps.add(new AppInfo(packageInfo, pm));
                }
            }
            Collections.sort(allApps, (a1, a2) -> a1.getAppName().compareToIgnoreCase(a2.getAppName()));
        }).start();
    }

    private void performComparison() {
        // Populate header info
        resultApp1Icon.setImageDrawable(app1Info.getIcon());
        resultApp2Icon.setImageDrawable(app2Info.getIcon());
        resultApp1Name.setText(app1Info.getAppName());
        resultApp2Name.setText(app2Info.getAppName());
        resultApp1Score.setText("Score: " + app1Info.getRiskScore());
        resultApp2Score.setText("Score: " + app2Info.getRiskScore());


        // Clear previous results
        app1PermissionsLayout.removeAllViews();
        app2PermissionsLayout.removeAllViews();
        app1TrackersLayout.removeAllViews();
        app2TrackersLayout.removeAllViews();

        // Compare Risk Scores and Set Recommendation
        if (app1Info.getRiskScore() < app2Info.getRiskScore()) {
            recommendationText.setText(app1Info.getAppName() + " is safer than " + app2Info.getAppName());
        } else if (app2Info.getRiskScore() < app1Info.getRiskScore()) {
            recommendationText.setText(app2Info.getAppName() + " is safer than " + app1Info.getAppName());
        } else {
            recommendationText.setText("Both apps have a similar risk profile.");
        }

        // Compare Permissions
        Set<String> app1Perms = new HashSet<>(app1Info.getDangerousPermissions());
        Set<String> app2Perms = new HashSet<>(app2Info.getDangerousPermissions());

        for (String perm : app1Perms) {
            boolean isUnique = !app2Perms.contains(perm);
            addTextViewToList(app1PermissionsLayout, perm.substring(perm.lastIndexOf('.') + 1), isUnique);
        }
        for (String perm : app2Perms) {
            boolean isUnique = !app1Perms.contains(perm);
            addTextViewToList(app2PermissionsLayout, perm.substring(perm.lastIndexOf('.') + 1), isUnique);
        }

        // Compare Trackers
        Set<String> app1Trackers = new HashSet<>(app1Info.getDetectedTrackers());
        Set<String> app2Trackers = new HashSet<>(app2Info.getDetectedTrackers());

        for (String tracker : app1Trackers) {
            boolean isUnique = !app2Trackers.contains(tracker);
            addTextViewToList(app1TrackersLayout, tracker, isUnique);
        }
        for (String tracker : app2Trackers) {
            boolean isUnique = !app1Trackers.contains(tracker);
            addTextViewToList(app2TrackersLayout, tracker, isUnique);
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

