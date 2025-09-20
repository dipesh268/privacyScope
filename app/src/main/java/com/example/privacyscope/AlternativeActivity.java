package com.example.privacyscope;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlternativeActivity extends AppCompatActivity {

    // --- UI Components ---
    private MaterialCardView selectAppButton;
    private ImageView selectedAppIcon;
    private TextView selectedAppName;
    private View selectionLayout, resultsLayout;
    private LinearLayout alternativesContainer;
    private ProgressBar loadingIndicator, resultsLoadingIndicator;
    // --- NEW UI Components for Robust Error Handling ---
    private LinearLayout errorLayout;
    private TextView errorMessage;
    private Button tryAgainButton;
    private String lastSearchedAppName; // To remember the last search for the "Try Again" button

    // --- Data & Threading ---
    private final List<AppInfo> allApps = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // A helper class to handle results and errors from the background thread gracefully
    private static class AlternativeResult {
        final List<AlternativeApp> alternatives;
        final Exception error;

        AlternativeResult(List<AlternativeApp> alternatives) { this.alternatives = alternatives; this.error = null; }
        AlternativeResult(Exception error) { this.alternatives = null; this.error = error; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternative);

        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        loadAllApps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown(); // Prevents memory leaks by stopping background threads
    }

    private void initializeViews() {
        selectAppButton = findViewById(R.id.selectAppButton);
        selectedAppIcon = findViewById(R.id.selectedAppIcon);
        selectedAppName = findViewById(R.id.selectedAppName);
        selectionLayout = findViewById(R.id.selectionLayout);
        resultsLayout = findViewById(R.id.resultsLayout);
        alternativesContainer = findViewById(R.id.alternativesContainer);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        resultsLoadingIndicator = findViewById(R.id.resultsLoadingIndicator);
        errorLayout = findViewById(R.id.errorLayout);
        errorMessage = findViewById(R.id.errorMessage);
        tryAgainButton = findViewById(R.id.tryAgainButton);
    }

    private void setupClickListeners() {
        selectAppButton.setOnClickListener(v -> {
            if (allApps.isEmpty()) {
                Toast.makeText(this, "Still loading apps, please wait...", Toast.LENGTH_SHORT).show();
            } else {
                showAppSelectionDialog();
            }
        });

        tryAgainButton.setOnClickListener(v -> {
            if(lastSearchedAppName != null && !lastSearchedAppName.isEmpty()){
                findAndDisplayAlternatives(lastSearchedAppName);
            }
        });
    }

    private void loadAllApps() {
        loadingIndicator.setVisibility(View.VISIBLE);
        selectAppButton.setEnabled(false);
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            allApps.clear();
            for (PackageInfo packageInfo : packages) {
                if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    allApps.add(new AppInfo(packageInfo, pm));
                }
            }
            Collections.sort(allApps, (a1, a2) -> a1.getAppName().compareToIgnoreCase(a2.getAppName()));
            handler.post(() -> {
                loadingIndicator.setVisibility(View.GONE);
                selectAppButton.setEnabled(true);
            });
        });
    }

    private void showAppSelectionDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_app_selection);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        RecyclerView recyclerView = dialog.findViewById(R.id.dialogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AppDialogAdapter dialogAdapter = new AppDialogAdapter(allApps, app -> {
            selectedAppName.setText(app.getAppName());
            selectedAppIcon.setImageDrawable(app.getIcon());
            selectedAppIcon.setImageTintList(null);
            findAndDisplayAlternatives(app.getAppName());
            dialog.dismiss();
        });
        recyclerView.setAdapter(dialogAdapter);
        dialog.show();
    }

    private void findAndDisplayAlternatives(String appName) {
        lastSearchedAppName = appName;
        selectionLayout.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        resultsLoadingIndicator.setVisibility(View.VISIBLE);
        alternativesContainer.removeAllViews();
        errorLayout.setVisibility(View.GONE);

        executor.execute(() -> {
            AlternativeResult result = findAlternativesOnline(appName);
            handler.post(() -> {
                resultsLoadingIndicator.setVisibility(View.GONE);
                if (result.error != null) {
                    showErrorMessage("Could not connect. Please check your internet connection and try again.", true);
                } else if (result.alternatives.isEmpty()) {
                    showErrorMessage("No safer alternatives found online for \"" + appName + "\".", false);
                } else {
                    displayAlternativeList(result.alternatives);
                }
            });
        });
    }

    private void displayAlternativeList(List<AlternativeApp> alternatives) {
        alternativesContainer.setVisibility(View.VISIBLE);
        for (AlternativeApp alt : alternatives) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.list_item_alternative, alternativesContainer, false);
            ImageView icon = itemView.findViewById(R.id.alternativeAppIcon);
            TextView name = itemView.findViewById(R.id.alternativeAppName);

            loadIconFromUrl(icon, alt.getIconUrl());
            name.setText(alt.getAppName());

            itemView.setOnClickListener(v -> {
                try {
                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + alt.getPackageName()));
                    startActivity(playStoreIntent);
                } catch (android.content.ActivityNotFoundException anfe) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + alt.getPackageName()));
                    startActivity(browserIntent);
                }
            });
            alternativesContainer.addView(itemView);
        }
    }

    private void showErrorMessage(String message, boolean showTryAgain) {
        alternativesContainer.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        errorMessage.setText(message);
        tryAgainButton.setVisibility(showTryAgain ? View.VISIBLE : View.GONE);
    }

    private void loadIconFromUrl(ImageView imageView, String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            handler.post(() -> imageView.setImageResource(R.mipmap.ic_launcher_round)); // Set placeholder if no icon URL
            return;
        }
        String fullUrl = "https://f-droid.org" + urlString;
        executor.execute(() -> {
            try {
                InputStream in = new java.net.URL(fullUrl).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                handler.post(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                Log.e("AlternativeActivity", "Error loading image", e);
                handler.post(() -> imageView.setImageResource(R.mipmap.ic_launcher_round)); // Fallback placeholder on error
            }
        });
    }

    private AlternativeResult findAlternativesOnline(String originalAppName) {
        List<AlternativeApp> alternatives = new ArrayList<>();
        String query = getSearchQueryForAppName(originalAppName);
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("https://f-droid.org/api/v1/search?q=" + URLEncoder.encode(query, "UTF-8"));
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);

            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();

            JSONObject response = new JSONObject(stringBuilder.toString());
            JSONArray apps = response.optJSONArray("apps");

            if (apps != null) {
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject app = apps.getJSONObject(i);
                    // Use optString for safer parsing
                    String name = app.optJSONObject("name").optString("en-US", app.optString("packageName"));
                    String packageName = app.getString("packageName");
                    String iconUrl = app.optString("icon");
                    alternatives.add(new AlternativeApp(name, packageName, iconUrl));
                }
            }
            return new AlternativeResult(alternatives);
        } catch (Exception e) {
            Log.e("AlternativeActivity", "Error fetching alternatives", e);
            return new AlternativeResult(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private String getSearchQueryForAppName(String appName) {
        String lowerCaseName = appName.toLowerCase();
        if (lowerCaseName.contains("whatsapp") || lowerCaseName.contains("messenger")) return "private messenger";
        if (lowerCaseName.contains("chrome") || lowerCaseName.contains("browser")) return "private browser";
        if (lowerCaseName.contains("instagram") || lowerCaseName.contains("tiktok")) return "photo sharing";
        if (lowerCaseName.contains("facebook")) return "social media";
        if (lowerCaseName.contains("gmail") || lowerCaseName.contains("outlook")) return "email client";
        return appName; // Fallback to the actual app name for a direct search
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_alternatives);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            } else if (itemId == R.id.nav_app_list) {
                startActivity(new Intent(getApplicationContext(), AppListActivity.class));
            } else if (itemId == R.id.nav_comparison) {
                startActivity(new Intent(getApplicationContext(), ComparisonActivity.class));
            } else if (itemId == R.id.nav_alternatives) {
                return true;
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(getApplicationContext(), ReportsActivity.class));
            }
            if (itemId != R.id.nav_alternatives) {
                overridePendingTransition(0, 0);
                finish();
            }
            return true;
        });
    }
}

