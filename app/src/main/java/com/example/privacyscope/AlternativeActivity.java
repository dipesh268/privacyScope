package com.example.privacyscope;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private TextView selectedAppName, resultsTitle, errorMessage;
    private View selectionLayout, resultsLayout, errorLayout;
    private LinearLayout alternativesContainer;
    private ProgressBar loadingIndicator, resultsLoadingIndicator;
    private Button tryAgainButton;


    // --- Data & Threading ---
    private final List<AppInfo> allApps = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastSelectedAppName;

    private static class AlternativeResult {
        final List<AlternativeApp> alternatives;
        final boolean wasOnlineSearch;

        AlternativeResult(List<AlternativeApp> alternatives, boolean wasOnlineSearch) {
            this.alternatives = alternatives;
            this.wasOnlineSearch = wasOnlineSearch;
        }
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
        executor.shutdown();
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
        resultsTitle = findViewById(R.id.resultsTitle);

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
            if (lastSelectedAppName != null) {
                findAndDisplayAlternatives(lastSelectedAppName);
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
            lastSelectedAppName = app.getAppName();
            findAndDisplayAlternatives(app.getAppName());
            dialog.dismiss();
        });
        recyclerView.setAdapter(dialogAdapter);
        dialog.show();
    }

    private void findAndDisplayAlternatives(String appName) {
        selectionLayout.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        resultsLoadingIndicator.setVisibility(View.VISIBLE);
        alternativesContainer.removeAllViews();
        errorLayout.setVisibility(View.GONE);
        resultsTitle.setText("Finding alternatives for " + appName);


        executor.execute(() -> {
            // --- THIS IS THE PERFECTED HYBRID LOGIC ---
            // First, try to find alternatives online
            AlternativeResult result = findAlternativesOnline(appName);

            // If the online search fails or finds nothing, try the offline list
            if (result.alternatives == null || result.alternatives.isEmpty()) {
                result = new AlternativeResult(getSimulatedAlternatives(appName), false);
            }
            // --- END OF HYBRID LOGIC ---

            AlternativeResult finalResult = result;
            handler.post(() -> {
                resultsLoadingIndicator.setVisibility(View.GONE);
                if (finalResult.alternatives.isEmpty()) {
                    String message = "No similar alternatives found for \"" + appName + "\".";
                    showErrorMessage(message, false);
                } else {
                    displayAlternativeList(finalResult.alternatives);
                }
            });
        });
    }

    private void displayAlternativeList(List<AlternativeApp> alternatives) {
        alternativesContainer.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        for (AlternativeApp alt : alternatives) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.list_item_alternative, alternativesContainer, false);
            ImageView icon = itemView.findViewById(R.id.alternativeAppIcon);
            TextView name = itemView.findViewById(R.id.alternativeAppName);

            // If it's an online result with an icon URL, load it. Otherwise, use a placeholder.
            if (alt.getIconUrl() != null && !alt.getIconUrl().isEmpty()) {
                loadIconFromUrl(icon, alt.getIconUrl());
            } else {
                icon.setImageResource(R.mipmap.ic_launcher_round);
            }
            name.setText(alt.getAppName());

            itemView.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + alt.getPackageName())));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + alt.getPackageName())));
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
            handler.post(() -> imageView.setImageResource(R.mipmap.ic_launcher_round));
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
                handler.post(() -> imageView.setImageResource(R.mipmap.ic_launcher_round));
            }
        });
    }

    private AlternativeResult findAlternativesOnline(String originalAppName) {
        if (!isNetworkAvailable()) {
            return new AlternativeResult(null, true); // Indicate online search failed
        }

        List<AlternativeApp> alternatives = new ArrayList<>();
        String query = originalAppName;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("https://f-droid.org/api/v1/search?q=" + URLEncoder.encode(query, "UTF-8"));
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setConnectTimeout(10000); // Shorter timeout
            urlConnection.setReadTimeout(10000);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return new AlternativeResult(null, true);
            }

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
                    if(app.getJSONObject("name").getString("en-US").equalsIgnoreCase(originalAppName)) continue;

                    String name = app.getJSONObject("name").getString("en-US");
                    String packageName = app.getString("packageName");
                    String iconUrl = app.optString("icon");
                    alternatives.add(new AlternativeApp(name, packageName, iconUrl));
                }
            }
            return new AlternativeResult(alternatives, true);
        } catch (Exception e) {
            Log.e("AlternativeActivity", "Error fetching alternatives", e);
            return new AlternativeResult(null, true); // Indicate online search failed
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private List<AlternativeApp> getSimulatedAlternatives(String originalAppName) {
        ArrayList<AlternativeApp> alternatives = new ArrayList<>();
        String lowerCaseName = originalAppName.toLowerCase();

        if (lowerCaseName.contains("whatsapp") || lowerCaseName.contains("messenger")) {
            alternatives.add(new AlternativeApp("Signal Messenger", "org.thoughtcrime.securesms", null));
            alternatives.add(new AlternativeApp("Telegram", "org.telegram.messenger", null));
        } else if (lowerCaseName.contains("chrome") || lowerCaseName.contains("browser")) {
            alternatives.add(new AlternativeApp("Brave Private Browser", "com.brave.browser", null));
            alternatives.add(new AlternativeApp("DuckDuckGo Private Browser", "com.duckduckgo.mobile.android", null));
        } else if (lowerCaseName.contains("instagram")) {
            alternatives.add(new AlternativeApp("Pixelfed", "de.pixelfed.app", null));
        }
        return alternatives;
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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

