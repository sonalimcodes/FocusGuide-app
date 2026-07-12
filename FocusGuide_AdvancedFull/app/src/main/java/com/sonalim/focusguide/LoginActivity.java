package com.sonalim.focusguide;

import android.app.Activity;        // ← missing import (required)
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
public class LoginActivity extends Activity {


    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);   // ← correct ID

        tvSignup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String e = etEmail.getText().toString().trim();
                String p = etPassword.getText().toString().trim();

                String savedE = Utils.prefs(LoginActivity.this).getString(Utils.PREF_USER_EMAIL, null);
                String savedP = Utils.prefs(LoginActivity.this).getString(Utils.PREF_USER_PASSWORD, null);

                if(TextUtils.isEmpty(e) || TextUtils.isEmpty(p)){
                    Toast.makeText(LoginActivity.this, "Enter credentials", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(savedE != null && savedE.equals(e) && savedP != null && savedP.equals(p)){
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
