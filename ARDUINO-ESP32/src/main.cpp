#include <estado.h>
#include <WiFi.h>

#define BAUDRATE      9600

void setup() {
  Serial.begin(BAUDRATE);

  Wire.begin(PIN_LCD_SDA, PIN_LCD_SCL);  
  lcd.begin(LCD_COLUMNAS, LCD_FILAS);

  // Configuracion inicial del LCD
  lcd.clear();
  lcd.setCursor(LCD_COLUMNA_INICIAL, LCD_FILA_INICIAL);
  lcd.print("Iniciando");
  lcd.setCursor(0,1);
  lcd.print("SmartCare Alert");

  //Configuración de los pulsadores  
  pulsadorLlamar.pin = PIN_PULSADOR;
  pulsadorAplazar.pin = PIN_PULSADOR_APLAZAR;
  pulsadorConfirmar.pin = PIN_PULSADOR_CONFIRMAR;

  //Configuramos los sensores
  pinMode(PIN_PRESION, INPUT);
  pinMode(PIN_HUMEDAD, INPUT);
  pinMode(PIN_PULSADOR, INPUT_PULLUP);
  pinMode(PIN_PULSADOR_APLAZAR, INPUT_PULLUP);
  pinMode(PIN_PULSADOR_CONFIRMAR, INPUT_PULLUP);

  //Configuramos los actuadores
  pinMode(PIN_BUZZER, OUTPUT);

  //Informamos por Serial que se está conectando a la red wifi
  Serial.print("Conectandose a: ");
  Serial.println(ssid);

  //Informamos por el lcd que se está conectando a la red wifi
  lcd.clear();
  cambiarFondoLCD(ROSA);
  lcd.print("Conectandose");
  lcd.setCursor(0,1);
  lcd.print("a ");
  lcd.print(ssid);

  wifiConnect();

  Serial.println("");
  Serial.println("WiFi Conectado");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  Serial.println(WiFi.macAddress());
  stMac = WiFi.macAddress();
  stMac.replace(":", "_");
  Serial.println(stMac);
  
  client.setServer(mqttServer, port);
  client.setCallback(callback);
  pinMode(2, OUTPUT);
}

void wifiConnect()  
{
  WiFi.begin(ssid, password);
  
  //Este while es necesario para que la placa 
  //pueda conectarse adecuadamente a la red wifi
  while (WiFi.status() != WL_CONNECTED) 
  {
    delay(500);
    Serial.print(".");
  }
}

//Funcion que reconecta el cliente, si pierde la conexion
void mqttReconnect() 
{
    if(!client.connected())
    {
      Serial.print("Intentando conexión MQTT...");
      long r = random(1000);
      sprintf(clientId, "clientId-%ld", r);
      if (client.connect(clientId,user_name,user_pass)) 
	  {
        Serial.print(clientId);
        Serial.println(" conectado");
        Serial.println("envio");
        client.subscribe(TOPICO_APLAZO);
      } else 
	  {
        Serial.print("fallo, rc=");
        Serial.print(client.state());
      }
  }
}

void loop() {
  if(nuevo_evento != EV_CONTINUE) //Esto es para evitar spam
    Serial.println("Estado: "+estados_string[estado_actual]+"  Evento: "+eventos_string[nuevo_evento]);

  fsm();

  if (!client.connected()) 
  {
    mqttReconnect();
  }
  client.loop();
} 