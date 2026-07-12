package com.sonalim.focusguide;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListAdapter extends BaseAdapter {

    Context ctx;
    List<AppInfo> apps = new ArrayList<>();
    Set<String> blocked;

    public AppListAdapter(Context c, List<AppInfo> apps, Set<String> blocked){
        this.ctx = c;
        this.apps = apps;
        this.blocked = new HashSet<>(blocked);
    }

    @Override
    public int getCount(){ return apps.size(); }

    @Override
    public Object getItem(int i){ return apps.get(i); }

    @Override
    public long getItemId(int i){ return i; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        if(convertView == null){
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_app, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.ivIcon);
        TextView tv = convertView.findViewById(R.id.tvAppName);
        CheckBox cb = convertView.findViewById(R.id.cbBlock);

        AppInfo ai = apps.get(position);
        tv.setText(ai.label);

        // 🔹 Load app icon
        try {
            PackageManager pm = ctx.getPackageManager();
            Drawable d = pm.getApplicationIcon(ai.packageName);
            icon.setImageDrawable(d);
        } catch (Exception e) {
            // 🔹 Safe fallback icon — NO drawable file required
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // 🔹 Set checkbox state based on blocked apps
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(blocked.contains(ai.packageName));

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) blocked.add(ai.packageName);
            else blocked.remove(ai.packageName);
            Utils.saveBlockedSet(ctx, blocked);
        });

        return convertView;
    }
}

class AppInfo {
    String label, packageName;
    AppInfo(String l, String p){
        label = l;
        packageName = p;
    }
}
