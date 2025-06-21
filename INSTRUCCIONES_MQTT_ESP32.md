# Instrucciones para Probar MQTT con ESP32 y App Android

## 📋 Resumen
Este documento te guía para probar la funcionalidad "Mosquito Prueba" de tu aplicación Android "Solution-of-Things" con un ESP32.

## 🎯 Objetivo
Establecer comunicación bidireccional entre tu app Android y un ESP32 usando el protocolo MQTT sobre WebSockets.

## 📦 Archivos Incluidos
- `ESP32_MQTT_Broker_Test.ino` - Sketch completo con broker MQTT local
- `ESP32_MQTT_Client_Simple.ino` - Sketch simple usando broker público
- `INSTRUCCIONES_MQTT_ESP32.md` - Este archivo

## 🚀 Opción 1: Usando Broker Público (Más Fácil)

### Paso 1: Preparar el ESP32
1. **Abrir Arduino IDE**
2. **Instalar biblioteca PubSubClient:**
   - Ve a `Herramientas > Gestor de Bibliotecas`
   - Busca "PubSubClient"
   - Instala la versión de **Nick O'Leary**
3. **Abrir el sketch:** `ESP32_MQTT_Client_Simple.ino`
4. **Configurar WiFi:**
   ```cpp
   const char* ssid = "TuWiFi";           // Cambia por tu SSID
   const char* password = "TuPassword";   // Cambia por tu contraseña
   ```
5. **Subir el código al ESP32**
6. **Abrir Monitor Serial** (115200 baudios)

### Paso 2: Configurar la App Android
1. **Abrir la app** "Solution-of-Things"
2. **Ir al menú lateral** (deslizar desde el borde izquierdo)
3. **Seleccionar "Mosquito Prueba"**
4. **Configurar conexión:**
   - **Broker:** `tcp://test.mosquitto.org:1883`
   - **Cliente ID:** `AndroidClient`
   - **Tópico:** `test/topic`
   - **Mensaje:** `Hola ESP32!`
5. **Presionar "Conectar"**

### Paso 3: Probar la Comunicación
1. **En la app, publicar un mensaje:**
   - Tópico: `test/topic`
   - Mensaje: `Hola ESP32!`
   - Presionar "Publicar Mensaje"

2. **En la app, suscribirse a respuestas:**
   - Tópico: `test/response`
   - Presionar "Suscribirse"

3. **Verificar en el Monitor Serial del ESP32:**
   ```
   Mensaje recibido en [test/topic] Hola ESP32!
   Procesando mensaje de prueba...
   Respuesta publicada: ESP32 recibió: Hola ESP32!
   ```

4. **Verificar en la app:**
   - Debería aparecer: `[HH:mm:ss] Mensaje recibido: ESP32 recibió: Hola ESP32!`

### Paso 4: Probar Control de LED
1. **En la app, publicar comando LED:**
   - Tópico: `esp32/led`
   - Mensaje: `ON`
   - Presionar "Publicar Mensaje"

2. **Verificar en el ESP32:**
   - El LED integrado debería encenderse
   - En el Monitor Serial: `LED encendido`

3. **Probar otros comandos:**
   - `OFF` - Apaga el LED
   - `TOGGLE` - Alterna el estado

## 🔧 Opción 2: Usando Broker Local (Más Completo)

### Paso 1: Configurar ESP32 como Broker
1. **Usar el sketch:** `ESP32_MQTT_Broker_Test.ino`
2. **Configurar WiFi** (igual que antes)
3. **Instalar biblioteca adicional:**
   - Busca "ArduinoJson" en el Gestor de Bibliotecas
   - Instala la versión de **Benoit Blanchon**
4. **Subir el código al ESP32**

### Paso 2: Configurar la App
1. **Obtener IP del ESP32** (aparece en el Monitor Serial)
2. **En la app, configurar:**
   - **Broker:** `tcp://[IP_DEL_ESP32]:1883`
   - **Cliente ID:** `AndroidClient`
   - **Tópico:** `test/topic`

### Paso 3: Probar Funcionalidades Avanzadas
- **Mensajes periódicos:** El ESP32 envía mensajes cada 5 segundos
- **Estado del sistema:** Información JSON cada 10 segundos
- **Control de LED:** Comandos ON/OFF/TOGGLE
- **Respuestas automáticas:** A cada mensaje recibido

## 📱 Funcionalidades de la App

### Campos de Configuración
- **Broker:** URL del servidor MQTT
- **Cliente ID:** Identificador único del cliente
- **Tópico:** Tópico para publicar/suscribirse
- **Mensaje:** Mensaje a enviar

### Botones
- **Conectar:** Establece conexión WebSocket
- **Desconectar:** Cierra la conexión
- **Publicar Mensaje:** Envía mensaje al tópico
- **Suscribirse:** Se suscribe al tópico para recibir mensajes

### Log de Mensajes
- Muestra todos los mensajes enviados y recibidos
- Incluye timestamps
- Formato: `[HH:mm:ss] Mensaje: contenido`

## 🔍 Solución de Problemas

### Error de Conexión
**Síntoma:** "Error de conexión" en la app
**Soluciones:**
1. Verificar que el ESP32 esté conectado a WiFi
2. Verificar que la IP del ESP32 sea correcta
3. Verificar que el broker esté funcionando
4. Probar con el broker público primero

### Mensajes No Recibidos
**Síntoma:** La app no recibe respuestas del ESP32
**Soluciones:**
1. Verificar que estés suscrito al tópico correcto
2. Verificar que el ESP32 esté publicando en el mismo tópico
3. Revisar el Monitor Serial del ESP32

### App Se Cierra
**Síntoma:** La aplicación se cierra inesperadamente
**Soluciones:**
1. Verificar que todas las dependencias estén instaladas
2. Revisar los logs de Android Studio
3. Reiniciar la aplicación

### ESP32 No Responde
**Síntoma:** El ESP32 no recibe mensajes
**Soluciones:**
1. Verificar conexión WiFi
2. Verificar conexión MQTT
3. Revisar el Monitor Serial
4. Reiniciar el ESP32

## 📊 Tópicos MQTT Disponibles

### Tópicos de Entrada (App → ESP32)
- `test/topic` - Mensajes de prueba
- `esp32/led` - Control del LED (ON/OFF/TOGGLE)

### Tópicos de Salida (ESP32 → App)
- `test/response` - Respuestas a mensajes de prueba
- `esp32/status` - Estado del sistema (JSON)
- `esp32/led/status` - Estado actual del LED

## 🎯 Ejemplos de Uso

### Ejemplo 1: Mensaje Simple
```
App publica en "test/topic": "Hola ESP32!"
ESP32 responde en "test/response": "ESP32 recibió: Hola ESP32!"
```

### Ejemplo 2: Control de LED
```
App publica en "esp32/led": "ON"
ESP32 enciende LED y publica en "esp32/led/status": "ON"
```

### Ejemplo 3: Estado del Sistema
```
ESP32 publica en "esp32/status": 
{
  "timestamp": 1234567890,
  "uptime": 60000,
  "free_heap": 123456,
  "wifi_rssi": -45,
  "led_state": "ON"
}
```

## 🔒 Notas de Seguridad

### Broker Público
- **test.mosquitto.org** es solo para pruebas
- No envíes información sensible
- Los mensajes son públicos
- Usa solo para desarrollo y pruebas

### Broker Local
- Más seguro para uso en producción
- Control total sobre los datos
- Requiere configuración adicional
- Recomendado para proyectos reales

## 📞 Soporte

Si encuentras problemas:
1. Revisa el Monitor Serial del ESP32
2. Verifica la configuración de WiFi
3. Confirma que las bibliotecas estén instaladas
4. Prueba con el broker público primero
5. Revisa los logs de la aplicación Android

## ✅ Checklist de Verificación

- [ ] ESP32 conectado a WiFi
- [ ] Biblioteca PubSubClient instalada
- [ ] Código subido al ESP32
- [ ] Monitor Serial funcionando
- [ ] App Android instalada
- [ ] Configuración MQTT correcta
- [ ] Conexión establecida
- [ ] Mensajes enviados y recibidos
- [ ] LED responde a comandos
- [ ] Log de mensajes funcionando

¡Con esto deberías poder probar completamente la funcionalidad MQTT de tu aplicación! 