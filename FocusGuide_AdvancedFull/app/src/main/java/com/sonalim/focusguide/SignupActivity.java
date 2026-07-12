package com.sonalim.focusguide;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignupActivity extends Activity {

    EditText etName, etAge, etEmail, etPassword, etParentPhone;
    Button btnSignup;
    TextView tvPin;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etParentPhone = findViewById(R.id.etParentPhone);
        btnSignup = findViewById(R.id.btnSignup);
        tvPin = findViewById(R.id.tvGeneratedPin);

        btnSignup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String name = etName.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String pw = etPassword.getText().toString().trim();
                String parentPhone = etParentPhone.getText().toString().trim();

                if(TextUtils.isEmpty(name) || TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pw)){
                    Toast.makeText(SignupActivity.this, "Fill required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                int age = Integer.parseInt(ageStr);

                if(age < 18 && TextUtils.isEmpty(parentPhone)){
                    Toast.makeText(SignupActivity.this, "Parent phone required for age < 18", Toast.LENGTH_SHORT).show();
                    return;
                }

                Utils.prefs(SignupActivity.this).edit()
                    .putString(Utils.PREF_USER_EMAIL, email)
                    .putString(Utils.PREF_USER_PASSWORD, pw)
                    .putString("parent_phone", parentPhone)
                    .putInt("age", age)
                    .apply();

                String pin = Utils.generatePIN();
                Utils.prefs(SignupActivity.this).edit().putString(Utils.PREF_PIN, pin).apply();

                tvPin.setText("Account created. Your PIN: " + pin);
                Toast.makeText(SignupActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
