/*
 * ESP32 MQTT Broker Test para "Solution-of-Things"
 * Compatible con la funcionalidad "Mosquito Prueba" de la app Android
 * 
 * Este sketch configura un ESP32 como broker MQTT que puede:
 * - Recibir mensajes de la app Android
 * - Responder a los mensajes recibidos
 * - Publicar mensajes periódicos
 * - Mostrar estado en el monitor serial
 * 
 * Configuración por defecto de la app:
 * - Broker: tcp://192.168.4.1:1883
 * - WebSocket: ws://192.168.4.1:2883
 * - Tópico: test/topic
 * - Mensaje: Hola ESP32!
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// Configuración WiFi
const char* ssid = "TuWiFi";           // Cambia por tu SSID
const char* password = "TuPassword";   // Cambia por tu contraseña

// Configuración MQTT
const char* mqtt_server = "192.168.4.1";
const int mqtt_port = 1883;
const char* client_id = "ESP32Client";

// Tópicos MQTT
const char* topic_test = "test/topic";
const char* topic_response = "test/response";
const char* topic_status = "esp32/status";
const char* topic_led = "esp32/led";

// Variables globales
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
unsigned long lastStatus = 0;
int ledState = LOW;
const int ledPin = 2;  // LED integrado del ESP32

// Configuración de timing
const long interval = 5000;      // Intervalo para mensajes periódicos (5 segundos)
const long statusInterval = 10000; // Intervalo para estado (10 segundos)

void setup() {
  // Inicializar comunicación serial
  Serial.begin(115200);
  Serial.println("\n=== ESP32 MQTT Broker Test ===");
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
  Serial.print("Dirección MAC: ");
  Serial.println(WiFi.macAddress());
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
  } else {
    Serial.println("Tópico no reconocido");
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
  } else {
    Serial.println("Comando LED no válido");
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
      
      // Publicar mensaje de conexión
      client.publish(topic_status, "ESP32 conectado y listo");
      
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
  
  // Publicar mensaje periódico cada 5 segundos
  if (now - lastMsg > interval) {
    lastMsg = now;
    
    // Crear mensaje con timestamp
    String message = "Mensaje periódico desde ESP32 - " + String(now);
    
    if (client.publish(topic_test, message.c_str())) {
      Serial.println("Mensaje periódico enviado: " + message);
    } else {
      Serial.println("Error al enviar mensaje periódico");
    }
  }
  
  // Publicar estado del sistema cada 10 segundos
  if (now - lastStatus > statusInterval) {
    lastStatus = now;
    
    // Crear JSON con información del sistema
    StaticJsonDocument<200> doc;
    doc["timestamp"] = now;
    doc["uptime"] = millis();
    doc["free_heap"] = ESP.getFreeHeap();
    doc["wifi_rssi"] = WiFi.RSSI();
    doc["led_state"] = ledState ? "ON" : "OFF";
    
    String statusJson;
    serializeJson(doc, statusJson);
    
    if (client.publish(topic_status, statusJson.c_str())) {
      Serial.println("Estado del sistema publicado");
    } else {
      Serial.println("Error al publicar estado");
    }
  }
  
  // Pequeño delay para estabilidad
  delay(100);
}

/*
 * INSTRUCCIONES DE USO:
 * 
 * 1. CAMBIAR CONFIGURACIÓN WIFI:
 *    - Reemplaza "TuWiFi" con tu SSID
 *    - Reemplaza "TuPassword" con tu contraseña
 * 
 * 2. INSTALAR BIBLIOTECAS (en Arduino IDE):
 *    - PubSubClient (por Nick O'Leary)
 *    - ArduinoJson (por Benoit Blanchon)
 * 
 * 3. CONFIGURAR PubSubClient:
 *    - Ve a: Herramientas > Gestor de Bibliotecas
 *    - Busca "PubSubClient"
 *    - Instala la versión de Nick O'Leary
 *    - En PubSubClient.h, cambia MQTT_MAX_PACKET_SIZE a 512
 * 
 * 4. CONFIGURAR LA APP:
 *    - Broker: tcp://[IP_DEL_ESP32]:1883
 *    - Cliente ID: AndroidClient
 *    - Tópico: test/topic
 * 
 * 5. COMANDOS DISPONIBLES:
 *    - Publicar en "test/topic": Cualquier mensaje
 *    - Publicar en "esp32/led": "ON", "OFF", "TOGGLE"
 * 
 * 6. TÓPICOS DE RESPUESTA:
 *    - "test/response": Respuestas a mensajes de prueba
 *    - "esp32/status": Estado del sistema
 *    - "esp32/led/status": Estado actual del LED
 * 
 * 7. MONITOR SERIAL:
 *    - Configurar a 115200 baudios
 *    - Ver mensajes de debug y estado
 */ 