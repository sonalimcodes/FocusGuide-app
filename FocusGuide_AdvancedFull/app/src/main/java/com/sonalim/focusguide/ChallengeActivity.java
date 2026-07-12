package com.sonalim.focusguide;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

public class ChallengeActivity extends Activity {

    private static final String TAG = "ChallengeActivity";

    TextView tvQuestion, tvTimer;
    EditText etAnswer;
    Button btnSubmit, btnGiveUp, btnExit;

    String targetPkg;
    String correctAnswer;
    CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // make sure layout exists and has expected IDs (see sample layout below)
        setContentView(R.layout.activity_challenge);

        tvQuestion = findViewById(R.id.tvQuestion);
        tvTimer = findViewById(R.id.tvTimer);
        etAnswer = findViewById(R.id.etAnswer);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnGiveUp = findViewById(R.id.btnGiveUp);
        btnExit = findViewById(R.id.btnExit); // may be null if layout missing — handled below

        targetPkg = getIntent().getStringExtra("pkg");
        if (targetPkg == null) targetPkg = "unknown";

        prepareQuestion();
        startTimer();

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> checkAnswer());
        }

        if (btnGiveUp != null) {
            btnGiveUp.setOnClickListener(v -> onFailed());
        }

        // EMERGENCY EXIT: allow globally for 10 minutes
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                long until = now + 10 * 60L * 1000L; // 10 minutes
                Utils.setEmergencyUntil(ChallengeActivity.this, until);
                Toast.makeText(ChallengeActivity.this,
                        "Emergency Exit enabled for 10 minutes",
                        Toast.LENGTH_LONG).show();
                finish();
            });
        } else {
            // if layout doesn't provide exit, log but do not crash
            Log.w(TAG, "btnExit not found in layout; Emergency Exit button disabled.");
        }
    }

    // ---------------- COMPLEX CHALLENGES ----------------
    void prepareQuestion() {
        Random r = new Random();
        int type = r.nextInt(5);

        switch (type) {
            case 0: {
                int a = r.nextInt(10) + 2;
                int b = r.nextInt(10) + 2;
                int c = r.nextInt(10) + 1;
                tvQuestion.setText(String.format(Locale.US, "Solve: (%d × %d) + %d", a, b, c));
                correctAnswer = String.valueOf((a * b) + c);
                break;
            }

            case 1: {
                int start = r.nextInt(10);
                tvQuestion.setText("Next number: " + start + ", " + (start+3)
                        + ", " + (start+6) + ", ?");
                correctAnswer = String.valueOf(start + 9);
                break;
            }

            case 2: {
                String word = "Focusing";
                tvQuestion.setText("Reverse this word: " + word);
                correctAnswer = new StringBuilder(word).reverse().toString();
                break;
            }

            case 3: {
                String text = "Concentration";
                tvQuestion.setText("How many vowels in: " + text);
                correctAnswer = String.valueOf(countVowels(text));
                break;
            }

            default: {
                int a = r.nextInt(10) + 2;
                int b = r.nextInt(10) + 2;
                int c = r.nextInt(5) + 2;
                tvQuestion.setText(String.format(Locale.US, "Solve: (%d + %d) × %d", a, b, c));
                correctAnswer = String.valueOf((a + b) * c);
                break;
            }
        }
    }

    int countVowels(String s) {
        int c = 0;
        s = s.toLowerCase();
        for(char ch : s.toCharArray())
            if("aeiou".indexOf(ch) >= 0) c++;
        return c;
    }

    // ---------------- TIMER ----------------
    void startTimer() {
        if (tvTimer != null) tvTimer.setText("Time: 30s");
        timer = new CountDownTimer(30_000, 1000) {
            public void onTick(long ms) {
                if (tvTimer != null) tvTimer.setText("Time: " + (ms/1000) + "s");
            }
            public void onFinish() {
                if (tvTimer != null) tvTimer.setText("Time: 0s");
                onFailed();
            }
        }.start();
    }

    // ---------------- CHECK ANSWER ----------------
    void checkAnswer() {
        if (etAnswer == null) {
            Toast.makeText(this, "Answer field missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String a = etAnswer.getText().toString().trim();

        if(TextUtils.isEmpty(a)) {
            Toast.makeText(this, "Enter answer", Toast.LENGTH_SHORT).show();
            return;
        }

        if(a.equalsIgnoreCase(correctAnswer)) onSolved();
        else onFailed();
    }

    // ---------------- SOLVED ----------------
    void onSolved() {
        if(timer != null) timer.cancel();

        // mark solved so the service won't challenge again this session
        Utils.prefs(this).edit().putBoolean("solved_" + targetPkg, true).apply();

        // clear fail counter for this app
        Utils.prefs(this).edit().putInt("fails_" + targetPkg, 0).apply();

        // clear parent alert flag so next session it will notify again
        Utils.resetAlert(this, targetPkg);

        // temporary per-app unlock for 5 minutes
        long until = System.currentTimeMillis() + 5 * 60L * 1000L;
        Utils.setUnlockUntil(this, targetPkg, until);

        new AlertDialog.Builder(this)
                .setTitle("Correct!")
                .setMessage("You solved the challenge. App unlocked for 5 minutes. Continue?")
                .setPositiveButton("Yes", (d,w) -> finish())
                .setNegativeButton("No", (d,w) -> finish())
                .setCancelable(false)
                .show();
    }

    // ---------------- FAILED ----------------
    void onFailed() {
        if(timer != null) timer.cancel();

        String key = "fails_" + targetPkg;
        int fails = Utils.prefs(this).getInt(key, 0) + 1;
        Utils.prefs(this).edit().putInt(key, fails).apply();

        if(fails >= 3){
            // send parent alert right away (if phone present) - Utils.sendSMS has fallback
            String phone = Utils.prefs(this).getString("parent_phone", "");
            if(!phone.isEmpty()){
                Utils.sendSMS(this, phone, "FocusGuide Alert: Child failed 3 challenges for " + getAppName());
            }

            Toast.makeText(this, "3 failed attempts. App locked.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Wrong ("+fails+"/3)", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getAppName(){
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(targetPkg, 0)).toString();
        } catch (Exception e) {
            return targetPkg;
        }
    }

    @Override
    protected void onDestroy() {
        if(timer != null) timer.cancel();
        super.onDestroy();
    }
}
