    package com.example.smartcareshake;


    import android.content.Context;
    import android.content.Intent;
    import android.hardware.Sensor;
    import android.hardware.SensorEvent;
    import android.hardware.SensorEventListener;
    import android.hardware.SensorManager;
    import android.hardware.camera2.CameraAccessException;
    import android.hardware.camera2.CameraManager;
    import android.media.MediaPlayer;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.VibrationEffect;
    import android.os.Vibrator;
    import android.widget.Button;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;

    public class SecondActivity extends AppCompatActivity implements SensorEventListener {

        private SensorManager sensorManager;
        private float acelVal;
        private float acelLast;
        private float shake;
        MediaPlayer mediaPlayerNotification;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_second);

            // Sonido de alerta en loop
            mediaPlayerNotification = MediaPlayer.create(this, R.raw.notification_alert);
            mediaPlayerNotification.setLooping(true);
            mediaPlayerNotification.start();

            // Inicializamos el sensor del acelerómetro
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

            acelVal = SensorManager.GRAVITY_EARTH;
            acelLast = SensorManager.GRAVITY_EARTH;
            shake = 0.00f;

            Button btnVolverMain = findViewById(R.id.button_volver_main);
            btnVolverMain.setOnClickListener(v -> {
                if (mediaPlayerNotification != null && mediaPlayerNotification.isPlaying()) {
                    mediaPlayerNotification.stop(); // Detenemos el sonido de alerta
                    mediaPlayerNotification.release(); // Liberamos los recursos del MediaPlayer
                    mediaPlayerNotification = null;
                }

                Intent intent = new Intent(SecondActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Opcional: cerrar SecondActivity y no regresar cuando se presione "Atrás"
            });

            Button btnAplazar = findViewById(R.id.button_aplazar);
            btnAplazar.setOnClickListener(v -> {
                aplazarSolicitud();

                Intent intent = new Intent(SecondActivity.this, AplazoActivity.class);
                startActivity(intent);
            });
        }

        boolean shakeEnabled = true;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!shakeEnabled) return;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            acelLast = acelVal;
            acelVal = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = acelVal - acelLast;
            shake = shake * 0.9f + delta;

            if (shake > 12) {
                aplazarSolicitud();

                // Nos movemos a la activity del aplazo
                Intent intent = new Intent(SecondActivity.this, AplazoActivity.class);
                startActivity(intent);

                shakeEnabled = false;

                // Bloqueamos la activación del shake por 10 segundos
                // esto lo hicimos porque sino permitía hacer muchos shakes juntos, y no quedaba bien el sonido de alerta
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        shakeEnabled = true;
                    }
                }, Constants.TIEMPO_APLAZO); // 10 segundos
            }
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No se necesita en este caso
        }

        // Función que se ejecuta cuando se detecta el shake
        private void aplazarSolicitud() {
            // apagar la notificación de alerta
            mediaPlayerNotification.pause();

            // Reproduce el sonido cuando se aplaza la solicitud
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.text_to_voice_alerta_aplazada);
            mediaPlayer.start();

            // Vibración
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)); // 500 ms de vibración
            }

            // Flash Luz
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraId = camManager.getCameraIdList()[0]; // 0 es la cámara trasera
                for (int i = 0; i < 2; i++) {
                    camManager.setTorchMode(cameraId, true);  // Flash ON
                    Thread.sleep(200);  // Pausa 200ms
                    camManager.setTorchMode(cameraId, false); // Flash OFF
                    Thread.sleep(200);  // Pausa 200ms
                }
            } catch (CameraAccessException | InterruptedException e) {
                e.printStackTrace();
            }


            Toast.makeText(this, "Solicitud aplazada", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPause() {
            super.onPause();
            sensorManager.unregisterListener(this);
        }

        @Override
        protected void onResume() {
            super.onResume();
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

            // se reactiva el sonido de alerta
            mediaPlayerNotification.start();
        }
    }
