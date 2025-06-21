/*
 * ESP32 MQTT Client Simple para "Solution-of-Things"
 * Versión simplificada que se conecta a un broker MQTT público
 * 
 * Este sketch permite probar la funcionalidad MQTT de tu app Android
 * conectándose a un broker MQTT público (test.mosquitto.org)
 * 
 * Configuración para la app:
 * - Broker: tcp://test.mosquitto.org:1883
 * - WebSocket: ws://test.mosquitto.org:8080
 * - Tópico: test/topic
 */

#include <WiFi.h>
#include <PubSubClient.h>

// Configuración WiFi - CAMBIA ESTOS VALORES
const char* ssid = "TuWiFi";           // Tu SSID de WiFi
const char* password = "TuPassword";   // Tu contraseña de WiFi

// Configuración MQTT - Broker público para pruebas
const char* mqtt_server = "test.mosquitto.org";
const int mqtt_port = 1883;
const char* client_id = "ESP32Client";

// Tópicos MQTT
const char* topic_test = "test/topic";
const char* topic_response = "test/response";
const char* topic_led = "esp32/led";

// Variables globales
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
int ledState = LOW;
const int ledPin = 2;  // LED integrado del ESP32

void setup() {
  // Inicializar comunicación serial
  Serial.begin(115200);
  Serial.println("\n=== ESP32 MQTT Client Simple ===");
  Serial.println("Para Solution-of-Things App");
  
  // Configurar LED
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, ledState);
  
  // Conectar a WiFi
  setup_wifi();
  
  // Configurar cliente MQTT
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  
  Serial.println("Setup completado");
  Serial.println("Esperando conexión MQTT...");
}

void setup_wifi() {
  Serial.print("Conectando a WiFi: ");
  Serial.println(ssid);
  
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println();
  Serial.println("WiFi conectado");
  Serial.print("Dirección IP: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido en [");
  Serial.print(topic);
  Serial.print("] ");
  
  // Convertir payload a string
  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  Serial.println(message);
  
  // Procesar mensaje según el tópico
  if (String(topic) == topic_test) {
    handleTestMessage(message);
  } else if (String(topic) == topic_led) {
    handleLedMessage(message);
  }
}

void handleTestMessage(String message) {
  Serial.println("Procesando mensaje de prueba...");
  
  // Crear respuesta
  String response = "ESP32 recibió: " + message;
  
  // Publicar respuesta
  if (client.publish(topic_response, response.c_str())) {
    Serial.println("Respuesta publicada: " + response);
  } else {
    Serial.println("Error al publicar respuesta");
  }
  
  // Cambiar estado del LED para indicar actividad
  ledState = !ledState;
  digitalWrite(ledPin, ledState);
}

void handleLedMessage(String message) {
  Serial.print("Comando LED recibido: ");
  Serial.println(message);
  
  if (message.equalsIgnoreCase("ON") || message.equalsIgnoreCase("1")) {
    ledState = HIGH;
    digitalWrite(ledPin, ledState);
    Serial.println("LED encendido");
  } else if (message.equalsIgnoreCase("OFF") || message.equalsIgnoreCase("0")) {
    ledState = LOW;
    digitalWrite(ledPin, ledState);
    Serial.println("LED apagado");
  } else if (message.equalsIgnoreCase("TOGGLE")) {
    ledState = !ledState;
    digitalWrite(ledPin, ledState);
    Serial.println("LED alternado");
  }
  
  // Publicar estado actual del LED
  String ledStatus = ledState ? "ON" : "OFF";
  client.publish("esp32/led/status", ledStatus.c_str());
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    
    if (client.connect(client_id)) {
      Serial.println("Conectado");
      
      // Suscribirse a tópicos
      client.subscribe(topic_test);
      client.subscribe(topic_led);
      
      Serial.println("Suscrito a tópicos:");
      Serial.println("  - " + String(topic_test));
      Serial.println("  - " + String(topic_led));
      
    } else {
      Serial.print("Error, rc=");
      Serial.print(client.state());
      Serial.println(" reintentando en 5 segundos");
      delay(5000);
    }
  }
}

void loop() {
  // Verificar conexión MQTT
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
  
  unsigned long now = millis();
  
  // Publicar mensaje periódico cada 10 segundos
  if (now - lastMsg > 10000) {
    lastMsg = now;
    
    // Crear mensaje con timestamp
    String message = "Mensaje periódico desde ESP32 - " + String(now);
    
    if (client.publish(topic_test, message.c_str())) {
      Serial.println("Mensaje periódico enviado: " + message);
    } else {
      Serial.println("Error al enviar mensaje periódico");
    }
  }
  
  delay(100);
}

/*
 * INSTRUCCIONES DE USO:
 * 
 * 1. CAMBIAR CONFIGURACIÓN WIFI:
 *    - Reemplaza "TuWiFi" con tu SSID
 *    - Reemplaza "TuPassword" con tu contraseña
 * 
 * 2. INSTALAR BIBLIOTECA:
 *    - Ve a: Herramientas > Gestor de Bibliotecas
 *    - Busca "PubSubClient"
 *    - Instala la versión de Nick O'Leary
 * 
 * 3. CONFIGURAR LA APP:
 *    - Broker: tcp://test.mosquitto.org:1883
 *    - WebSocket: ws://test.mosquitto.org:8080
 *    - Cliente ID: AndroidClient
 *    - Tópico: test/topic
 * 
 * 4. PROBAR:
 *    - Sube el código al ESP32
 *    - Abre el monitor serial (115200 baudios)
 *    - En la app, conecta al broker
 *    - Publica mensajes en "test/topic"
 *    - Suscríbete a "test/response" para ver respuestas
 * 
 * 5. COMANDOS LED:
 *    - Publicar en "esp32/led": "ON", "OFF", "TOGGLE"
 *    - El LED integrado del ESP32 cambiará de estado
 * 
 * NOTA: Este broker público es solo para pruebas.
 * Para uso en producción, usa un broker privado.
 */ 