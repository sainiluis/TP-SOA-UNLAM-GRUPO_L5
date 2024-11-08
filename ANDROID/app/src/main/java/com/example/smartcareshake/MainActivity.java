package com.example.smartcareshake;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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

        Log.i(TAG, "Ejecuta: OnCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEstado = findViewById(R.id.txtValorEstado);
        logoView = findViewById(R.id.new_image);
        button_start = findViewById(R.id.button_start);

        // Hacer visibles txtEstado y logoView directamente
        txtEstado.setVisibility(View.VISIBLE);
        logoView.setVisibility(View.VISIBLE);

        // Configurar el botón sin necesidad de la animación de cortina
        button_start.setVisibility(View.VISIBLE);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
                finish();
            }
        });

        ImageView logoView = findViewById(R.id.new_image);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        logoView.startAnimation(rotateAnimation);

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




    }

    private void connect()
    {

        mqttHandler.connect(mqttHandler.BROKER_URL,mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);


        try {

            Thread.sleep(1000);
            //subscribeToTopic(MqttHandler.TOPIC_BOTON);
            subscribeToTopic(MqttHandler.SMART_CARE_ORINO);

            Log.d("Main activity","Conectado correctamente");
            Log.i(TAG, "Estado de conexión MQTT: " + mqttHandler.isConnected());



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

        Log.i(TAG, "Ejecuta: OnResume");
        Log.i(TAG, "Estado de conexión MQTT: " + mqttHandler.isConnected());

        // Crear un Handler para ejecutar la reconexión con un retraso de 5 segundos
        new Handler().postDelayed(() -> {
            if (mqttHandler != null && !mqttHandler.isConnected()) {
                Log.i(TAG, "Reconectando MQTT después de 5 segundos en onResume...");
                connect();
            }
        }, 5000); // 5000 milisegundos = 5 segundos de delay




    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Ejecuta: OnStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mqttHandler.disconnect(); // Desconecta MQTT al salir de la actividad
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
            Log.i(TAG,"Intentando conexión");
            connect();
            Log.i(TAG,"Conectado");

        }

    }


    public class ReceptorOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            // Referencias a los TextView para actualizar los valores
            TextView txtValorEstado = findViewById(R.id.txtValorEstado);
            TextView txtSensorLlamado = findViewById(R.id.txtSensorLlamado);
            TextView txtSensorOrina = findViewById(R.id.txtSensorOrina);
            TextView txtSensorPresion = findViewById(R.id.txtSensorPresion);

            // Obtener el JSON recibido a través del Intent
            String msgJson = intent.getStringExtra("msgJson");
            if (msgJson != null) {
                Log.i("MAIN ACTIVITY", msgJson);
            }

            try {
                // Crear el objeto JSON a partir del mensaje recibido
                JSONObject jsonObject = new JSONObject(msgJson);

                // Extraer y mostrar el estado general
                String estado = jsonObject.getString("estado");
                txtValorEstado.setText("Estado Actual: " + estado);

                // Extraer y mostrar el valor del sensor de llamado
                String sensorLlamado = jsonObject.getString("llamado");
                txtSensorLlamado.setText("Sensor Llamado: " + sensorLlamado);

                // Extraer y mostrar el valor del sensor de orina
                String sensorOrina = jsonObject.getString("orina");
                txtSensorOrina.setText("Sensor Orina: " + sensorOrina);

                // Extraer y mostrar el valor del sensor de presión
                String sensorPresion = jsonObject.getString("presion");
                txtSensorPresion.setText("Sensor Presión: " + sensorPresion);

                // Verificar si el estado requiere abrir SecondActivity
                if ("PacienteLlamo".equals(estado) || "PacienteOrino".equals(estado) || "PacienteSeLevanto".equals(estado)) {
                    // Desconectar MQTT antes de cambiar de actividad
                    mqttHandler.disconnect();

                    // Ir a SecondActivity y finalizar MainActivity
                    Intent secondActivityIntent = new Intent(context, SecondActivity.class);
                    context.startActivity(secondActivityIntent);
                    ((MainActivity) context).finish(); // Finalizar MainActivity
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MAIN ACTIVITY", "Error al procesar JSON: " + e.getMessage());
            }
        }

    }


}
