package com.example.privacyscope;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView highRiskCountTextView, mediumRiskCountTextView, lowRiskCountTextView;
    private RecyclerView riskyAppsRecyclerView;
    private MaterialButton scanAppsButton;
    private BottomNavigationView bottomNavigationView;
    private RiskyAppsAdapter adapter;

    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> topRiskyApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Components
        highRiskCountTextView = findViewById(R.id.highRiskCount);
        mediumRiskCountTextView = findViewById(R.id.mediumRiskCount);
        lowRiskCountTextView = findViewById(R.id.lowRiskCount);
        riskyAppsRecyclerView = findViewById(R.id.riskyAppsRecyclerView);
        scanAppsButton = findViewById(R.id.scanAppsButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);

        // Setup RecyclerView
        riskyAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RiskyAppsAdapter(topRiskyApps, this);
        riskyAppsRecyclerView.setAdapter(adapter);

        // Load data on start
        loadAndAnalyzeApps();

        // Setup Listeners
        scanAppsButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Re-scanning apps...", Toast.LENGTH_SHORT).show();
            loadAndAnalyzeApps();
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                return true;
            } else if (itemId == R.id.nav_app_list) {
                startActivity(new Intent(getApplicationContext(), AppListActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_comparison) {
                startActivity(new Intent(getApplicationContext(), ComparisonActivity.class));
                overridePendingTransition(0, 0);
                finish();
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

    private void loadAndAnalyzeApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            allApps.clear();

            for (PackageInfo packageInfo : packages) {
                // Check if it's a user-installed app
                if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    // This is the corrected way to create the AppInfo object
                    AppInfo appInfo = new AppInfo(packageInfo, pm);
                    allApps.add(appInfo);
                }
            }

            // Sort apps by risk score descending
            Collections.sort(allApps, (a1, a2) -> Integer.compare(a2.getRiskScore(), a1.getRiskScore()));

            // Update UI on the main thread
            runOnUiThread(this::updateDashboardUI);
        }).start();
    }

    private void updateDashboardUI() {
        int highRisk = 0, mediumRisk = 0, lowRisk = 0;
        for (AppInfo app : allApps) {
            switch (app.getRiskLevel()) {
                case HIGH: highRisk++; break;
                case MEDIUM: mediumRisk++; break;
                case LOW: lowRisk++; break;
            }
        }

        highRiskCountTextView.setText(String.valueOf(highRisk));
        mediumRiskCountTextView.setText(String.valueOf(mediumRisk));
        lowRiskCountTextView.setText(String.valueOf(lowRisk));

        topRiskyApps.clear();
        topRiskyApps.addAll(allApps.subList(0, Math.min(allApps.size(), 5)));
        adapter.notifyDataSetChanged();
    }
}

