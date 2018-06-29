package com.sample.smsbackup.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.sample.smsbackup.utilities.Constants;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i;



        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);

        if (settings.getBoolean(Constants.PREFS_FIRST_LUNCH, true)) {
            i = new Intent(SplashActivity.this, WelcomeActivity.class);
        } else {
            i = new Intent(SplashActivity.this, HomeActivity.class);
        }

        i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
        finish();
    }
}
