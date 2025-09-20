package com.example.privacyscope;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListActivity extends AppCompatActivity {

    private RecyclerView appListRecyclerView;
    private AppListAdapter adapter;
    private final List<AppInfo> allApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        appListRecyclerView = findViewById(R.id.allAppsRecyclerView);
        appListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(allApps, this);
        appListRecyclerView.setAdapter(adapter);

        // Setup Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_app_list);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_app_list) {
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
            else if (itemId == R.id.nav_alternatives) {
                startActivity(new Intent(getApplicationContext(), AlternativeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        loadAllApps();
    }

    private void loadAllApps() {
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

            // Sort apps alphabetically by name for this list
            Collections.sort(allApps, (a1, a2) -> a1.getAppName().compareToIgnoreCase(a2.getAppName()));

            // Update UI on the main thread
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }).start();
    }
}

