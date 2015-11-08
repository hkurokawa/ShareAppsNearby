package com.hkurokawa.shareappsnearby;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.ViewHolder> {
    private LayoutInflater inflater;
    private List<AppInfo> apps = new ArrayList<>();

    public AppsListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setApps(List<AppInfo> apps) {
        this.apps.clear();
        this.apps.addAll(apps);
        this.notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final AppInfo info = apps.get(position);
        holder.labelView.setText(info.getLabel());
        holder.packageNameView.setText(info.getPackageName());
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.icon)
        ImageView iconView;
        @Bind(R.id.label)
        TextView labelView;
        @Bind(R.id.package_name)
        TextView packageNameView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
