package com.saida.mahmood.plantina;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SplashScreenActivity extends AppCompatActivity {

    private LinearLayout title;
    private TextView tagLine;
    private View border;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        title = findViewById(R.id.tagLine);
        tagLine = findViewById(R.id.tagline);
        border = findViewById(R.id.borderView);
        Animation();

        Thread timer = new Thread() {
            public void run() {
                try {

                    sleep(4000);
                    Intent intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };
        timer.start();
    }

    public void Animation() {
        Animation myanimeUp = AnimationUtils.loadAnimation(this, R.anim.transition);
        title.startAnimation(myanimeUp);
        tagLine.startAnimation(myanimeUp);
        border.startAnimation(myanimeUp);
    }
}