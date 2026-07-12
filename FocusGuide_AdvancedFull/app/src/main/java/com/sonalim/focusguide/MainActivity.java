package com.sonalim.focusguide;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    TextView tvStatus;
    Button btnStart, btnAccessibility, btnEmergency;
    EditText etMinutes;
    ListView lvApps;
    AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStartFocus);
        btnAccessibility = findViewById(R.id.btnAccessibilitySettings);
        btnEmergency = findViewById(R.id.btnEmergency);
        etMinutes = findViewById(R.id.etMinutes);
        lvApps = findViewById(R.id.lvApps);

        requestSmsPermissionIfNeeded();

        // Load blocked set
        Set<String> blocked = Utils.getBlockedSet(this);

        // Load all launchable apps including WhatsApp, Instagram, Snapchat
        List<ApplicationInfo> allApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();

        for(ApplicationInfo ai : allApps){
            if(pm.getLaunchIntentForPackage(ai.packageName) != null){
                if(ai.packageName.equals(getPackageName())) continue; // skip our own app
                String label = pm.getApplicationLabel(ai).toString();
                apps.add(new AppInfo(label, ai.packageName));
            }
        }

        adapter = new AppListAdapter(this, apps, blocked);
        lvApps.setAdapter(adapter);

        btnStart.setOnClickListener(v -> {
            String minStr = etMinutes.getText().toString().trim();
            if(minStr.isEmpty()){
                Toast.makeText(MainActivity.this, "Enter duration in minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            int minutes;
            try { minutes = Integer.parseInt(minStr); }
            catch (Exception e){ minutes = 0; }

            if(minutes <= 0){
                Toast.makeText(MainActivity.this, "Enter a valid duration", Toast.LENGTH_SHORT).show();
                return;
            }

            long now = System.currentTimeMillis();
            long end = now + (minutes * 60L * 1000L);

            Utils.prefs(this).edit()
                    .putBoolean(Utils.PREF_FOCUS_RUNNING, true)
                    .putLong("focus_end", end)
                    .apply();

            Toast.makeText(this,
                    "Focus Mode started for " + minutes + " minutes.\nEnable Accessibility Service.",
                    Toast.LENGTH_LONG).show();

            tvStatus.setText("Status: Focus Mode ON for " + minutes + " min");
        });

        btnAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnEmergency.setOnClickListener(v -> {
            long last = Utils.prefs(MainActivity.this).getLong(Utils.PREF_EMERGENCY_TS, 0);
            long now = System.currentTimeMillis();

            if(now - last < 24L*60*60*1000){
                Toast.makeText(this,
                        "Emergency Exit already used in the last 24 hours",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            long until = now + 10*60*1000;
            Utils.prefs(MainActivity.this).edit()
                    .putLong("emergency_until", until)
                    .putLong(Utils.PREF_EMERGENCY_TS, now)
                    .apply();

            Toast.makeText(this,
                    "Emergency Exit allowed for 10 minutes",
                    Toast.LENGTH_SHORT).show();
        });

        // If service is not enabled, show alert
        if(!isAccessibilityServiceEnabled()){
            new AlertDialog.Builder(this)
                    .setTitle("Accessibility Required")
                    .setMessage("Please enable FocusGuide Accessibility Service for app blocking to work.")
                    .setPositiveButton("Open Settings", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private boolean isAccessibilityServiceEnabled(){
        ComponentName cn = new ComponentName(this, FocusAccessibilityService.class);
        String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if(enabled == null) return false;
        return enabled.toLowerCase().contains(cn.flattenToString().toLowerCase());
    }

    private void requestSmsPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED){

                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 101);
            }
        }
    }
}
