/*
 * ESP32 PetFeeder Completo: WiFi + BLE + MQTT + Servo Motor
 * 
 * Este sketch combina:
 * - Configuración WiFi vía BLE y servidor web
 * - Funcionalidad MQTT para comunicación con app Android
 * - Control de servo motor para dispensar comida
 * - Control de LED y respuestas automáticas
 * - Gestión de credenciales con Preferences
 * 
 * Compatible con la app "MyPaws" - "Mosquito Prueba"
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
#include <ESP32Servo.h>

// Configuración de pines
#define LED_PIN 2
#define BOOT_BUTTON 0
#define SERVO_PIN 13  // Pin para el servo motor

// Configuración BLE
#define SERVICE_UUID        "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define CHARACTERISTIC_UUID "6e400002-b5a3-f393-e0a9-e50e24dcca9e"

// Configuración MQTT
const char* mqtt_server = "test.mosquitto.org";
const int mqtt_port = 1883;
String clientId = "PetFeeder_ESP32_";

// Tópicos MQTT
const char* topic_test = "test/topic";
const char* topic_response = "test/response";
const char* topic_led = "esp32/led";
const char* topic_status = "petfeeder/status";
const char* topic_config = "petfeeder/config";
const char* topic_feeder = "petfeeder/control";  // Nuevo tópico para control del comedero
const char* topic_feeder_status = "petfeeder/feeder/status";  // Estado del comedero

// Variables globales
WebServer server(80);
Preferences prefs;
WiFiClient espClient;
PubSubClient mqttClient(espClient);
Servo feederServo;  // Objeto servo para el comedero

bool apMode = false;
bool wifiConnected = false;
bool mqttConnected = false;
bool bleActive = false;
int ledState = LOW;

// Variables del comedero
int servoPosition = 0;      // Posición actual del servo (0-180)
int lastServoPosition = 0;  // Última posición del servo
bool feederBusy = false;    // Si el comedero está en movimiento

unsigned long lastPressTime = 0;
int pressCounter = 0;
const unsigned long resetWindow = 5000;

unsigned long lastMqttMsg = 0;
unsigned long lastStatusMsg = 0;
const long mqttInterval = 10000;      // 10 segundos
const long statusInterval = 30000;    // 30 segundos

// Configuración del servo
const int SERVO_MIN_POS = 0;    // Posición cerrada
const int SERVO_MAX_POS = 90;   // Posición abierta (ajustar según tu comedero)
const int SERVO_DELAY = 1000;   // Tiempo que permanece abierto (ms)

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

// Función para dispensar comida
void dispenseFood(int portions = 1) {
  if (feederBusy) {
    Serial.println("⚠️ Comedero ocupado, no se puede dispensar ahora");
    mqttClient.publish(topic_feeder_status, "BUSY");
    return;
  }
  
  feederBusy = true;
  Serial.print("🍽️ Dispensando ");
  Serial.print(portions);
  Serial.println(" porción(es) de comida");
  
  // Publicar estado
  mqttClient.publish(topic_feeder_status, "DISPENSING");
  
  // Parpadear LED durante la dispensación
  for (int i = 0; i < portions; i++) {
    Serial.print("  Porción ");
    Serial.print(i + 1);
    Serial.println("...");
    
    // Abrir comedero
    feederServo.write(SERVO_MAX_POS);
    delay(SERVO_DELAY);
    
    // Cerrar comedero
    feederServo.write(SERVO_MIN_POS);
    delay(500);  // Pausa entre porciones
    
    // Parpadear LED
    digitalWrite(LED_PIN, HIGH);
    delay(200);
    digitalWrite(LED_PIN, LOW);
    delay(200);
  }
  
  feederBusy = false;
  Serial.println("✅ Comida dispensada correctamente");
  
  // Publicar estado final
  mqttClient.publish(topic_feeder_status, "READY");
  
  // Publicar confirmación
  String confirmMsg = "Comida dispensada: " + String(portions) + " porción(es)";
  mqttClient.publish(topic_response, confirmMsg.c_str());
}

// Función para mover servo a posición específica
void moveServoTo(int position) {
  if (position < SERVO_MIN_POS || position > SERVO_MAX_POS) {
    Serial.println("⚠️ Posición inválida para el servo");
    return;
  }
  
  Serial.print("🔧 Moviendo servo a posición: ");
  Serial.println(position);
  
  feederServo.write(position);
  servoPosition = position;
  
  String statusMsg = "Servo en posición: " + String(position);
  mqttClient.publish(topic_feeder_status, statusMsg.c_str());
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
  } else if (String(topic) == topic_feeder) {
    handleFeederMessage(message);
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

// Manejar comandos del comedero
void handleFeederMessage(String message) {
  Serial.print("🍽️ Comando comedero recibido: ");
  Serial.println(message);
  
  message.toUpperCase();  // Convertir a mayúsculas para comparación
  
  if (message.equalsIgnoreCase("FEED") || message.equalsIgnoreCase("DISPENSE")) {
    dispenseFood(1);  // Una porción por defecto
  } else if (message.startsWith("FEED:")) {
    // Formato: FEED:2 (dispensar 2 porciones)
    int portions = message.substring(5).toInt();
    if (portions > 0 && portions <= 5) {  // Máximo 5 porciones por seguridad
      dispenseFood(portions);
    } else {
      Serial.println("⚠️ Número de porciones inválido (1-5)");
      mqttClient.publish(topic_feeder_status, "ERROR: Invalid portions");
    }
  } else if (message.equalsIgnoreCase("OPEN")) {
    moveServoTo(SERVO_MAX_POS);
  } else if (message.equalsIgnoreCase("CLOSE")) {
    moveServoTo(SERVO_MIN_POS);
  } else if (message.equalsIgnoreCase("STATUS")) {
    String status = feederBusy ? "BUSY" : "READY";
    status += " | Servo: " + String(servoPosition);
    mqttClient.publish(topic_feeder_status, status.c_str());
  } else if (message.startsWith("SERVO:")) {
    // Formato: SERVO:45 (mover servo a posición 45)
    int position = message.substring(6).toInt();
    moveServoTo(position);
  } else {
    Serial.println("⚠️ Comando de comedero no reconocido");
    mqttClient.publish(topic_feeder_status, "ERROR: Unknown command");
  }
}

// Conectar a MQTT
void connectMQTT() {
  if (!wifiConnected) {
    Serial.println("⚠️ No hay WiFi para conectar MQTT");
    return;
  }
  
  Serial.print("🔗 Conectando a MQTT con Client ID: ");
  Serial.println(clientId);
  
  if (mqttClient.connect(clientId.c_str())) {
    Serial.println("✅ Conectado a MQTT");
    mqttConnected = true;
    
    // Suscribirse a tópicos
    mqttClient.subscribe(topic_test);
    mqttClient.subscribe(topic_led);
    mqttClient.subscribe(topic_config);
    mqttClient.subscribe(topic_feeder);  // Suscribirse al tópico del comedero
    
    Serial.println("📡 Suscrito a tópicos:");
    Serial.println("  - " + String(topic_test));
    Serial.println("  - " + String(topic_led));
    Serial.println("  - " + String(topic_config));
    Serial.println("  - " + String(topic_feeder));
    
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

void setup() {
  Serial.begin(115200);
  Serial.println("\n🚀 Iniciando PetFeeder ESP32...");
  
  // Configurar pines
  pinMode(LED_PIN, OUTPUT);
  pinMode(BOOT_BUTTON, INPUT);
  
  // Inicializar servo
  ESP32PWM::allocateTimer(0);
  feederServo.setPeriodHertz(50);  // 50Hz para servo estándar
  feederServo.attach(SERVO_PIN);
  feederServo.write(SERVO_MIN_POS);  // Posición inicial cerrada
  servoPosition = SERVO_MIN_POS;
  
  Serial.println("🔧 Servo inicializado en posición cerrada");
  
  // Inicializar Preferences
  prefs.begin("petfeeder", false);
  
  // Configurar MQTT
  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(mqttCallback);
  
  // Generar Client ID único
  clientId += String(random(0xffff), HEX);
  
  // Verificar botón de reset
  if (digitalRead(BOOT_BUTTON) == LOW) {
    Serial.println("🔄 Botón BOOT presionado - Reseteando configuración WiFi");
    prefs.clear();
    delay(1000);
  }
  
  // Intentar conectar WiFi
  String ssid = prefs.getString("wifi_ssid", "");
  String password = prefs.getString("wifi_password", "");
  
  if (ssid.length() > 0) {
    Serial.println("📶 Intentando conectar a WiFi guardado...");
    WiFi.begin(ssid.c_str(), password.c_str());
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
      delay(500);
      Serial.print(".");
      attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
      wifiConnected = true;
      Serial.println("\n✅ Conectado a WiFi");
      Serial.print("📡 IP: ");
      Serial.println(WiFi.localIP());
      
      // Conectar MQTT
      connectMQTT();
      
      // Iniciar servidor web
      setupWebServer();
      server.begin();
      
    } else {
      Serial.println("\n❌ Fallo al conectar WiFi - Iniciando modo AP");
      startAPMode();
    }
  } else {
    Serial.println("📶 No hay credenciales WiFi - Iniciando modo AP");
    startAPMode();
  }
  
  Serial.println("🎯 PetFeeder ESP32 listo");
}

void loop() {
  // Manejar botón BOOT para reset
  handleBootButton();
  
  // Manejar MQTT
  if (wifiConnected && mqttConnected) {
    mqttClient.loop();
    
    // Publicar estado periódicamente
    if (millis() - lastStatusMsg > statusInterval) {
      lastStatusMsg = millis();
      
      String statusMsg = "PetFeeder Status | WiFi: " + String(wifiConnected ? "ON" : "OFF") +
                        " | MQTT: " + String(mqttConnected ? "ON" : "OFF") +
                        " | Servo: " + String(servoPosition) +
                        " | Busy: " + String(feederBusy ? "YES" : "NO");
      
      mqttClient.publish(topic_status, statusMsg.c_str());
    }
  }
  
  // Manejar servidor web si está en modo AP
  if (apMode) {
    server.handleClient();
    blinkLED();
  }
  
  // Reconectar MQTT si se perdió la conexión
  if (wifiConnected && !mqttConnected) {
    Serial.println("🔄 Reconectando MQTT...");
    connectMQTT();
    delay(5000);
  }
  
  delay(100);
}

// Función para manejar el botón BOOT
void handleBootButton() {
  if (digitalRead(BOOT_BUTTON) == LOW) {
    unsigned long currentTime = millis();
    
    if (currentTime - lastPressTime > resetWindow) {
      pressCounter = 1;
    } else {
      pressCounter++;
    }
    
    lastPressTime = currentTime;
    
    if (pressCounter >= 3) {
      Serial.println("🔄 Reset de configuración WiFi");
      prefs.clear();
      ESP.restart();
    }
  }
}

// Función para iniciar modo AP
void startAPMode() {
  apMode = true;
  
  // Configurar AP
  WiFi.mode(WIFI_AP);
  WiFi.softAP("PetFeeder_Setup", "12345678");
  
  Serial.println("📶 Modo AP iniciado");
  Serial.print("📡 SSID: PetFeeder_Setup");
  Serial.print(" | Contraseña: 12345678");
  Serial.print(" | IP: ");
  Serial.println(WiFi.softAPIP());
  
  // Iniciar servidor web
  setupWebServer();
  server.begin();
  
  // Iniciar BLE
  startBLE();
}

// Función para configurar servidor web
void setupWebServer() {
  server.on("/", HTTP_GET, []() {
    String html = R"(
<!DOCTYPE html>
<html>
<head>
    <title>PetFeeder WiFi Setup</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .container { max-width: 400px; margin: 0 auto; }
        input, button { width: 100%; padding: 10px; margin: 5px 0; box-sizing: border-box; }
        button { background: #4CAF50; color: white; border: none; cursor: pointer; }
        button:hover { background: #45a049; }
        .status { padding: 10px; margin: 10px 0; border-radius: 5px; }
        .success { background: #d4edda; color: #155724; }
        .error { background: #f8d7da; color: #721c24; }
    </style>
</head>
<body>
    <div class="container">
        <h2>🍽️ PetFeeder WiFi Setup</h2>
        <form id="wifiForm">
            <input type="text" id="ssid" placeholder="Nombre de red WiFi" required>
            <input type="password" id="password" placeholder="Contraseña WiFi" required>
            <button type="submit">Conectar</button>
        </form>
        <div id="status"></div>
    </div>
    <script>
        document.getElementById('wifiForm').onsubmit = function(e) {
            e.preventDefault();
            const ssid = document.getElementById('ssid').value;
            const password = document.getElementById('password').value;
            
            fetch('/wifi', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: 'ssid=' + encodeURIComponent(ssid) + '&password=' + encodeURIComponent(password)
            })
            .then(response => response.text())
            .then(data => {
                document.getElementById('status').innerHTML = '<div class="success">' + data + '</div>';
                if (data.includes('Conectado')) {
                    setTimeout(() => {
                        window.location.href = 'http://192.168.4.1/status';
                    }, 3000);
                }
            })
            .catch(error => {
                document.getElementById('status').innerHTML = '<div class="error">Error: ' + error + '</div>';
            });
        };
    </script>
</body>
</html>
    )";
    server.send(200, "text/html", html);
  });
  
  server.on("/wifi", HTTP_POST, handleWiFiConfig);
  
  server.on("/status", HTTP_GET, []() {
    String status = "PetFeeder Status:\n";
    status += "WiFi: " + String(wifiConnected ? "Conectado" : "Desconectado") + "\n";
    status += "MQTT: " + String(mqttConnected ? "Conectado" : "Desconectado") + "\n";
    status += "Servo: " + String(servoPosition) + "\n";
    status += "Comedero: " + String(feederBusy ? "Ocupado" : "Listo");
    server.send(200, "text/plain", status);
  });
}

// Manejar configuración WiFi vía servidor web
void handleWiFiConfig() {
  if (server.method() != HTTP_POST) {
    server.send(405, "text/plain", "Método no permitido");
    return;
  }
  
  String ssid = server.arg("ssid");
  String password = server.arg("password");
  
  if (ssid.length() == 0) {
    server.send(400, "text/plain", "SSID requerido");
    return;
  }
  
  Serial.println("📶 Intentando conectar a: " + ssid);
  
  WiFi.begin(ssid.c_str(), password.c_str());
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    // Guardar credenciales
    prefs.putString("wifi_ssid", ssid);
    prefs.putString("wifi_password", password);
    
    wifiConnected = true;
    apMode = false;
    
    Serial.println("\n✅ Conectado a WiFi");
    Serial.print("📡 IP: ");
    Serial.println(WiFi.localIP());
    
    // Conectar MQTT
    connectMQTT();
    
    server.send(200, "text/plain", "Conectado exitosamente a " + ssid + "\nReiniciando en 3 segundos...");
    
    // Reiniciar después de 3 segundos
    delay(3000);
    ESP.restart();
    
  } else {
    server.send(500, "text/plain", "Error al conectar a " + ssid);
  }
}

// Función para iniciar BLE
void startBLE() {
  BLEDevice::init("PetFeeder_BLE");
  BLEServer *pServer = BLEDevice::createServer();
  
  BLEService *pService = pServer->createService(SERVICE_UUID);
  
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_READ |
    BLECharacteristic::PROPERTY_WRITE
  );
  
  pCharacteristic->setValue("PetFeeder BLE Ready");
  
  pService->start();
  
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);
  BLEDevice::startAdvertising();
  
  bleActive = true;
  Serial.println("📱 BLE iniciado");
} 