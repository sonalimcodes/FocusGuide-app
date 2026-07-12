package com.sonalim.focusguide;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Set;

public class FocusAccessibilityService extends AccessibilityService {

    private static final String TAG = "FocusAccessService";
    private long lastLaunch = 0;
    private static final long MIN_GAP = 1200; // avoid double triggers within 1.2 sec

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        String pkg = (event.getPackageName() == null) ? "" : event.getPackageName().toString();
        if (pkg.isEmpty()) return;

        long now = System.currentTimeMillis();

        // ---------------------------------------------------------
        // 🔥 GLOBAL EMERGENCY EXIT — allow ALL APPS for 10 minutes
        // ---------------------------------------------------------
        long emergencyUntil = Utils.getEmergencyUntil(this);
        if (now < emergencyUntil) {
            Log.d(TAG, "GLOBAL EMERGENCY EXIT ACTIVE — Apps allowed");
            return;
        }

        // ---------------------------------------------------------
        // Focus Mode must be ON
        // ---------------------------------------------------------
        boolean running = Utils.prefs(this).getBoolean(Utils.PREF_FOCUS_RUNNING, false);
        if (!running) return;

        // ---------------------------------------------------------
        // Check Focus Mode end time
        // ---------------------------------------------------------
        long focusEnd = Utils.prefs(this).getLong("focus_end", 0);
        if (now >= focusEnd) {
            Log.i(TAG, "Focus Mode ended — stopping blocking");
            Utils.prefs(this).edit().putBoolean(Utils.PREF_FOCUS_RUNNING, false).apply();
            return;
        }

        // ---------------------------------------------------------
        // App must be in Blocked List
        // ---------------------------------------------------------
        Set<String> blocked = Utils.getBlockedSet(this);
        if (!blocked.contains(pkg)) return;

        // ---------------------------------------------------------
        // ⏳ PER-APP TEMPORARY UNLOCK (from challenge)
        // ---------------------------------------------------------
        long unlockUntil = Utils.getUnlockUntil(this, pkg);
        if (now < unlockUntil) {
            Log.d(TAG, "Temporary unlock active for: " + pkg);
            return;
        }

        // ---------------------------------------------------------
        // 🔔 SEND PARENT ALERT ONLY ON FIRST ATTEMPT
        // ---------------------------------------------------------
        String parentPhone = Utils.prefs(this).getString("parent_phone", "");
        boolean alertSent = Utils.isAlertSent(this, pkg);

        if (!alertSent && !parentPhone.isEmpty()) {
            String appName = getAppName(pkg);
            String message = "⚠ ALERT: Your child is trying to open " + appName;

            try {
                Utils.sendSMS(this, parentPhone, message);
                Utils.markAlertSent(this, pkg);
                Log.i(TAG, "Parent notified for: " + appName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send alert SMS", e);
            }

            launchChallenge(pkg);
            return;
        }

        // ---------------------------------------------------------
        // CHALLENGE LOGIC
        // ---------------------------------------------------------

        long now2 = System.currentTimeMillis();
        if (now2 - lastLaunch < MIN_GAP) return; // prevent double pop
        lastLaunch = now2;

        launchChallenge(pkg);
    }

    // ---------------------------------------------------------
    // 🚀 Launch challenge screen
    // ---------------------------------------------------------
    void launchChallenge(String pkg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Intent i = new Intent(this, ChallengeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("pkg", pkg);
                startActivity(i);
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch challenge", e);
            }
        });
    }

    // ---------------------------------------------------------
    // 📛 Get human-friendly app name
    // ---------------------------------------------------------
    private String getAppName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    @Override
    public void onInterrupt() {}
}

