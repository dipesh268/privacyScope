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

public class RiskyAppsAdapter extends RecyclerView.Adapter<RiskyAppsAdapter.ViewHolder> {

    private final List<AppInfo> appList;
    private final Context context;

    public RiskyAppsAdapter(List<AppInfo> appList, Context context) {
        this.appList = appList;
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
        AppInfo app = appList.get(position);

        holder.appName.setText(app.getAppName());
        holder.riskScore.setText("Risk Score: " + app.getRiskScore());
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.riskChip.setText(app.getRiskLevel().name());

        // Set listener to open the detail activity, passing the package name
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AppDetailActivity.class);
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
        TextView appName, riskScore;
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

