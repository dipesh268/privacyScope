package com.example.privacyscope;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final List<AppInfo> appList;
    private final Context context;

    public AppListAdapter(List<AppInfo> appList, Context context) {
        this.appList = appList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appList.get(position);

        holder.appName.setText(app.getAppName());
        holder.appPackageName.setText(app.getPackageName());
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.riskChip.setText(app.getRiskLevel().name());

        switch (app.getRiskLevel()) {
            case HIGH:
                holder.riskChip.setChipBackgroundColorResource(R.color.risk_high);
                break;
            case MEDIUM:
                holder.riskChip.setChipBackgroundColorResource(R.color.risk_medium);
                break;
            case LOW:
                holder.riskChip.setChipBackgroundColorResource(R.color.risk_low);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AppDetailActivity.class);
            // FIX: Pass only the package name, not the whole object.
            intent.putExtra("PACKAGE_NAME", app.getPackageName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName, appPackageName;
        Chip riskChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // This ensures all UI components are correctly linked from the XML.
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appPackageName = itemView.findViewById(R.id.appPackageName);
            riskChip = itemView.findViewById(R.id.riskChip);

            // NEW: Add null checks to provide a clearer error message if something is wrong with the layout file.
            if (appIcon == null || appName == null || appPackageName == null || riskChip == null) {
                throw new IllegalStateException("A required view is missing from the list_item_app.xml layout file. " +
                        "Please ensure all views (appIcon, appName, appPackageName, riskChip) have the correct IDs.");
            }
        }
    }
}

