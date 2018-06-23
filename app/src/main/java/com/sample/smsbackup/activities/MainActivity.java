package com.sample.smsbackup.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.sample.smsbackup.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void erase(View view){
        Toast.makeText(
                getApplicationContext(),
                R.string.stringErase,
                Toast.LENGTH_SHORT)
        .show();
    }

}
