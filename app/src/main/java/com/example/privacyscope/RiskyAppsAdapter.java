package com.example.privacyscope;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

public class RiskyAppsAdapter extends RecyclerView.Adapter<RiskyAppsAdapter.ViewHolder> {

    private final List<AppInfo> riskyApps;
    private final Context context;

    public RiskyAppsAdapter(List<AppInfo> riskyApps, Context context) {
        this.riskyApps = riskyApps;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_risky_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = riskyApps.get(position);

        holder.appIcon.setImageDrawable(app.getIcon());
        holder.appName.setText(app.getAppName());
        holder.riskScore.setText("Risk Score: " + app.getRiskScore());

        AppInfo.RiskLevel level = app.getRiskLevel();
        holder.riskChip.setText(level.toString());

        int chipColorRes;
        switch (level) {
            case HIGH:
                chipColorRes = R.color.risk_high;
                break;
            case MEDIUM:
                chipColorRes = R.color.risk_medium;
                break;
            default: // LOW
                chipColorRes = R.color.risk_low;
                break;
        }

        // --- THIS IS THE FIX ---
        // This is a more robust way to set the background color that respects themes.
        holder.riskChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, chipColorRes)));
        // --- END OF FIX ---


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AppDetailActivity.class);
            intent.putExtra("PACKAGE_NAME", app.getPackageName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return riskyApps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView riskScore;
        Chip riskChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            riskScore = itemView.findViewById(R.id.riskScore);
            riskChip = itemView.findViewById(R.id.riskChip);
        }
    }
}

