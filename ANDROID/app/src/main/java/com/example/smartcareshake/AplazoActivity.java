package com.example.smartcareshake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class AplazoActivity extends AppCompatActivity {

    private TextView tvTimer;
    private Button btnCancelAplazo;

    private MqttHandler mqttHandler;
    private ReceptorOperacion receiver =new ReceptorOperacion();
    private ConnectionLost connectionLost =new ConnectionLost();

    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;

    private static final String TAG = "AplazoActivity";

    private String estadoActual;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aplazo);

        estadoActual = getIntent().getStringExtra("estado_actual");

        Log.e(TAG, "Ejecuta onCreate");


        tvTimer = findViewById(R.id.tv_timer);
        btnCancelAplazo = findViewById(R.id.button_cancel_aplazo);

        mqttHandler = MqttHandler.getInstance(this);
        configurarBroadcastReciever();
        subscribeToTopic("/smartcare");

        if (mqttHandler != null) {
            Log.i(TAG, "Estado de conexión MQTT: " + mqttHandler.isConnected());
        } else {
            Log.i(TAG, "mqttHandler es null");
        }


        //if (!mqttHandler.isConnected()) {
            // Configuramos el Broadcast receiver

         //   configurarBroadcastReciever();

            // Usamos este handler para que la conexión se haga unos ms después. ya que si no el pasaje del splash a la MainActivity no era ligero y se trababa.
         //   new Handler().postDelayed(() -> connect(), 500); // 500 ms de retraso
       // }

        // Contador para el tiempo restante de aplazo
        countDownTimer = new CountDownTimer(Constants.TIEMPO_APLAZO, Constants.COUNTDOWN) {  // (por ahora ponemos 10 seg pero en realidad deberia ser 120 seg)
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Tiempo restante: " + millisUntilFinished / Constants.COUNTDOWN + " segundos");
            }

            public void onFinish() {
                tvTimer.setText("Tiempo finalizado");
                Intent secondActivityIntent = new Intent(AplazoActivity.this, SecondActivity.class);
                secondActivityIntent.putExtra("estado_actual", estadoActual);
                startActivity(secondActivityIntent);
                finish();
            }
        }.start();


        btnCancelAplazo.setOnClickListener(v -> {
            Intent secondActivityIntent = new Intent(AplazoActivity.this, SecondActivity.class);
            secondActivityIntent.putExtra("estado_actual", estadoActual);
            startActivity(secondActivityIntent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {

        Log.e(TAG, "Ejecuta OnDestroy");
        unregisterReceiver(receiver);

        super.onDestroy();



        // try {
        //    if (mqttHandler != null) {
           //     mqttHandler.disconnect();
          //  }
           // unregisterReceiver(receiver);
           // unregisterReceiver(connectionLost);


       // } catch (Exception e) {
           // Log.e(TAG, "Error al liberar recursos", e);
       // }

    }

    private void publishMessage(String topic, String message){
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
            subscribeToTopic(MqttHandler.SMART_CARE);

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

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "ReceptorOperacion recibió un mensaje en AplazoActivity");

            String msgJson = intent.getStringExtra("msgJson");
            if (msgJson != null) {
                try {

                    JSONObject jsonObject = new JSONObject(msgJson);


                    if (jsonObject.has("estado")) {
                        String estado = jsonObject.getString("estado");
                        if (estado.equals("Paciente Se Levanto")) {

                            if (countDownTimer != null) {
                                countDownTimer.cancel();  // Detener el temporizador al recibir el mensaje
                            }

                            Intent secondActivityIntent = new Intent(AplazoActivity.this, SecondActivity.class);
                            secondActivityIntent.putExtra("estado_actual", estado);
                            startActivity(secondActivityIntent);
                            finish();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("AplazoActivity", "Error al procesar JSON: " + e.getMessage());
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
