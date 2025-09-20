package com.example.privacyscope;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppDialogAdapter extends RecyclerView.Adapter<AppDialogAdapter.ViewHolder> {

    private final List<AppInfo> appList;
    private final OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo app);
    }

    public AppDialogAdapter(List<AppInfo> appList, OnAppSelectedListener listener) {
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_dialog_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.appName.setText(app.getAppName());
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.itemView.setOnClickListener(v -> listener.onAppSelected(app));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.dialogAppIcon);
            appName = itemView.findViewById(R.id.dialogAppName);
        }
    }
}

