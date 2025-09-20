package com.example.privacyscope;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private TextView reportContentTextView;
    private Button shareTextButton, sharePdfButton;
    private ChipGroup reportTypeChipGroup;
    private MaterialCardView selectAppCard;
    private ImageView selectedAppIcon;
    private TextView selectedAppName;
    private ProgressBar loadingIndicator;


    private String reportContent;
    private final List<AppInfo> allApps = new ArrayList<>();
    private AppInfo selectedAppInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        initializeViews();
        setupListeners();
        setupBottomNavigation();

        // Start loading the app list. The report will be generated
        // automatically when this process completes.
        loadAllApps();
    }

    private void initializeViews() {
        reportContentTextView = findViewById(R.id.reportContentTextView);
        shareTextButton = findViewById(R.id.shareTextButton);
        sharePdfButton = findViewById(R.id.sharePdfButton);
        reportTypeChipGroup = findViewById(R.id.reportTypeChipGroup);
        selectAppCard = findViewById(R.id.selectAppCard);
        selectedAppIcon = findViewById(R.id.selectedAppIcon);
        selectedAppName = findViewById(R.id.selectedAppName);
        loadingIndicator = findViewById(R.id.loadingIndicator);
    }

    private void setupListeners() {
        reportTypeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipAllApps)) {
                selectAppCard.setVisibility(View.GONE);
                loadingIndicator.setVisibility(View.GONE);
                generateAllAppsReport();
            } else if (checkedIds.contains(R.id.chipSingleApp)) {
                selectAppCard.setVisibility(View.VISIBLE);
                if (selectedAppInfo != null) {
                    generateSingleAppReport(selectedAppInfo);
                } else {
                    reportContentTextView.setText("Please select an app to generate a detailed report.");
                    reportContent = "";
                }
            }
        });

        selectAppCard.setOnClickListener(v -> showAppSelectionDialog());
        shareTextButton.setOnClickListener(v -> shareReportAsText());
        sharePdfButton.setOnClickListener(v -> shareReportAsPdf());
    }

    private void loadAllApps() {
        selectAppCard.setEnabled(false);
        loadingIndicator.setVisibility(View.VISIBLE);

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

            // --- THIS IS THE FIX ---
            // Now that apps are loaded, generate the initial report and enable UI.
            runOnUiThread(() -> {
                loadingIndicator.setVisibility(View.GONE);
                selectAppCard.setEnabled(true);
                generateAllAppsReport(); // Generate the report *after* loading is complete.
            });
            // --- END OF FIX ---
        }).start();
    }


    private void generateAllAppsReport() {
        reportContentTextView.setText("Generating summary report...");
        new Thread(() -> {
            // Check if allApps is still being loaded
            if (allApps.isEmpty() && loadingIndicator.getVisibility() == View.VISIBLE) {
                runOnUiThread(() -> reportContentTextView.setText("Loading app data, please wait..."));
                return;
            }

            List<AppInfo> sortedApps = new ArrayList<>(allApps);
            Collections.sort(sortedApps, (a1, a2) -> Integer.compare(a2.getRiskScore(), a1.getRiskScore()));

            StringBuilder sb = new StringBuilder();
            sb.append("PrivacyScope - All Apps Summary Report\n");
            sb.append("Generated on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            sb.append("====================================\n\n");

            for (AppInfo app : sortedApps) {
                sb.append("App: ").append(app.getAppName()).append("\n");
                sb.append("  - Risk Score: ").append(app.getRiskScore()).append(" (").append(app.getRiskLevel()).append(")\n");
                sb.append("  - Permissions: ").append(app.getDangerousPermissions().size()).append("\n");
                sb.append("  - Trackers: ").append(app.getDetectedTrackers().size()).append("\n\n");
            }

            reportContent = sb.toString();
            runOnUiThread(() -> reportContentTextView.setText(reportContent));
        }).start();
    }

    private void generateSingleAppReport(AppInfo app) {
        StringBuilder sb = new StringBuilder();
        sb.append("PrivacyScope - Single App Detailed Report\n");
        sb.append("====================================\n\n");
        sb.append("App Name: ").append(app.getAppName()).append("\n");
        sb.append("Package: ").append(app.getPackageName()).append("\n");
        sb.append("Risk Score: ").append(app.getRiskScore()).append(" (").append(app.getRiskLevel()).append(")\n\n");

        sb.append("--- Dangerous Permissions (").append(app.getDangerousPermissions().size()).append(") ---\n");
        if (app.getDangerousPermissions().isEmpty()) {
            sb.append("None\n");
        } else {
            for (String perm : app.getDangerousPermissions()) {
                sb.append("- ").append(perm.substring(perm.lastIndexOf('.') + 1)).append("\n");
            }
        }
        sb.append("\n");

        sb.append("--- Trackers Detected (").append(app.getDetectedTrackers().size()).append(") ---\n");
        if (app.getDetectedTrackers().isEmpty()) {
            sb.append("None\n");
        } else {
            for (String tracker : app.getDetectedTrackers()) {
                sb.append("- ").append(tracker).append("\n");
            }
        }
        sb.append("\n");

        reportContent = sb.toString();
        reportContentTextView.setText(reportContent);
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
            selectedAppInfo = app;
            selectedAppName.setText(app.getAppName());
            selectedAppIcon.setImageDrawable(app.getIcon());
            selectedAppIcon.setImageTintList(null);
            generateSingleAppReport(app);
            dialog.dismiss();
        });
        recyclerView.setAdapter(dialogAdapter);
        dialog.show();
    }


    private void shareReportAsText() {
        if (reportContent == null || reportContent.isEmpty() || reportContent.startsWith("Please select")) {
            Toast.makeText(this, "Please generate a report first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, reportContent);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, "Share Report Via"));
    }

    private void shareReportAsPdf() {
        if (reportContent == null || reportContent.isEmpty() || reportContent.startsWith("Please select")) {
            Toast.makeText(this, "Please generate a report first.", Toast.LENGTH_SHORT).show();
            return;
        }
        File pdfFile = createPdfFromString(reportContent);
        if (pdfFile != null) {
            Uri pdfUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF Report Via"));
        }
    }

    private File createPdfFromString(String text) {
        File pdfPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String fileName = "PrivacyScope_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File file = new File(pdfPath, fileName);

        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 page size
            PdfDocument.Page page = document.startPage(pageInfo);

            TextPaint textPaint = new TextPaint();
            textPaint.setTextSize(10);
            StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, page.getCanvas().getWidth() - 40)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build();

            page.getCanvas().save();
            page.getCanvas().translate(20, 20); // Margins
            staticLayout.draw(page.getCanvas());
            page.getCanvas().restore();

            document.finishPage(page);
            document.writeTo(new FileOutputStream(file));
            document.close();
            Toast.makeText(this, "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_reports);
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
                startActivity(new Intent(getApplicationContext(), ComparisonActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_reports) {
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

