package com.ViDuo.viduo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SplashScreen extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        SystemClock.sleep(3000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(SplashScreen.this,DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}