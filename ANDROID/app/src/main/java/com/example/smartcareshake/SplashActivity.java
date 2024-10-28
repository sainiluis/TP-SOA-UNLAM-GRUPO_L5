package com.example.smartcareshake;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        ImageView splashImage = findViewById(R.id.splash_image);
        Animation slideLeft = AnimationUtils.loadAnimation(this, R.anim.slide_left);
        splashImage.startAnimation(slideLeft);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, SecondActivity.class);
            startActivity(intent);
            finish();
        }, Constants.DELAY_SPLASH);
    }
}