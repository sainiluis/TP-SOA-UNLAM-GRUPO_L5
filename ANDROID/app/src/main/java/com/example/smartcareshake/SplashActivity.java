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

        // Se usa este handler  para retrasar la ejecución de la main activity
        // Y esperar a que el splash haya terminado
        int splashScreenDuration = 1400; // Duración del splash en milisegundos
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            //overridePendingTransition(0, 0);
            finish();
        }, splashScreenDuration);
    }
}
