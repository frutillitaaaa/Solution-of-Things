# 🍽️ PetFeeder - Sistema Completo de Comedero Automático

## 📋 Resumen del Proyecto

Este proyecto implementa un **comedero automático para mascotas** controlado desde un smartphone Android a través de WiFi y MQTT. El sistema incluye:

- **ESP32** con servo motor para dispensar comida
- **App Android** para control remoto
- **Comunicación MQTT** para comandos y estado
- **Configuración WiFi** vía BLE y servidor web

## 🏗️ Arquitectura del Sistema

```
📱 Smartphone (App Android)
    ↕️ MQTT
🌐 Broker MQTT (test.mosquitto.org)
    ↕️ MQTT
🔧 ESP32 + Servo Motor
    ↕️ Mecánico
🍽️ Comedero de Mascotas
```

## 📦 Componentes Necesarios

### Hardware
- **ESP32** (cualquier modelo)
- **Servo Motor** (SG90 o similar)
- **Fuente de alimentación** (5V para el servo)
- **Comedero mecánico** (recipiente con tapa)
- **Cables y conectores**

### Software
- **Arduino IDE** con librerías ESP32
- **Android Studio** para la app
- **Librerías MQTT** (Eclipse Paho)

## 🔧 Instalación y Configuración

### 1. Hardware del ESP32

#### Conexiones del Servo:
```
ESP32 Pin 13 → Señal del Servo (cable amarillo/naranja)
ESP32 GND → GND del Servo (cable negro/marrón)
ESP32 5V → VCC del Servo (cable rojo)
```

#### Montaje Mecánico:
1. **Fijar el servo** al comedero
2. **Conectar el brazo** del servo a la tapa del comedero
3. **Ajustar la posición** para que:
   - Posición 0° = Comedero cerrado
   - Posición 90° = Comedero abierto

### 2. Software del ESP32

#### Instalar Librerías:
1. Abrir Arduino IDE
2. Ir a **Herramientas → Administrar Bibliotecas**
3. Instalar:
   - `ESP32Servo` por Kevin Harrington
   - `PubSubClient` por Nick O'Leary
   - `ArduinoJson` por Benoit Blanchon
   - `Preferences` (incluida con ESP32)

#### Cargar el Sketch:
1. Abrir `ESP32_PetFeeder_Complete.ino`
2. Seleccionar tu placa ESP32
3. Cargar el código

#### Configuración Inicial:
1. **Primera vez**: El ESP32 creará una red WiFi llamada `PetFeeder_Setup`
2. **Conectar** a esta red (contraseña: `12345678`)
3. **Abrir navegador** y ir a `192.168.4.1`
4. **Ingresar** credenciales de tu WiFi
5. **El ESP32 se reiniciará** y se conectará automáticamente

### 3. App Android

#### Configuración:
1. Abrir el proyecto en **Android Studio**
2. **Sincronizar** dependencias Gradle
3. **Compilar** y instalar en tu dispositivo

#### Configuración MQTT:
- **Broker**: `tcp://test.mosquitto.org:1883`
- **Client ID**: `AndroidClient` (o cualquier nombre único)

## 🎮 Uso del Sistema

### Flujo de Trabajo:

1. **Conectar ESP32 a WiFi** (una sola vez)
2. **Abrir la app** en el smartphone
3. **Conectar a MQTT** (botón "Conectar")
4. **Usar los controles** del comedero:
   - 🍽️ **1 Porción**: Dispensa una porción de comida
   - 🍽️ **Múltiples**: Selecciona 1-5 porciones
   - 🔓 **Abrir**: Abre el comedero manualmente
   - 🔒 **Cerrar**: Cierra el comedero manualmente
   - 📊 **Estado**: Consulta el estado actual

### Comandos MQTT Disponibles:

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `FEED` | Dispensar 1 porción | `FEED` |
| `FEED:N` | Dispensar N porciones | `FEED:3` |
| `OPEN` | Abrir comedero | `OPEN` |
| `CLOSE` | Cerrar comedero | `CLOSE` |
| `STATUS` | Consultar estado | `STATUS` |
| `SERVO:N` | Mover servo a posición N | `SERVO:45` |

### Tópicos MQTT:

| Tópico | Dirección | Descripción |
|--------|-----------|-------------|
| `petfeeder/control` | App → ESP32 | Comandos de control |
| `petfeeder/feeder/status` | ESP32 → App | Estado del comedero |
| `petfeeder/status` | ESP32 → App | Estado general del sistema |
| `test/topic` | Bidireccional | Pruebas generales |
| `test/response` | ESP32 → App | Respuestas de prueba |

## ⚙️ Personalización

### Ajustar el Servo:

En el código del ESP32, modifica estas constantes:

```cpp
const int SERVO_MIN_POS = 0;    // Posición cerrada
const int SERVO_MAX_POS = 90;   // Posición abierta
const int SERVO_DELAY = 1000;   // Tiempo abierto (ms)
```

### Cambiar el Pin del Servo:

```cpp
#define SERVO_PIN 13  // Cambiar por el pin que uses
```

### Ajustar el Tamaño de Porción:

Modifica la función `dispenseFood()` para cambiar:
- Tiempo que permanece abierto
- Número máximo de porciones
- Comportamiento del LED

## 🔍 Solución de Problemas

### ESP32 no se conecta a WiFi:
1. **Verificar credenciales** en el servidor web
2. **Reiniciar** el ESP32
3. **Usar modo AP** para reconfigurar

### App no se conecta a MQTT:
1. **Verificar** conexión a internet
2. **Comprobar** broker y puerto
3. **Revisar** logs en Logcat

### Servo no funciona:
1. **Verificar** conexiones eléctricas
2. **Comprobar** voltaje (5V)
3. **Ajustar** posiciones del servo
4. **Revisar** pin asignado

### Comedero no dispensa correctamente:
1. **Ajustar** posiciones del servo
2. **Verificar** montaje mecánico
3. **Aumentar** tiempo de apertura
4. **Limpiar** mecanismo

## 📊 Monitoreo y Logs

### ESP32 Serial Monitor:
```
🚀 Iniciando PetFeeder ESP32...
🔧 Servo inicializado en posición cerrada
📶 Conectado a WiFi
🔗 Conectando a MQTT...
✅ Conectado a MQTT
🍽️ Dispensando 1 porción(es) de comida
✅ Comida dispensada correctamente
```

### App Android Logcat:
```
Filtrar por: "MqttActivity"
[14:30:15] 🍽️ Comando enviado: Dispensar 1 porción
[14:30:16] ✅ Comedero listo
[14:30:17] 📨 Respuesta del ESP32: Comida dispensada: 1 porción(es)
```

## 🚀 Próximas Mejoras

### Funcionalidades Adicionales:
- **Programación horaria** de comidas
- **Sensor de peso** para monitorear nivel de comida
- **Cámara** para ver a la mascota
- **Notificaciones** push
- **Múltiples mascotas** con RFID
- **Historial** de comidas
- **Modo automático** basado en horarios

### Mejoras Técnicas:
- **Broker MQTT local** (Mosquitto)
- **Encriptación** de comunicaciones
- **Backup** de configuración
- **OTA Updates** (actualizaciones inalámbricas)
- **Batería** de respaldo
- **Sensor de temperatura** para comida

## 📞 Soporte

Para problemas técnicos:
1. **Revisar** logs del ESP32 y Android
2. **Verificar** conexiones de hardware
3. **Probar** con comandos básicos MQTT
4. **Consultar** documentación de librerías

## 📝 Notas Importantes

- **Seguridad**: El broker público `test.mosquitto.org` es para pruebas
- **Alimentación**: Usar fuente de 5V estable para el servo
- **Mantenimiento**: Limpiar el mecanismo regularmente
- **Pruebas**: Siempre probar con comida seca primero
- **Backup**: Guardar configuración WiFi en lugar seguro

---

**¡Disfruta alimentando a tus mascotas de forma inteligente! 🐕🐱** 