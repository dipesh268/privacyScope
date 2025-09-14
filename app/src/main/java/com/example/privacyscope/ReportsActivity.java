package com.example.privacyscope;

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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

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
    private String reportContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        reportContentTextView = findViewById(R.id.reportContentTextView);
        shareTextButton = findViewById(R.id.shareTextButton);
        sharePdfButton = findViewById(R.id.sharePdfButton);

        setupBottomNavigation();
        loadAndGenerateReport();

        shareTextButton.setOnClickListener(v -> shareReportAsText());
        sharePdfButton.setOnClickListener(v -> shareReportAsPdf());
    }

    private void loadAndGenerateReport() {
        reportContentTextView.setText("Generating report, please wait...");
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS);
            List<AppInfo> allApps = new ArrayList<>();

            for (PackageInfo packageInfo : packages) {
                if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    allApps.add(new AppInfo(packageInfo, pm));
                }
            }
            Collections.sort(allApps, (a1, a2) -> Integer.compare(a2.getRiskScore(), a1.getRiskScore())); // Sort by risk score descending

            StringBuilder sb = new StringBuilder();
            sb.append("PrivacyScope Security Report\n");
            sb.append("Generated on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            sb.append("====================================\n\n");

            for (AppInfo app : allApps) {
                sb.append("App: ").append(app.getAppName()).append("\n");
                sb.append("Risk Score: ").append(app.getRiskScore()).append(" (").append(app.getRiskLevel()).append(")\n");
                sb.append("Permissions: ").append(app.getDangerousPermissions().isEmpty() ? "None" : app.getDangerousPermissions()).append("\n");
                sb.append("Trackers: ").append(app.getDetectedTrackers().isEmpty() ? "None" : app.getDetectedTrackers()).append("\n");
                sb.append("------------------------------------\n");
            }

            reportContent = sb.toString();
            runOnUiThread(() -> reportContentTextView.setText(reportContent));
        }).start();
    }

    private void shareReportAsText() {
        if (reportContent == null || reportContent.isEmpty()) {
            Toast.makeText(this, "Report is not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, reportContent);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, "Share Report Via"));
    }

    private void shareReportAsPdf() {
        if (reportContent == null || reportContent.isEmpty()) {
            Toast.makeText(this, "Report is not ready yet.", Toast.LENGTH_SHORT).show();
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
            return false;
        });
    }
}

