#ifndef ESTADO_H_
#define ESTADO_H_

#include <evento.h>
#include <Wire.h>
#include "rgb_lcd.h"

//Pines de los actuadores
#define PIN_BUZZER        32

//Configuración LCD
#define PIN_LCD_SDA             21
#define PIN_LCD_SCL             22
#define LCD_COLUMNAS            16
#define LCD_FILAS               2
#define LCD_COLUMNA_INICIAL     0
#define LCD_FILA_INICIAL        0
#define LCD_ADDR                0x27
#define TIEMPO_ESCRITURA_LCD    1000    //en milisegundos

extern rgb_lcd lcd;

//Definición de enums para los colores del fondo del LCD
enum colores {VERDE, AZUL, AMARILLO, ROJO, ROSA, BLANCO};

#define TIEMPO_ENTRE_CICLOS_DE_EJECUCION    50 //milisegundos

//Definición de los tonos del buzzer
#define TONO_SI         247
#define TONO_SOL        196
#define TONO_MI         165
#define DURACION_BUZZER 300 //en milisegundos

extern unsigned short ultimo_indice_lector_sensor;

#define MAX_ESTADOS 5
enum estados {ST_ESTABLE, ST_PULSADO, ST_ORINADO, ST_LEVANTADO, ST_APLAZADO};
extern String estados_string[MAX_ESTADOS];

extern enum estados estado_actual;
extern enum estados ultimo_estado;

//Funciones
void get_event                  ();
void fsm                        (); //Máquina de estados
void pausarActuadores           ();
void llamadaPaciente            ();
void confirmarLlamada           ();
void actualizarLCD              (bool, estados);
void cambiarFondoLCD            (colores);
//Funciones encargadas de informar, de momento mediante Serial.print, que ocurrió algo
void informarPulsoPaciente      ();
void informarOrino              ();
void informarLevanto            ();
void informarConfirmacion       ();
void informarPausaActuadores    ();
void orinoPaciente              ();
void pacienteSeLevanto          ();
void wifiConnect                ();
void callback                   (char*, byte*, unsigned int);
void mqttReconnect              ();
String generateJson             ();
void mqttInformarEstado         (const char*, const char*);

#endif