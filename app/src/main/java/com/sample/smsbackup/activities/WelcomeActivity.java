package com.sample.smsbackup.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sample.smsbackup.R;
import com.sample.smsbackup.adapters.WelcomeAdapter;
import com.sample.smsbackup.utilities.Constants;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager welcomePager;
    private LinearLayout dotLayout;
    private WelcomeAdapter adapter;
    private TextView[] dots;
    private Button btnGoNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcomePager = findViewById(R.id.welcomePager);
        dotLayout = findViewById(R.id.dotLayout);
        btnGoNext = findViewById(R.id.btnGoNext);
        adapter = new WelcomeAdapter(this);

        welcomePager.setAdapter(adapter);
        welcomePager.addOnPageChangeListener(pagerListener);

        updateFooter(0);
    }

    private void updateFooter(int position){

        dots = new TextView[adapter.slideTitle.length];
        dotLayout.removeAllViews();

        for (int i=0; i<dots.length;i++){
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;", Html.FROM_HTML_MODE_LEGACY));
            dots[i].setTextSize(35);
            dots[i].setTextColor(ContextCompat.getColor(this, R.color.doInactive));

            dotLayout.addView(dots[i]);
        }

        dots[position].setTextColor(ContextCompat.getColor(this, R.color.dotActive));

        if(position == dots.length - 1)
            btnGoNext.setVisibility(View.VISIBLE);
        else
            btnGoNext.setVisibility(View.GONE);
    }

    public void onGoNext(View view){
        Intent homeActivityIntent = new Intent(WelcomeActivity.this, HomeActivity.class);
        homeActivityIntent.setFlags(homeActivityIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(homeActivityIntent);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        settings.edit().putBoolean(Constants.PREFS_FIRST_LUNCH, false).apply();
        finish();
    }

    ViewPager.OnPageChangeListener pagerListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            updateFooter(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
