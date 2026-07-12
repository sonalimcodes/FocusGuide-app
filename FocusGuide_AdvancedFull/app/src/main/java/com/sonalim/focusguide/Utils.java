package com.sonalim.focusguide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Utils {

    private static final String TAG = "Utils";

    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_PASSWORD = "user_password";
    public static final String PREF_PIN = "user_pin";
    public static final String PREF_BLOCKED = "blocked_apps";
    public static final String PREF_FOCUS_RUNNING = "focus_running";
    public static final String PREF_EMERGENCY_TS = "emergency_ts";

    // -----------------------------------------
    // Shared Preferences Access
    // -----------------------------------------
    public static SharedPreferences prefs(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    // -----------------------------------------
    // Generate Random PIN (if used)
    // -----------------------------------------
    public static String generatePIN() {
        Random r = new Random();
        int p = 1000 + r.nextInt(9000);
        return String.valueOf(p);
    }

    // -----------------------------------------
    // Blocked Apps Set
    // -----------------------------------------
    public static Set<String> getBlockedSet(Context c) {
        Set<String> s = prefs(c).getStringSet(PREF_BLOCKED, null);
        if (s == null) return new HashSet<>();
        return new HashSet<>(s); // return safe copy
    }

    public static void saveBlockedSet(Context c, Set<String> s) {
        if (s == null) s = new HashSet<>();
        prefs(c).edit().putStringSet(PREF_BLOCKED, new HashSet<>(s)).apply();
    }

    // -----------------------------------------
    // SEND SMS or Email Fallback
    // -----------------------------------------
    public static void sendSMS(Context ctx, String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "sendSMS: Parent phone number empty");
            return;
        }

        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, null, null);
            Log.i(TAG, "SMS sent to " + phoneNumber);

        } catch (Exception e) {
            Log.e(TAG, "SMS failed, fallback to email: " + e.getMessage());

            try {
                Intent email = new Intent(Intent.ACTION_SENDTO);
                String parentEmail = prefs(ctx).getString(PREF_USER_EMAIL, "");

                if (parentEmail == null || parentEmail.trim().isEmpty()) {
                    email.setData(Uri.parse("mailto:"));
                } else {
                    email.setData(Uri.parse("mailto:" + parentEmail));
                }

                email.putExtra(Intent.EXTRA_SUBJECT, "FocusGuide Alert");
                email.putExtra(Intent.EXTRA_TEXT, message);
                email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                ctx.startActivity(email);

            } catch (Exception ex) {
                Log.e(TAG, "Email fallback also failed: " + ex.getMessage());
            }
        }
    }

    // ===============================================================
    //                 🔥 EMERGENCY EXIT (GLOBAL)
    // ===============================================================

    public static long getEmergencyUntil(Context c) {
        return prefs(c).getLong("emergency_until", 0);
    }

    public static void setEmergencyUntil(Context c, long until) {
        prefs(c).edit().putLong("emergency_until", until).apply();
    }

    // ===============================================================
    //         🔓 PER-APP TEMPORARY UNLOCK (from challenge)
    // ===============================================================

    public static long getUnlockUntil(Context c, String pkg) {
        return prefs(c).getLong("unlock_until_" + pkg, 0);
    }

    public static void setUnlockUntil(Context c, String pkg, long until) {
        prefs(c).edit().putLong("unlock_until_" + pkg, until).apply();
    }

    public static void clearUnlock(Context c, String pkg) {
        prefs(c).edit().remove("unlock_until_" + pkg).apply();
    }

    // ===============================================================
    //               📩 PARENT ALERT TRACKING
    // ===============================================================

    public static boolean isAlertSent(Context c, String pkg) {
        return prefs(c).getBoolean("alert_sent_" + pkg, false);
    }

    public static void markAlertSent(Context c, String pkg) {
        prefs(c).edit().putBoolean("alert_sent_" + pkg, true).apply();
    }

    public static void resetAlert(Context c, String pkg) {
        prefs(c).edit().remove("alert_sent_" + pkg).apply();
    }
}
