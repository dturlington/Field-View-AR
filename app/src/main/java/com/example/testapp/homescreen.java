package com.example.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class homescreen extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);


    }

 // HOW TO MAKE BUTTON
    public void goToAR(View v) {
        Intent intent = new Intent(homescreen.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    public void goToHowTo(View v) {
        Intent intent = new Intent(homescreen.this, howto.class);
        startActivity(intent);
        finish();
    }
    public void goToCredits(View v) {
        Intent intent = new Intent(homescreen.this, credits.class);
        startActivity(intent);
        finish();
    }
}



