#ifndef EVENTO_H_
#define EVENTO_H_

#include <Arduino.h>
#include <WiFi.h>
#include "PubSubClient.h"

//Pines de los sensores
#define PIN_HUMEDAD             35
#define PIN_PRESION             34
#define PIN_PULSADOR            15
#define PIN_PULSADOR_APLAZAR    18
#define PIN_PULSADOR_CONFIRMAR  0

// Definición de Constantes
#define TIEMPO_INTERVALO_BUZZER 3000
#define TIEMPO_LEER_SENSORES    500     // Cada cuanto tiempo se leerán los sensores sin aplazo
#define TIEMPO_TIMEOUT          60000   // Cada cuanto tiempo se producirá un evento de timeout
#define UMBRAL_PRESION          250  // 250   // Valor de presión a detectar para que se considere que hay algo encima del sensor
#define UMBRAL_HUMEDAD          2000    // Valor de humedad a detectar para que se considere que haya orina en el papagayo

//Variables de tiempo de los sensores
extern unsigned long ultimo_tiempo_actual;
extern unsigned long tiempo_lectura_presion;
extern unsigned long tiempo_evento_timeout;

extern unsigned long temporizador_aplazo;

//Definición de Eventos
#define MAX_EVENTOS 7
enum eventos {EV_CONTINUE, EV_PULSO, EV_ORINO, EV_LEVANTO, EV_CONFIRMO, EV_APLAZO, EV_TIMEOUT};
extern String eventos_string[MAX_EVENTOS];

extern enum eventos nuevo_evento;
extern enum eventos ultimo_evento;

//Funciones para detectar eventos
bool consultar_llamada(bool forzar, unsigned long tiempo_actual);
bool sensar_humedad(bool forzar, unsigned long tiempo_actual);
bool sensar_presion(bool forzar, unsigned long tiempo_actual);
bool sensar_llamada(bool forzar, unsigned long tiempo_actual);
bool sensar_aplazo(bool forzar, unsigned long tiempo_actual);
bool sensar_confirmacion(bool forzar, unsigned long tiempo_actual);
bool sensar_timeout(bool forzar, unsigned long tiempo_actual);

//Función para detectar datos envíados desde el celular
void callback(char*, byte*, unsigned int);

#define MAX_LECTORES 6
#define MAX_LECTORES 6
typedef bool (*lectorSensor)(bool forzar, unsigned long tiempo_actual); // Definimos como deben ser las funciones para leer sensores
extern lectorSensor lector_sensor[MAX_LECTORES];

//Variable que indica si el paciente llamó o no a la enfermera
extern bool paciente_llamo;
extern bool aplazado;

//Estructura para guardar los datos de los pulsadores
struct pulsador
{
    unsigned short pin;
    bool estado_actual = LOW;
    bool estado_anterior = LOW;
};

//Variables para guardar los datos de los pulsadores
extern pulsador pulsadorLlamar, pulsadorAplazar, pulsadorConfirmar;

bool sensar_pulsador(pulsador*, eventos); //Función para obtener el valor de cualquier pulsador

//Variables para la configuración del Wifi y la comunicación mediante MQTT
#define TAMANIO_STRING_VALOR_SENSOR 10  //Define el tamaño que tendrá el string que guardará el valor de un sensor
#define TAMANIO_MENSAJE_MQTT        100

//Definición de tópicos de MQTT
#define TOPICO_LEVANTO      "/smartcare/levanto"
#define TOPICO_CONFIRMADO   "/smartcare/confirmado"
#define TOPICO_PAUSADO      "/smartcare/pausado"
#define TOPICO_ORINO        "/smartcare/orino"
#define TOPICO_PULSO        "/smartcare/pulso"
#define TOPICO_APLAZO       "/smartcare/aplazo"

extern const char* ssid;
extern const char* password;
extern const char* mqttServer;
extern const char* user_name;
extern const char* user_pass;  

extern const char * topic_temp;
extern const char * topic_luz;

extern int port;
extern String stMac;
extern char mac[50];
extern char clientId[50];
extern long last_time;

extern WiFiClient espClient;
extern PubSubClient client;

extern bool recibido_mensaje_aplazo;

#endif