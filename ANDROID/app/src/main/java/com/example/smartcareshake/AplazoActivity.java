package com.example.smartcareshake;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AplazoActivity extends AppCompatActivity {

    private TextView tvTimer;
    private Button btnCancelAplazo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aplazo);

        tvTimer = findViewById(R.id.tv_timer);
        btnCancelAplazo = findViewById(R.id.button_cancel_aplazo);

        // Contador para el tiempo restante de aplazo
        new CountDownTimer(Constants.TIEMPO_APLAZO, Constants.COUNTDOWN) {  // (por ahora ponemos 10 seg pero en realidad deberia ser 120 seg)
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Tiempo restante: " + millisUntilFinished / Constants.COUNTDOWN + " segundos");
            }

            public void onFinish() {
                tvTimer.setText("Tiempo finalizado");
                finish();
            }
        }.start();


        btnCancelAplazo.setOnClickListener(v -> {
            finish();
        });
    }
}
