    package com.example.smartcareshake;


    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.content.IntentFilter;
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
    import android.util.Log;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;
    import android.content.BroadcastReceiver;

    import org.json.JSONException;
    import org.json.JSONObject;

    public class SecondActivity extends AppCompatActivity implements SensorEventListener {

        private SensorManager sensorManager;
        private float acelVal;
        private float acelLast;
        private float shake;
        MediaPlayer mediaPlayerNotification;
        private static final String TAG = "SecondActivity";

        private MqttHandler mqttHandler;
        private ReceptorOperacion receiver =new ReceptorOperacion();
        private ConnectionLost connectionLost =new ConnectionLost();

        public IntentFilter filterReceive;
        public IntentFilter filterConncetionLost;

        private Button buttonEstadoAlerta;
        private Button buttonValorSensor;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_second);

            // Referencias a los botones
            buttonEstadoAlerta = findViewById(R.id.button_estado_alerta);
            buttonValorSensor = findViewById(R.id.button_valor_sensor);

            // Obtiene el valor del estado alerta (este valor se envió desde la main activity
            String estadoAlerta = getIntent().getStringExtra("estado_alerta");

            // Actualiza los botones con los valores recibidos
            if (estadoAlerta != null) {
                buttonEstadoAlerta.setText("Estado: " + estadoAlerta);
            } else {
                buttonEstadoAlerta.setText("Estado: No disponible");
            }

            buttonValorSensor.setText("Valor sensor: No disponible");


            mqttHandler = new MqttHandler(getApplicationContext());

            if (mqttHandler != null) {
                Log.i(TAG, "Estado de conexión MQTT: " + mqttHandler.isConnected());
            } else {
                Log.i(TAG, "mqttHandler es null");
            }


            if (!mqttHandler.isConnected()) {
                // Configuramos el Broadcast receiver

                configurarBroadcastReciever();

                // Usamos este handler para que la conexión se haga unos ms después. ya que si no el pasaje del splash a la MainActivity no era ligero y se trababa.
                new Handler().postDelayed(() -> connect(), 500); // 500 ms de retraso
            }

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
            Log.i(TAG, "Ejecuta: OnPause");
        }

        @Override
        protected void onDestroy() {
            try {
                mqttHandler.disconnect();
                unregisterReceiver(receiver);
                unregisterReceiver(connectionLost);
            } catch (Exception e) {
                Log.e(TAG, "Error al liberar recursos", e);
            }
            super.onDestroy();
        }

        @Override
        protected void onResume() {
            super.onResume();
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

            // se reactiva el sonido de alerta
            mediaPlayerNotification.start();
        }


        private void publishMessage(String topic, String message){
            Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
            mqttHandler.publish(topic,message);
        }
        private void subscribeToTopic(String topic){
            Toast.makeText(this, "Subscribing to topic "+ topic, Toast.LENGTH_SHORT).show();
            mqttHandler.subscribe(topic);
        }

        private void connect()
        {

            mqttHandler.connect(mqttHandler.BROKER_URL,mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);


            try {

                Thread.sleep(1000);
                //subscribeToTopic(MqttHandler.TOPIC_BOTON);
                subscribeToTopic(MqttHandler.SMART_CARE_ORINO);

                Log.i(TAG,"Conectado correctamente");
                Log.i(TAG, "Estado de conexión MQTT: " + mqttHandler.isConnected());



            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }



        }


        public class ConnectionLost extends BroadcastReceiver

        {

            public void onReceive(Context context, Intent intent) {

                Toast.makeText(getApplicationContext(),"Conexion Perdida",Toast.LENGTH_SHORT).show();
                Log.i(TAG,"Intentando conexión");
                connect();
                Log.i(TAG,"Conectado");

            }

        }


        // BroadcastReceiver para escuchar mensajes de confirmación
        public class ReceptorOperacion extends BroadcastReceiver {

            public void onReceive(Context context, Intent intent) {

                // Obtener el JSON recibido a través del Intent
                String msgJson = intent.getStringExtra("msgJson");
                if (msgJson != null && msgJson.contains("\"confirmar\": \"true\"")) {
                    Log.i("SecondActivity", "Confirmación recibida. Regresando a MainActivity...");

                    // Desconectar MQTT y cerrar SecondActivity
                    mqttHandler.disconnect();

                    // Regresar a MainActivity
                    Intent mainIntent = new Intent(SecondActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();  // Cerrar la actividad actual
                }

                // Verificar si el mensaje contiene "valorSensor"
                if (msgJson.contains("\"valorSensor\"")) {
                    try {
                        // Convertir el mensaje a JSON
                        JSONObject jsonObject = new JSONObject(msgJson);

                        // Obtener el valor del sensor
                        String valorSensor = jsonObject.getString("valorSensor");

                        // Actualizar el texto del botón con el valor del sensor
                        Button buttonValorSensor = findViewById(R.id.button_valor_sensor);
                        buttonValorSensor.setText("Valor sensor: " + valorSensor);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("SecondActivity", "Error al procesar JSON: " + e.getMessage());
                    }
                }

            }



        }

        private void configurarBroadcastReciever()
        {
            //se asocia(registra) la  accion RESPUESTA_OPERACION, para que cuando el Servicio de recepcion la ejecute
            //se invoque automaticamente el OnRecive del objeto receiver
            filterReceive = new IntentFilter(MqttHandler.ACTION_DATA_RECEIVE);
            filterConncetionLost = new IntentFilter(MqttHandler.ACTION_CONNECTION_LOST);

            filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
            filterConncetionLost.addCategory(Intent.CATEGORY_DEFAULT);

            registerReceiver(receiver, filterReceive);
            registerReceiver(connectionLost,filterConncetionLost);

        }
    }
