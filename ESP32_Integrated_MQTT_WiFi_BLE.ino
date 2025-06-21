/*
 * ESP32 Integrado: WiFi + BLE + MQTT para "Solution-of-Things"
 * 
 * Este sketch combina:
 * - Configuración WiFi vía BLE y servidor web
 * - Funcionalidad MQTT para comunicación con app Android
 * - Control de LED y respuestas automáticas
 * - Gestión de credenciales con Preferences
 * 
 * Compatible con la app "Solution-of-Things" - "Mosquito Prueba"
 */

#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>
#include <ArduinoJson.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <PubSubClient.h>

// Configuración de pines
#define LED_PIN 2
#define BOOT_BUTTON 0

// Configuración BLE
#define SERVICE_UUID        "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define CHARACTERISTIC_UUID "6e400002-b5a3-f393-e0a9-e50e24dcca9e"

// Configuración MQTT
const char* mqtt_server = "test.mosquitto.org";  // Broker público por defecto
const int mqtt_port = 1883;
const char* client_id = "PetFeeder_ESP32";

// Tópicos MQTT
const char* topic_test = "test/topic";
const char* topic_response = "test/response";
const char* topic_led = "esp32/led";
const char* topic_status = "petfeeder/status";
const char* topic_config = "petfeeder/config";

// Variables globales
WebServer server(80);
Preferences prefs;
WiFiClient espClient;
PubSubClient mqttClient(espClient);

bool apMode = false;
bool wifiConnected = false;
bool mqttConnected = false;
int ledState = LOW;

unsigned long lastPressTime = 0;
int pressCounter = 0;
const unsigned long resetWindow = 5000;

unsigned long lastMqttMsg = 0;
unsigned long lastStatusMsg = 0;
const long mqttInterval = 10000;      // 10 segundos
const long statusInterval = 30000;    // 30 segundos

// Función para parpadear LED
void blinkLED() {
  static unsigned long lastBlink = 0;
  static bool state = false;
  if (millis() - lastBlink >= 500) {
    lastBlink = millis();
    state = !state;
    digitalWrite(LED_PIN, state ? HIGH : LOW);
  }
}

// Callback MQTT
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("📨 MQTT recibido en [");
  Serial.print(topic);
  Serial.print("] ");
  
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
  } else if (String(topic) == topic_config) {
    handleConfigMessage(message);
  }
}

// Manejar mensajes de prueba
void handleTestMessage(String message) {
  Serial.println("🔄 Procesando mensaje de prueba...");
  
  String response = "PetFeeder ESP32 recibió: " + message;
  
  if (mqttClient.publish(topic_response, response.c_str())) {
    Serial.println("✅ Respuesta publicada: " + response);
  } else {
    Serial.println("❌ Error al publicar respuesta");
  }
  
  // Cambiar estado del LED para indicar actividad
  ledState = !ledState;
  digitalWrite(LED_PIN, ledState);
}

// Manejar comandos de LED
void handleLedMessage(String message) {
  Serial.print("💡 Comando LED recibido: ");
  Serial.println(message);
  
  if (message.equalsIgnoreCase("ON") || message.equalsIgnoreCase("1")) {
    ledState = HIGH;
    digitalWrite(LED_PIN, ledState);
    Serial.println("💡 LED encendido");
  } else if (message.equalsIgnoreCase("OFF") || message.equalsIgnoreCase("0")) {
    ledState = LOW;
    digitalWrite(LED_PIN, ledState);
    Serial.println("💡 LED apagado");
  } else if (message.equalsIgnoreCase("TOGGLE")) {
    ledState = !ledState;
    digitalWrite(LED_PIN, ledState);
    Serial.println("💡 LED alternado");
  } else if (message.equalsIgnoreCase("BLINK")) {
    // Parpadear LED por 3 segundos
    for (int i = 0; i < 6; i++) {
      digitalWrite(LED_PIN, HIGH);
      delay(250);
      digitalWrite(LED_PIN, LOW);
      delay(250);
    }
    Serial.println("💡 LED parpadeó");
  }
  
  // Publicar estado actual del LED
  String ledStatus = ledState ? "ON" : "OFF";
  mqttClient.publish("petfeeder/led/status", ledStatus.c_str());
}

// Manejar mensajes de configuración
void handleConfigMessage(String message) {
  Serial.println("⚙️ Mensaje de configuración recibido: " + message);
  
  String response = "Configuración recibida: " + message;
  mqttClient.publish("petfeeder/config/response", response.c_str());
}

// Conectar a MQTT
void connectMQTT() {
  if (!wifiConnected) {
    Serial.println("⚠️ No hay WiFi para conectar MQTT");
    return;
  }
  
  Serial.print("🔗 Conectando a MQTT...");
  
  if (mqttClient.connect(client_id)) {
    Serial.println("✅ Conectado a MQTT");
    mqttConnected = true;
    
    // Suscribirse a tópicos
    mqttClient.subscribe(topic_test);
    mqttClient.subscribe(topic_led);
    mqttClient.subscribe(topic_config);
    
    Serial.println("📡 Suscrito a tópicos:");
    Serial.println("  - " + String(topic_test));
    Serial.println("  - " + String(topic_led));
    Serial.println("  - " + String(topic_config));
    
    // Publicar mensaje de conexión
    mqttClient.publish(topic_status, "PetFeeder ESP32 conectado y listo");
    
    // Encender LED para indicar conexión MQTT
    digitalWrite(LED_PIN, HIGH);
    delay(1000);
    digitalWrite(LED_PIN, ledState);
    
  } else {
    Serial.print("❌ Error MQTT, rc=");
    Serial.print(mqttClient.state());
    Serial.println(" reintentando en 5 segundos");
    mqttConnected = false;
  }
}

// Manejar configuración WiFi vía servidor web
void handleWiFiConfig() {
  if (server.method() != HTTP_POST) {
    server.send(405, "text/plain", "Método no permitido");
    return;
  }

  StaticJsonDocument<256> doc;
  DeserializationError error = deserializeJson(doc, server.arg("plain"));
  if (error) {
    server.send(400, "application/json", "{\"status\":\"json inválido\"}");
    return;
  }

  const char* ssid = doc["ssid"];
  const char* password = doc["password"];

  Serial.println("📡 Recibido:");
  Serial.print("  SSID: "); Serial.println(ssid);
  Serial.print("  PASS: "); Serial.println(password);

  prefs.begin("wifi-creds", false);
  prefs.putString("ssid", ssid);
  prefs.putString("pass", password);
  prefs.end();

  server.send(200, "application/json", "{\"status\":\"ok\"}");
  delay(2000);
  ESP.restart();
}

// Iniciar modo Access Point
void iniciarModoAP() {
  apMode = true;
  WiFi.softAP("PetFeeder_Setup_EA9E");
  IPAddress IP = WiFi.softAPIP();
  Serial.print("📶 AP activo en: "); Serial.println(IP);

  server.on("/wifi", HTTP_POST, handleWiFiConfig);
  server.begin();
  Serial.println("🌐 Servidor web iniciado");
  pinMode(LED_PIN, OUTPUT);
}

// Callback BLE
class StartAPCallback : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    String command = String(pCharacteristic->getValue().c_str());
    Serial.print("📥 BLE recibido: ");
    Serial.println(command);

    if (command == "start_ap") {
      Serial.println("🟡 Comando válido: start_ap → iniciando AP");
      iniciarModoAP();
    }
  }
};

// Iniciar BLE
void iniciarBLE() {
  Serial.println("🔵 Iniciando BLE...");
  BLEDevice::init("PetFeeder BLE");

  BLEServer* pServer = BLEDevice::createServer();
  BLEService* pService = pServer->createService(SERVICE_UUID);

  BLECharacteristic* pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_WRITE
  );

  pCharacteristic->setCallbacks(new StartAPCallback());
  pCharacteristic->addDescriptor(new BLE2902());

  pService->start();

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->start();

  Serial.println("📡 BLE listo. Esperando comando...");
}

// Intentar conexión WiFi
void intentarConexionWiFi() {
  prefs.begin("wifi-creds", true);
  String ssid = prefs.getString("ssid", "");
  String pass = prefs.getString("pass", "");
  prefs.end();

  if (ssid == "") {
    Serial.println("⚠️ No hay WiFi guardado. Esperando comando BLE...");
    return;
  }

  Serial.print("🔗 Conectando a WiFi: ");
  Serial.println(ssid);

  WiFi.begin(ssid.c_str(), pass.c_str());
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
    delay(500); 
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {
    wifiConnected = true;
    Serial.println("\n✅ Conectado a WiFi");
    Serial.print("IP: "); Serial.println(WiFi.localIP());
    digitalWrite(LED_PIN, HIGH);
    
    // Configurar MQTT después de conectar WiFi
    mqttClient.setServer(mqtt_server, mqtt_port);
    mqttClient.setCallback(mqttCallback);
    connectMQTT();
    
  } else {
    Serial.println("\n❌ Falló la conexión. Esperando BLE...");
  }
}

// Publicar estado del sistema
void publishSystemStatus() {
  if (!mqttConnected) return;
  
  StaticJsonDocument<300> doc;
  doc["device"] = "PetFeeder_ESP32";
  doc["timestamp"] = millis();
  doc["uptime"] = millis();
  doc["free_heap"] = ESP.getFreeHeap();
  doc["wifi_rssi"] = WiFi.RSSI();
  doc["led_state"] = ledState ? "ON" : "OFF";
  doc["wifi_connected"] = wifiConnected;
  doc["mqtt_connected"] = mqttConnected;
  doc["ap_mode"] = apMode;
  
  String statusJson;
  serializeJson(doc, statusJson);
  
  if (mqttClient.publish(topic_status, statusJson.c_str())) {
    Serial.println("📊 Estado del sistema publicado");
  } else {
    Serial.println("❌ Error al publicar estado");
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println("\n=== PetFeeder ESP32 Integrado ===");
  Serial.println("WiFi + BLE + MQTT para Solution-of-Things");
  
  pinMode(LED_PIN, OUTPUT);
  pinMode(BOOT_BUTTON, INPUT_PULLUP);
  digitalWrite(LED_PIN, LOW);

  intentarConexionWiFi();
  if (!wifiConnected) {
    iniciarBLE();
  }
  
  Serial.println("🚀 Setup completado");
}

void loop() {
  // Detectar 3 pulsaciones al botón BOOT
  if (digitalRead(BOOT_BUTTON) == LOW) {
    delay(50);
    if (digitalRead(BOOT_BUTTON) == LOW) {
      unsigned long now = millis();
      if (now - lastPressTime < resetWindow) {
        pressCounter++;
      } else {
        pressCounter = 1;
      }
      lastPressTime = now;
      Serial.println("🔘 BOOT presionado. Conteo: " + String(pressCounter));
      delay(500);
    }
  }

  if (pressCounter >= 3) {
    Serial.println("⚠️ 3 pulsaciones detectadas → Borrando WiFi...");
    prefs.begin("wifi-creds", false);
    prefs.clear();
    prefs.end();

    pressCounter = 0;
    delay(1000);
    ESP.restart();
  }

  // Manejar servidor web si está en modo AP
  if (apMode) {
    server.handleClient();
    if (!wifiConnected) blinkLED();
  }

  // Manejar MQTT si está conectado a WiFi
  if (wifiConnected) {
    // Reconectar MQTT si se perdió la conexión
    if (!mqttClient.connected()) {
      mqttConnected = false;
      connectMQTT();
    } else {
      mqttClient.loop();
      
      unsigned long now = millis();
      
      // Publicar mensaje periódico cada 10 segundos
      if (now - lastMqttMsg > mqttInterval) {
        lastMqttMsg = now;
        
        String message = "PetFeeder ESP32 - Mensaje periódico - " + String(now);
        
        if (mqttClient.publish(topic_test, message.c_str())) {
          Serial.println("📤 Mensaje periódico enviado: " + message);
        } else {
          Serial.println("❌ Error al enviar mensaje periódico");
        }
      }
      
      // Publicar estado del sistema cada 30 segundos
      if (now - lastStatusMsg > statusInterval) {
        lastStatusMsg = now;
        publishSystemStatus();
      }
    }
  }
  
  delay(100);
}

/*
 * INSTRUCCIONES DE USO:
 * 
 * 1. CONFIGURACIÓN INICIAL:
 *    - Presiona 3 veces el botón BOOT para borrar WiFi
 *    - Usa BLE o servidor web para configurar WiFi
 *    - El ESP32 se conectará automáticamente a MQTT
 * 
 * 2. CONFIGURAR LA APP ANDROID:
 *    - Broker: tcp://test.mosquitto.org:1883
 *    - Cliente ID: AndroidClient
 *    - Tópico: test/topic
 * 
 * 3. FUNCIONALIDADES MQTT:
 *    - Recibe mensajes en "test/topic"
 *    - Responde en "test/response"
 *    - Control LED en "esp32/led" (ON/OFF/TOGGLE/BLINK)
 *    - Estado en "petfeeder/status"
 *    - Configuración en "petfeeder/config"
 * 
 * 4. INDICADORES LED:
 *    - Parpadeo lento: Modo AP activo
 *    - LED fijo: WiFi conectado
 *    - LED apagado: Sin conexión
 * 
 * 5. COMANDOS BLE:
 *    - "start_ap": Inicia modo Access Point
 * 
 * 6. RESET:
 *    - 3 pulsaciones rápidas del botón BOOT
 *    - Borra credenciales WiFi y reinicia
 */ 