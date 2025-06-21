# Integración MQTT con Sketch WiFi + BLE Existente

## 🎯 Objetivo
Integrar la funcionalidad MQTT de tu aplicación "Solution-of-Things" con tu sketch existente que ya maneja WiFi, BLE y servidor web.

## 📋 Análisis del Sketch Original

Tu sketch original incluye:
- ✅ **WiFi**: Conexión y gestión de credenciales con Preferences
- ✅ **BLE**: Configuración vía Bluetooth Low Energy
- ✅ **Servidor Web**: Configuración WiFi vía HTTP POST
- ✅ **Botón BOOT**: Reset de credenciales (3 pulsaciones)
- ✅ **LED**: Indicador de estado
- ✅ **ArduinoJson**: Manejo de JSON

## 🔧 Integración Realizada

### Nuevas Funcionalidades Agregadas:
- ✅ **MQTT Client**: Conexión a broker MQTT
- ✅ **Callbacks MQTT**: Manejo de mensajes recibidos
- ✅ **Tópicos Personalizados**: Para tu aplicación específica
- ✅ **Respuestas Automáticas**: A cada mensaje recibido
- ✅ **Control de LED**: Vía comandos MQTT
- ✅ **Estado del Sistema**: Publicación periódica en JSON
- ✅ **Reconexión Automática**: Si se pierde la conexión MQTT

### Tópicos MQTT Implementados:

#### **Entrada (App → ESP32):**
- `test/topic` - Mensajes de prueba generales
- `esp32/led` - Control del LED (ON/OFF/TOGGLE/BLINK)
- `petfeeder/config` - Mensajes de configuración

#### **Salida (ESP32 → App):**
- `test/response` - Respuestas a mensajes de prueba
- `petfeeder/status` - Estado completo del sistema (JSON)
- `petfeeder/led/status` - Estado actual del LED
- `petfeeder/config/response` - Respuestas de configuración

## 🚀 Instrucciones de Uso

### Paso 1: Instalar Bibliotecas
En Arduino IDE, instala:
1. **PubSubClient** (por Nick O'Leary)
2. **ArduinoJson** (por Benoit Blanchon) - Ya tienes esta

### Paso 2: Configurar WiFi
**Opción A - Vía BLE:**
1. Conecta tu dispositivo BLE
2. Envía comando: `start_ap`
3. Conéctate al AP: `PetFeeder_Setup_EA9E`
4. Ve a `http://192.168.4.1`
5. Envía JSON: `{"ssid":"TuWiFi","password":"TuPassword"}`

**Opción B - Reset y BLE:**
1. Presiona 3 veces el botón BOOT
2. Sigue los pasos de la Opción A

### Paso 3: Verificar Conexión MQTT
Una vez conectado a WiFi:
- El ESP32 se conectará automáticamente a MQTT
- El LED se encenderá brevemente para confirmar
- En el Monitor Serial verás: `✅ Conectado a MQTT`

### Paso 4: Configurar la App Android
En "Solution-of-Things" → "Mosquito Prueba":
- **Broker:** `tcp://test.mosquitto.org:1883`
- **Cliente ID:** `AndroidClient`
- **Tópico:** `test/topic`
- **Mensaje:** `Hola PetFeeder!`

## 🎮 Comandos Disponibles

### Comandos LED vía MQTT:
```
Tópico: esp32/led
Mensajes:
- "ON" - Enciende el LED
- "OFF" - Apaga el LED  
- "TOGGLE" - Alterna el estado
- "BLINK" - Parpadea por 3 segundos
```

### Mensajes de Prueba:
```
Tópico: test/topic
Mensaje: Cualquier texto
Respuesta: "PetFeeder ESP32 recibió: [mensaje]"
```

### Estado del Sistema:
```
Tópico: petfeeder/status
Contenido: JSON con información completa
```

## 📊 Flujo de Funcionamiento

### 1. Inicio del Sistema:
```
ESP32 arranca → Intenta conectar WiFi → 
Si falla → Inicia BLE → Espera comando start_ap
Si conecta → Conecta MQTT → Listo para usar
```

### 2. Comunicación MQTT:
```
App envía mensaje → ESP32 recibe → 
Procesa según tópico → Responde automáticamente
```

### 3. Indicadores LED:
```
- Parpadeo lento: Modo AP activo
- LED fijo: WiFi + MQTT conectados
- LED apagado: Sin conexión
- Cambios rápidos: Actividad MQTT
```

## 🔍 Monitoreo y Debug

### Monitor Serial (115200 baudios):
```
=== PetFeeder ESP32 Integrado ===
WiFi + BLE + MQTT para Solution-of-Things
🔗 Conectando a WiFi: TuWiFi
✅ Conectado a WiFi
IP: 192.168.1.100
🔗 Conectando a MQTT...
✅ Conectado a MQTT
📡 Suscrito a tópicos:
  - test/topic
  - esp32/led
  - petfeeder/config
📨 MQTT recibido en [test/topic] Hola PetFeeder!
🔄 Procesando mensaje de prueba...
✅ Respuesta publicada: PetFeeder ESP32 recibió: Hola PetFeeder!
```

### Log de la App Android:
```
[14:30:15] Conectado exitosamente
[14:30:20] Enviado a test/topic: Hola PetFeeder!
[14:30:21] Mensaje recibido: PetFeeder ESP32 recibió: Hola PetFeeder!
```

## ⚙️ Personalización

### Cambiar Broker MQTT:
```cpp
// En el código, cambia:
const char* mqtt_server = "tu.broker.mqtt.com";
const int mqtt_port = 1883;
```

### Agregar Nuevos Tópicos:
```cpp
// 1. Definir nuevo tópico:
const char* topic_nuevo = "petfeeder/nuevo";

// 2. Suscribirse en connectMQTT():
mqttClient.subscribe(topic_nuevo);

// 3. Manejar en mqttCallback():
if (String(topic) == topic_nuevo) {
    handleNuevoMensaje(message);
}
```

### Modificar Respuestas:
```cpp
void handleTestMessage(String message) {
    // Personaliza la respuesta aquí
    String response = "Tu respuesta personalizada: " + message;
    mqttClient.publish(topic_response, response.c_str());
}
```

## 🐛 Solución de Problemas

### MQTT No Se Conecta:
1. Verificar que WiFi esté conectado
2. Verificar que el broker sea accesible
3. Revisar el Monitor Serial para errores
4. Probar con broker público: `test.mosquitto.org`

### Mensajes No Llegan:
1. Verificar que estés suscrito al tópico correcto
2. Verificar que el ESP32 esté publicando en el mismo tópico
3. Revisar logs en Monitor Serial

### LED No Responde:
1. Verificar que el comando sea correcto (ON/OFF/TOGGLE/BLINK)
2. Verificar que el tópico sea `esp32/led`
3. Revisar Monitor Serial para confirmar recepción

### WiFi Se Pierde:
1. El ESP32 intentará reconectar automáticamente
2. Si falla, iniciará BLE para nueva configuración
3. MQTT se reconectará cuando WiFi esté disponible

## 📱 Compatibilidad con tu App

### Configuración Recomendada:
- **Broker:** `tcp://test.mosquitto.org:1883`
- **WebSocket:** `ws://test.mosquitto.org:8080`
- **Cliente ID:** `AndroidClient`
- **Tópico por defecto:** `test/topic`

### Tópicos para Probar:
1. **Mensaje simple:** `test/topic` → `test/response`
2. **Control LED:** `esp32/led` → `petfeeder/led/status`
3. **Estado sistema:** Suscribirse a `petfeeder/status`

## ✅ Checklist de Verificación

- [ ] Bibliotecas instaladas (PubSubClient, ArduinoJson)
- [ ] Código subido al ESP32
- [ ] WiFi configurado y conectado
- [ ] MQTT conectado (LED fijo)
- [ ] App Android configurada
- [ ] Mensajes enviados y recibidos
- [ ] LED responde a comandos
- [ ] Estado del sistema se publica
- [ ] Logs funcionando en ambos lados

## 🎉 Resultado Final

Con esta integración tendrás:
- ✅ **Configuración WiFi** vía BLE o servidor web
- ✅ **Comunicación MQTT** bidireccional con tu app
- ✅ **Control remoto** del LED del ESP32
- ✅ **Monitoreo** del estado del sistema
- ✅ **Respuestas automáticas** a cada mensaje
- ✅ **Reconexión automática** si se pierde la conexión
- ✅ **Compatibilidad total** con tu app "Solution-of-Things"

¡Tu ESP32 ahora es un dispositivo IoT completo que puede comunicarse con tu aplicación Android! 