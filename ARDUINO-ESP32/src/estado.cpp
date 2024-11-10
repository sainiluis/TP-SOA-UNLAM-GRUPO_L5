#include <estado.h>

unsigned short ultimo_indice_lector_sensor = 0;
unsigned long ultima_llamada_buzzer = 0;
//configuración inicial de los estados
String estados_string[] = {"ST_ESTABLE", "ST_PULSADO", "ST_ORINADO", "ST_LEVANTADO", "ST_APLAZADO"};
enum estados estado_actual;
enum estados ultimo_estado;

//Configuración inicial del display
rgb_lcd lcd;
unsigned long tiempo_lcd = 0; //temporizador del lcd

void get_event()
{
  //Leemos los sensores
  unsigned short indice = 0;
  unsigned long tiempo_actual = millis();
  unsigned long diferencia = (tiempo_actual - ultimo_tiempo_actual);
  bool timeout = (diferencia > TIEMPO_ENTRE_CICLOS_DE_EJECUCION)?(true):(false);

  if(timeout)
  {
    timeout = false;
    ultimo_tiempo_actual = tiempo_actual;

    indice = (ultimo_indice_lector_sensor % MAX_LECTORES);
    ultimo_indice_lector_sensor++;

    if(lector_sensor[indice](false, tiempo_actual))
      return; //Si los sensores detectaron un evento salimos para atenderlo en fsm
  }
  //Si los sensores no detectaron un nuevo evento, continuamos
  nuevo_evento = EV_CONTINUE;
}

//Finite-State Machine
void fsm()
{
  //Obtenemos un evento
  get_event();

  //Se valida el estado
  //En caso de serlo, actuamos de acuerdo al evento y al estado

  switch ( estado_actual )
  {
    case ST_ESTABLE:
    {
      switch ( nuevo_evento )
      {
        case EV_PULSO:
        {
          actualizarLCD(true, ST_PULSADO);
          llamadaPaciente();
          estado_actual = ST_PULSADO;
        }
        break;

        case EV_ORINO:
        {
          actualizarLCD(true, ST_ORINADO);
          informarOrino();
          estado_actual = ST_ORINADO;
        }
        break;

        case EV_LEVANTO:
        {
          actualizarLCD(true, ST_LEVANTADO);
          informarLevanto();
          estado_actual = ST_LEVANTADO;
        }
        break;
        
        case EV_CONTINUE:
        {
          actualizarLCD(false, ST_ESTABLE);
        }
        break;

        default:
        break;
      }
    }
    break;

    case ST_PULSADO:
    {
      switch ( nuevo_evento )
      {
        case EV_CONFIRMO:
        {
          actualizarLCD(true, ST_ESTABLE);
          confirmarLlamada();
          pausarActuadores();
          estado_actual = ST_ESTABLE;
        }
        break;

        case EV_APLAZO:
        {
          aplazado = true;
          actualizarLCD(true, ST_APLAZADO);
          pausarActuadores();
          ultimo_estado = ST_PULSADO;
          estado_actual = ST_APLAZADO;
        }
        break;

        case EV_ORINO:
        {
          actualizarLCD(true, ST_ORINADO);
          informarOrino();
          estado_actual = ST_ORINADO;
        }
        break;

        case EV_LEVANTO:
        {
          actualizarLCD(true, ST_LEVANTADO);
          informarLevanto();
          estado_actual = ST_LEVANTADO;
        }
        break;
        
        case EV_CONTINUE:
        {
          actualizarLCD(false, ST_PULSADO);

          if (millis() - ultima_llamada_buzzer > TIEMPO_INTERVALO_BUZZER){
            llamadaPaciente();
            ultima_llamada_buzzer = millis();
          }

          
        }
        break;

        default:
        break;
        }
    }
    break;

    case ST_ORINADO:
    {
      switch ( nuevo_evento )
      {
        case EV_CONFIRMO:
        {
          actualizarLCD(true, ST_ESTABLE);
          confirmarLlamada();
          pausarActuadores();
          estado_actual = ST_ESTABLE;
        }
        break;

        case EV_APLAZO:
        {
          aplazado = true;
          actualizarLCD(true, ST_APLAZADO);
          pausarActuadores();
          ultimo_estado = ST_ORINADO;
          estado_actual = ST_APLAZADO;
        }
        break;

        case EV_LEVANTO:
        {
          actualizarLCD(true, ST_LEVANTADO);
          informarLevanto();
          estado_actual = ST_LEVANTADO;
        }
        break;
        
        case EV_CONTINUE:
        {
          actualizarLCD(false, ST_ORINADO);
          if (millis() - ultima_llamada_buzzer > TIEMPO_INTERVALO_BUZZER){
            orinoPaciente();
            ultima_llamada_buzzer = millis();
          }
        }
        break;

        default:
        break;
        }
    }
    break;
    
    case ST_LEVANTADO:
    {
      switch ( nuevo_evento )
      {
        case EV_CONFIRMO:
        {
          actualizarLCD(true, ST_ESTABLE);
          confirmarLlamada();
          estado_actual = ST_ESTABLE;
        }
        break;
        
        case EV_CONTINUE:
        {
          actualizarLCD(false, ST_LEVANTADO);
          if (millis() - ultima_llamada_buzzer > TIEMPO_INTERVALO_BUZZER){
            pacienteSeLevanto();
            ultima_llamada_buzzer = millis();
          }
        }
        break;

        default:
        break;
        }
    }
    break;

    case ST_APLAZADO:
    {
      switch ( nuevo_evento )
      {
        case EV_CONFIRMO:
        {
          actualizarLCD(true, ST_ESTABLE);
          confirmarLlamada();
          pausarActuadores();
          estado_actual = ST_ESTABLE;
          aplazado = false;
        }
        break;

        case EV_LEVANTO:
        {
          actualizarLCD(true, ST_LEVANTADO);
          informarLevanto();
          estado_actual = ST_LEVANTADO;
          aplazado = false;
        }
        break;

        case EV_TIMEOUT:
        {
          actualizarLCD(true, ultimo_estado);
          estado_actual = ultimo_estado;
          aplazado = false;
        }
        break;
        
        case EV_CONTINUE:
        {
          actualizarLCD(false, ST_APLAZADO);
        }
        break;

        default:
        break;
        }
    }
    break;

    default:
    break;
  }

}

//Definición de funciones propias
void pausarActuadores(){
  tone(PIN_BUZZER, TONO_SOL, DURACION_BUZZER);
  informarPausaActuadores();
}

// Activa los actuadores cuando el paciente orinó
void orinoPaciente(){
  tone(PIN_BUZZER, TONO_SI, DURACION_BUZZER);
  informarOrino();
}
// Activa los actuadores cuando el paciente se levantó
void pacienteSeLevanto(){
  tone(PIN_BUZZER, TONO_SI, DURACION_BUZZER);
  informarLevanto();
}

void llamadaPaciente()
{
  tone(PIN_BUZZER, TONO_SI, DURACION_BUZZER);
  informarPulsoPaciente();
}

void confirmarLlamada()
{
  tone(PIN_BUZZER, TONO_MI, DURACION_BUZZER);
  informarConfirmacion();
}

void actualizarLCD(bool forzar, estados estado)
{
    /*
      Esta función imprime por una pantalla LCD el estado en el que se encuentra el SE.
      La impresión por pantalla se realiza según un intervalo de tiempo definido por la constante "TIEMPO_ESCRITURA_LCD".
      El parámetro "forzar" nos permite realizar la impresión aunque no se haya completado el intervalo de tiempo.
      El parámetro "estado" nos permite indicar el estado que debe ser impreso.
    */

    unsigned long tiempo = millis();

    if(!forzar && tiempo - tiempo_lcd < TIEMPO_ESCRITURA_LCD)
    {
      return;
    }

    tiempo_lcd = tiempo;
    lcd.clear();

    switch ( estado )
    {
      case ST_ESTABLE:
      {
        cambiarFondoLCD(VERDE);
        lcd.print("Paciente ");
        lcd.setCursor(0,1);
        lcd.print("estable");
      }
      break;
        
      case ST_PULSADO:
      {
        cambiarFondoLCD(AZUL);
        lcd.print("Paciente ");
        lcd.setCursor(0,1);
        lcd.print("llamando!");
      }
      break;

      case ST_ORINADO:
      {
        cambiarFondoLCD(AMARILLO);
        lcd.print("Paciente ");
        lcd.setCursor(0,1);
        lcd.print("orinado!");
      }
      break;

      case ST_LEVANTADO:
      {
        cambiarFondoLCD(ROJO);
        lcd.print("Paciente ");
        lcd.setCursor(0,1);
        lcd.print("levanto!");
      }
      break;

      case ST_APLAZADO:
      {
        cambiarFondoLCD(BLANCO);
        lcd.print("Aplazado");
      }
      break;

      default:
      break;
    }
    
}

void cambiarFondoLCD(colores color){
  switch (color)
  {
  case VERDE:
    lcd.setRGB(0,255,0);
    break;

  case AZUL:
    lcd.setRGB(0,0,255);
    break;
  
  case AMARILLO:
    lcd.setRGB(255,255,0);
    break;

  case ROJO:
    lcd.setRGB(255,0,0);
    break;

  case ROSA:
    lcd.setRGB(255,0,255);
    break;

  case BLANCO:
    lcd.setRGB(255,255,255);
    break;

  default:
    break;
  }
}

/*
  Las siguientes funciones notifican a través de la consola la situación.
  Estas notificaciones serán enviadas al celular una vez implementada la aplicación Android.
*/

void informarPulsoPaciente()
{
  Serial.println("El paciente ha pulsado el botón de llamada.");

  /*
    {
      "estado": "Paciente Llamo",
    }
  */

  mqttInformarEstado(TOPICO_PULSO, {"estado"}, {"Paciente Llamo"});
}

void informarOrino()
{
  Serial.println("El paciente se ha orinado.");

  /*
    {
      "estado": "Paciente Orino",
      "presion": 512
    }
  */

  mqttInformarEstado(TOPICO_ORINO, {"estado","valorSensor"}, {"Paciente Orino", "X"}, PIN_HUMEDAD);
}

void informarLevanto()
{
  Serial.println("El paciente se ha levantado.");

  /*
    {
      "estado": "Paciente Se Levanto",
      "presion": 1234
    }
  */

  mqttInformarEstado(TOPICO_LEVANTO, {"estado","valorSensor"}, {"Paciente Se Levanto", "X"}, PIN_PRESION);
}

void informarConfirmacion()
{
  Serial.println("Se ha confirmado la llamada del paciente.");

  /*
    {
      "confirmar": "true"
    }
  */

  mqttInformarEstado(TOPICO_CONFIRMADO, {"confirmar"}, {"true"});
}

void informarPausaActuadores()
{
  Serial.println("Se han pausado los actuadores.");
}

/*
  para que se guarde un valor hay que pasarle X como valor, esta función reemplazará esa X por el valor del pin dado
  Solo soporta la lectura de UN SOLO PIN a la vez, y solo hace una lectura analógica
*/
void mqttInformarEstado(const char* topico, std::initializer_list<const char *> claves, std::initializer_list<const char *> valores, int pin)
{    
  String json;

  //Inicialización de variables para guardar texto
  char mensaje[TAMANIO_MENSAJE_MQTT];
  char valorSensor[TAMANIO_STRING_VALOR_SENSOR];
  std::vector<const char*> vClaves;
  std::vector<const char*> vValores;

  //guardamos claves en vector de claves
  for(const char* clave: claves)
  {
    vClaves.push_back(clave);
  }
    
  //Conversión de int a char
  if(pin != -1)
    itoa(analogRead(pin), valorSensor, BASE_STRING_VALOR_SENSOR);

  //guardamos valores en vector de valores
  for(const char* valor: valores)
  {
    //Buscamos el valor con X y lo reemplazamos
    if(pin != -1 && strcmp(valor, "X") == 0)
      vValores.push_back(valorSensor);
    else
      vValores.push_back(valor);
  }

  //Armamos el json y lo guardamos en mensaje
  json = obtenerJson(vClaves, vValores);
  json.toCharArray(mensaje, json.length()+1);

  //Se publica un mensaje en un topico del broker
  Serial.println(mensaje);
  client.publish(topico, mensaje);
}

//Funciones String JSON
String obtenerJson(std::vector<const char*> claves, std::vector<const char*> valores){

  String json;
  auto clave = claves.begin();
  auto valor = valores.begin();

  json = "{\n";
  //while( clave != claves.end() && valor != valores.end() )
  while( clave != claves.end() && valor != valores.end())
    json += "\"" + (String) *clave++ + "\": \"" + (String) *valor++ + ( valor != valores.end() ? "\"," : "\"" ) + "\n";
  json += "}";

  return json;
}

/*
// Funciones JSON
void obtenerJson(const char* clave, const char* valor, char* mensaje){
  strcpy(mensaje, "{\n\"");
  strcat(mensaje, clave);
  strcat(mensaje, "\": ");
  strcat(mensaje, valor);
  strcat(mensaje, "\n}");
}

void intToChar(int valor, char* str){
  itoa(valor, str, BASE_STRING_VALOR_SENSOR);
}

void obtenerJsonValor(char* clave, int pin, char* texto){
  char c[BASE_STRING_VALOR_SENSOR];
  char json[TAMANIO_MENSAJE_MQTT];

  intToChar(analogRead(pin),c);
  obtenerJson(clave, c, json);
  strcpy(texto, json);
}
*/