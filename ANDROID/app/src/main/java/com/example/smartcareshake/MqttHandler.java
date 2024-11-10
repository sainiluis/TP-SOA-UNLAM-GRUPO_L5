package com.example.smartcareshake;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;



public class MqttHandler implements MqttCallback {
    public static final String BROKER_URL = "tcp://broker.emqx.io:1883";
    public static final String CLIENT_ID = "mqttx_f9bfd3w4";
    public static final String USER="";
    public static final String PASS="";

    public static final String SMART_CARE_PULSO = "/smartcare/pulso";
    public static final String SMART_CARE = "/smartcare";
    public static final String SMART_CARE_LEVANTO = "/smartcare/levanto";

    public static final String ACTION_DATA_RECEIVE ="com.example.intentservice.intent.action.DATA_RECEIVE";
    public static final String ACTION_CONNECTION_LOST ="com.example.intentservice.intent.action.CONNECTION_LOST";
    private MqttClient client;
    private Context mContext;

    private static MqttHandler instance;

    public MqttHandler(Context mContext) {

        this.mContext = mContext;

    }

    public static MqttHandler getInstance(Context context) {
        if (instance == null) {
            instance = new MqttHandler(context.getApplicationContext());
        }
        return instance;
    }

    public void connect( String brokerUrl, String clientId,String username, String password) {
        try {


            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(username);
            options.setPassword(password.toCharArray());


            ///////////////////////////////////
            // Set up the persistence layer
            MemoryPersistence persistence = new MemoryPersistence();

            client = new MqttClient(brokerUrl, clientId, persistence);
            client.connect(options);
            Log.i("MAIN ACTIVITY", "Conectando mqtt...");
            client.setCallback(this);
            //client.subscribe("#");
        } catch (MqttException e) {
            Log.d("Aplicacion",e.getMessage()+ "  "+e.getCause());
        }
    }

    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(2);
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d("MAIN ACTIVITY","conexion perdida "+ cause.getMessage().toString());

        Intent i = new Intent(ACTION_CONNECTION_LOST);
        mContext.sendBroadcast(i);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        String msgJson=message.toString();

        //JSONObject json = new JSONObject(message.toString());

        //Se envian los valores sensados por el potenciometro, al bradcast reciever de la activity principal
        Intent i = new Intent(ACTION_DATA_RECEIVE);
        i.putExtra("msgJson", msgJson);

        mContext.sendBroadcast(i);

    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

}
