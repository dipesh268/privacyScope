package com.example.privacyscope;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppDialogAdapter extends RecyclerView.Adapter<AppDialogAdapter.AppViewHolder> {

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
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_dialog_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.bind(app, listener);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.dialogAppIcon);
            appName = itemView.findViewById(R.id.dialogAppName);
        }

        void bind(final AppInfo app, final OnAppSelectedListener listener) {
            appIcon.setImageDrawable(app.getIcon());
            appName.setText(app.getAppName());
            itemView.setOnClickListener(v -> listener.onAppSelected(app));
        }
    }
}
