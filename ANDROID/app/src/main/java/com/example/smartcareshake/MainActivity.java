package com.example.smartcareshake;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Process;
import android.widget.Toast;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;


import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity {

    private MqttHandler mqttHandler;

    private TextView txtJson;
    private TextView txtEstado;
    private Button cmdLedApagar;
    private Button cmdLedEncender;
    private Button navigateButton;


    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;
    private ReceptorOperacion receiver =new ReceptorOperacion();
    private ConnectionLost connectionLost =new ConnectionLost();


    Button button_start;
    TextView txt_main;
    ImageView curtainView;
    ImageView logoView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //txtJson=(TextView)findViewById(R.id.txtJson);
        txtEstado=(TextView)findViewById(R.id.txtValorEstado);


        // Encontrar la vista de la cortina y el botón
        curtainView = findViewById(R.id.curtain_view);
        button_start = findViewById(R.id.button_start);
        logoView = findViewById(R.id.new_image);
        //txt_main = findViewById(R.id.txt_main);

        // Ocultar el botón al inicio
        button_start.setVisibility(View.INVISIBLE);
        logoView.setVisibility(View.INVISIBLE);

        // Cargar la animación
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.curtain_open);

        // Aplicar la animación
        curtainView.startAnimation(animation);

        // Listener para manejar el final de la animación
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Ocultar la vista de la cortina y mostrar el botón
                curtainView.setVisibility(View.GONE);
                button_start.setVisibility(View.VISIBLE);
                txtEstado.setVisibility(View.VISIBLE);
                button_start.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                logoView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mqttHandler = new MqttHandler(getApplicationContext());

        connect();

        configurarBroadcastReciever();


    }

    private void connect()
    {
        mqttHandler.connect(mqttHandler.BROKER_URL,mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);


        try {

            Thread.sleep(1000);
            //subscribeToTopic(MqttHandler.TOPIC_BOTON);
            subscribeToTopic(MqttHandler.TOPIC_TEMPERATURA);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }



    }

    //Metodo que crea y configurar un broadcast receiver para comunicar el servicio que recibe los mensaje del servidor
    //con la activity principal
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



    public void onClick(View v) {
        // Ejemplo de navegación y mensaje
        txt_main.setText("INICIANDO APP...");
        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        String current_date = LocalDateTime.now().toString();
        intent.putExtra("date", current_date);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connect(); // Reconecta MQTT al volver
        Log.i(TAG, "Ejecuta: OnResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Ejecuta: OnStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mqttHandler.disconnect(); // Desconecta MQTT al salir de la actividad
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

    private void publishMessage(String topic, String message){
        Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic,message);
    }
    private void subscribeToTopic(String topic){
        Toast.makeText(this, "Subscribing to topic "+ topic, Toast.LENGTH_SHORT).show();
        mqttHandler.subscribe(topic);
    }



    public class ConnectionLost extends BroadcastReceiver

    {

        public void onReceive(Context context, Intent intent) {

            Toast.makeText(getApplicationContext(),"Conexion Perdida",Toast.LENGTH_SHORT).show();

            connect();

        }

    }


    public class ReceptorOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            //Se obtiene los valores que envio el servicio atraves de un untent
            //NOtAR la utilizacion de un objeto Bundle es opcional.
            String msgJson = intent.getStringExtra("msgJson");
            txtJson.setText(msgJson);

            try {
                JSONObject jsonObject = new JSONObject(msgJson);
                String value = jsonObject.getString("value");
                txtEstado.setText(value+"°");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

    }


}
