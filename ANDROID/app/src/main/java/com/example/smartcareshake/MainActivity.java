package com.example.smartcareshake;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import java.util.Iterator;
import java.util.Random;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.example.smartcareshake.EstadoHistorico;



import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MqttHandler mqttHandler;
    private JSONObject globalState;

    private TextView txtJson;
    private TextView txtEstado;
    private Button cmdLedApagar;
    private Button cmdLedEncender;
    private Button navigateButton;


    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;
    private ReceptorOperacion receiver =new ReceptorOperacion();
    private ConnectionLost connectionLost =new ConnectionLost();

    private Handler handler = new Handler();
    private int dotCount = 0;
    private String baseText = "Monitoreando";

    Button button_start;
    TextView txt_main;
    ImageView curtainView;
    ImageView logoView;
    private static final String TAG = "MainActivity";

    private static final String PREFS_NAME = "MyAppPreferences";
    private static final String COUNTER_KEY = "contadorEstados";
    private Map<String, EstadoHistorico> contadorEstados = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Ejecuta: OnCreate");

        // Inicializar el JSON global
        globalState = new JSONObject();
        try {
            globalState.put("estado", "Monitoreando");
            globalState.put("llamado", "0");
            globalState.put("orina", "0");
            globalState.put("presion", "0");
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        String[] estados = { "PacienteOrino", "PacienteSeLevanto", "PacienteLlamo"};
//        Map<String, EstadoHistorico> contadorEstados = new HashMap<>();
//
//        for (int i = 0; i <20; i++) {
//            Random random = new Random();
//            int randomIndex = random.nextInt(3);
//            String estadoActual = estados[randomIndex];; // Alterna entre "Orino", "Levanto", y "Llamo"
//
//            // Verificamos si el estado ya existe en el mapa
//            if (contadorEstados.containsKey(estadoActual)) {
//                // Si existe, incrementamos las ocurrencias
//                contadorEstados.get(estadoActual).incrementarOcurrencias();
//            } else {
//                // Si no existe, creamos un nuevo EstadoHistorico con la ocurrencia inicial
//                EstadoHistorico estado = new EstadoHistorico(estadoActual);
//                contadorEstados.put(estadoActual, estado);
//            }
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadContadorEstados();


        // Configurar el título, estado y párrafo iniciales
        TextView txtEstado = findViewById(R.id.button_3);  // Botón que muestra el estado actual
        txtEstado.setText("Monitoreando...");

        // Configurar la animación del logo
        ImageView logoView = findViewById(R.id.logoView);
        //Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        //logoView.startAnimation(rotateAnimation);

        Button buttonHistorico = findViewById(R.id.button_5);
        buttonHistorico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                saveContadorEstados();
               // mqttHandler.disconnect();
                try{
                    Intent Historicointent = new Intent(MainActivity.this, HistoricoActivity.class);
                    Historicointent.putExtra("contadorEstados", (Serializable) contadorEstados);
                    startActivity(Historicointent);
                } catch(Exception e) {
                    Log.i(TAG, "Error",e);
                }


            }
        });

        // Inicializar MQTT Handler y verificar conexión
        mqttHandler = new MqttHandler(getApplicationContext());

        if (mqttHandler != null) {
            Log.i(TAG, "Estado de conexión MQTT: " + mqttHandler.isConnected());
        } else {
            Log.i(TAG, "mqttHandler es null");
        }

        // Configurar BroadcastReceiver y conexión con retraso
        if (!mqttHandler.isConnected()) {
            configurarBroadcastReciever();
            new Handler().postDelayed(() -> connect(), 500); // 500 ms de retraso
        }

        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        txtEstado.startAnimation(pulseAnimation);
    }

    private void actualizarEstado(String estado) {
        // Referencia al botón que muestra el estado actual
        Button txtEstado = findViewById(R.id.button_3);
        txtEstado.setText(estado);

        // Verificar si el estado requiere pasar a SecondActivity
        if ("Paciente Llamo".equals(estado) || "Paciente Orino".equals(estado) || "Paciente Se Levanto".equals(estado)) {

            saveContadorEstados();

            // Desconectar MQTT antes de cambiar de actividad
            mqttHandler.disconnect();

            // Ir a SecondActivity y finalizar MainActivity
            Intent secondActivityIntent = new Intent(MainActivity.this, SecondActivity.class);
            secondActivityIntent.putExtra("estado_alerta", estado);
            startActivity(secondActivityIntent);
            finish();
        }
    }

    private void agregarEstadoAlHistorial(String estado) {
        if ("Paciente Llamo".equals(estado) || "Paciente Orino".equals(estado) || "Paciente Se Levanto".equals(estado)) {
            // Si el estado ya existe en el mapa, incrementa el contador de ocurrencias
            if (contadorEstados.containsKey(estado)) {
                EstadoHistorico estadoExistente = contadorEstados.get(estado);
                estadoExistente.incrementarOcurrencias(); // Asegúrate de que este método esté incrementando el valor correctamente
            } else {
                // Si el estado no existe en el mapa, crea un nuevo objeto y agrégalo al mapa
                EstadoHistorico estadoNuevo = new EstadoHistorico(estado);
                contadorEstados.put(estado, estadoNuevo);
            }
        }
    }

    private void loadContadorEstados() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        contadorEstados = new HashMap<>();

        // Recuperar el JSON guardado en SharedPreferences
        String jsonString = prefs.getString("contadorEstados", null);
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keys = jsonObject.keys();

                // Convertir el JSON a un HashMap de <String, EstadoHistorico>
                while (keys.hasNext()) {
                    String key = keys.next();
                    int ocurrencias = jsonObject.getInt(key);

                    // Crear un nuevo EstadoHistorico y asignar el número de ocurrencias
                    EstadoHistorico estadoHistorico = new EstadoHistorico(key);
                    estadoHistorico.setOcurrencias(ocurrencias);

                    contadorEstados.put(key, estadoHistorico);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveContadorEstados() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Limpia los valores anteriores

        // Guardar el HashMap contadorEstados en SharedPreferences como un JSON String
        JSONObject json = new JSONObject();
        for (Map.Entry<String, EstadoHistorico> entry : contadorEstados.entrySet()) {
            try {
                json.put(entry.getKey(), entry.getValue().getOcurrencias());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString("contadorEstados", json.toString());
        editor.apply(); // Guardar cambios de forma asincrónica
    }


    private void connect()
    {

        mqttHandler.connect(mqttHandler.BROKER_URL,mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);


        try {

            Thread.sleep(1000);
            //subscribeToTopic(MqttHandler.TOPIC_BOTON);
            subscribeToTopic(MqttHandler.SMART_CARE);

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
            handler.removeCallbacksAndMessages(null);
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

            // Obtener el JSON recibido a través del Intent
            String msgJson = intent.getStringExtra("msgJson");
            if (msgJson != null) {
                Log.i("MAIN ACTIVITY", "Mensaje recibido: " + msgJson);

                try {
                    // Parsear el mensaje recibido
                    JSONObject newMessage = new JSONObject(msgJson);

                    // Verificar y actualizar cada campo individualmente
                    if (newMessage.has("estado")) {
                        String estado = newMessage.getString("estado");
                        globalState.put("estado", estado);
                        agregarEstadoAlHistorial(estado);
                        actualizarEstado(estado); // Verificar si se necesita cambiar a SecondActivity
                    }

                } catch (JSONException e) {
                    Log.e("MAIN ACTIVITY", "Error al procesar JSON: " + e.getMessage());
                }
            }
        }

    }


}
