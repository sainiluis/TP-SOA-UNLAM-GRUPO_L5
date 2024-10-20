#include <evento.h>
#include <WiFi.h>
#include "PubSubClient.h"
#include <ArduinoJson.h>

//No se pueden declarar valores de variables en el archivo header
//por lo que se tiene que ejecutar este código para que se
//declaren los valores de los eventos y otras variables
unsigned long ultimo_tiempo_actual = 0;
unsigned long tiempo_lectura_presion = 0;
unsigned long tiempo_lectura_humedad = 0;
unsigned long tiempo_evento_llamada = 0;
unsigned long tiempo_evento_timeout = 0;
unsigned long tiempo_lectura_aplazo = 0;

//Variables para detectar el aplazo
bool aplazado = false;
bool recibido_mensaje_aplazo = false;

//Configuración del wifi y el servidor mqtt
const char* ssid        = "SO Avanzados";
const char* password    = "SOA.2019";
const char* mqttServer  = "broker.emqx.io";
const char* user_name   = "";
const char* user_pass   = "";

int port = 1883;
String stMac;
char mac[50];
char clientId[50];

WiFiClient espClient;
PubSubClient client(espClient);

//Guardamos los nombres de los eventos para informar por pantalla
String eventos_string[] = {"EV_CONTINUE", "EV_PULSO", "EV_ORINO", "EV_LEVANTO", "EV_CONFIRMO", "EV_APLAZO", "EV_TIMEOUT"};

//Guardamos los punteros de las funciones que leen el estado de los sensores
lectorSensor lector_sensor[] = {sensar_presion, sensar_humedad, sensar_llamada, sensar_aplazo, sensar_timeout, sensar_confirmacion};

//Creamos variables para guardar el último evento ocurrido y el nuevo evento
enum eventos nuevo_evento;
enum eventos ultimo_evento;

//Configuración de los pulsadores  
pulsador pulsadorLlamar;
pulsador pulsadorAplazar;
pulsador pulsadorConfirmar;

bool sensar_llamada(bool forzar, unsigned long tiempo_actual)
{
  return sensar_pulsador(&pulsadorLlamar, EV_PULSO);
}

bool sensar_humedad(bool forzar, unsigned long tiempo_actual)
{
  if (tiempo_actual == 0)
    tiempo_actual = millis();

  // obtenemos el tiempo transcurrido entre el tiempo actual y la última vez que se midió la humedad
  unsigned long diferencia = (forzar) ? (TIEMPO_LEER_SENSORES) : (tiempo_actual - tiempo_lectura_humedad);

  if (diferencia >= TIEMPO_LEER_SENSORES)
  {
    tiempo_lectura_humedad = tiempo_actual;

    unsigned short valor_lectura = analogRead(PIN_HUMEDAD);

    if(valor_lectura >= UMBRAL_HUMEDAD)
    {
      nuevo_evento = EV_ORINO;
      return true;
    }
  }
  return false;
}

bool sensar_presion(bool forzar, unsigned long tiempo_actual)
{
  if (tiempo_actual == 0)
    tiempo_actual = millis();

  // obtenemos el tiempo transcurrido entre el tiempo actual y la última vez que se midió la presión
  unsigned long diferencia = (forzar) ? (TIEMPO_LEER_SENSORES) : (tiempo_actual - tiempo_lectura_presion);

  if (diferencia >= TIEMPO_LEER_SENSORES)
  {
    tiempo_lectura_presion = tiempo_actual;

    unsigned short valor_lectura = analogRead(PIN_PRESION);

    //Serial.println(valor_lectura);

    if (valor_lectura < UMBRAL_PRESION)
    {
      nuevo_evento = EV_LEVANTO;
      return true;
    }
  }
  return false;
}

bool sensar_confirmacion(bool forzar, unsigned long tiempo_actual)
{
 return sensar_pulsador(&pulsadorConfirmar, EV_CONFIRMO);
}

bool sensar_aplazo(bool forzar, unsigned long tiempo_actual)
{
  if (tiempo_actual == 0)
  tiempo_actual = millis();

  // obtenemos el tiempo transcurrido entre el tiempo actual y la última vez que se midió la presión
  unsigned long diferencia = (forzar) ? (TIEMPO_LEER_SENSORES) : (tiempo_actual - tiempo_lectura_aplazo);

  if (diferencia >= TIEMPO_LEER_SENSORES)
  {
    tiempo_lectura_aplazo = tiempo_actual;

    if(recibido_mensaje_aplazo) {
      nuevo_evento = EV_APLAZO;

      recibido_mensaje_aplazo = false;
    
      return true;
    }

  }
  return false;
}

bool sensar_timeout(bool forzar, unsigned long tiempo_actual)
{
  if(aplazado == false)
  {
    return false;
  }

  if (tiempo_actual == 0)
    tiempo_actual = millis();

  // obtenemos el tiempo transcurrido entre el tiempo actual y la última vez que se midió la presión
  unsigned long diferencia = (forzar) ? (TIEMPO_TIMEOUT) : (tiempo_actual - tiempo_evento_timeout);

  if (diferencia >= TIEMPO_TIMEOUT)
  {
    tiempo_evento_timeout = tiempo_actual;
    nuevo_evento = EV_TIMEOUT;
    return true;
  }
  
  return false;
}

//Función para leer los valores de cualquier pulsador
bool sensar_pulsador(pulsador *pulsador, eventos evento)
{
  pulsador->estado_actual = digitalRead(pulsador->pin);
  bool cambio = (pulsador->estado_actual == LOW && pulsador->estado_anterior == HIGH);

  if(cambio)
  {
    nuevo_evento = evento;
  }
  pulsador->estado_anterior = pulsador->estado_actual;
  
  return cambio;
}

// Funciones para manejo del aplazo desde el teléfono

//Funcion Callback que recibe los mensajes enviados por lo dispositivos
void callback(char* topic, byte* message, unsigned int length) 
{
  char cMessage=char(*message);

  if(cMessage == 'a'){
    recibido_mensaje_aplazo = true;
  }
}