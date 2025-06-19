# Funcionalidad MQTT - Mosquito Prueba

## Descripción
Esta nueva funcionalidad permite conectarse a un broker MQTT y enviar suscripciones a tu ESP32. La implementación utiliza la librería **HiveMQ MQTT Client** para Android, que es una alternativa moderna y eficiente a Eclipse Paho.

## Características
- Conexión a broker MQTT personalizable
- Publicación de mensajes a tópicos específicos
- Suscripción a tópicos para recibir mensajes
- Interfaz de usuario intuitiva
- Log de mensajes en tiempo real
- Estado de conexión visible
- Soporte completo para MQTT 5.0
- Manejo asíncrono de operaciones

## Configuración por defecto
- **Broker MQTT**: `tcp://192.168.4.1:1883` (IP típica del ESP32 en modo AP)
- **Cliente ID**: `AndroidClient`
- **Tópico por defecto**: `test/topic`
- **Mensaje por defecto**: `Hola ESP32!`

## Cómo usar

### 1. Acceder a la funcionalidad
- Abre la aplicación
- Desliza desde el borde izquierdo para abrir el menú lateral
- Selecciona "Mosquito Prueba"

### 2. Conectar al broker MQTT
- Verifica que la dirección del broker sea correcta (por defecto apunta al ESP32)
- Ajusta el Client ID si es necesario
- Presiona "Conectar"
- El estado cambiará a "Conectado" cuando la conexión sea exitosa

### 3. Publicar mensajes
- Ingresa el tópico donde quieres publicar
- Escribe el mensaje que quieres enviar
- Presiona "Publicar Mensaje"
- El mensaje aparecerá en el log con timestamp

### 4. Suscribirse a tópicos
- Ingresa el tópico al que quieres suscribirte
- Presiona "Suscribirse"
- Los mensajes recibidos en ese tópico aparecerán automáticamente en el log

### 5. Desconectar
- Presiona "Desconectar" para cerrar la conexión MQTT

## Configuración del ESP32
Para que tu ESP32 reciba los mensajes MQTT, asegúrate de que esté configurado como broker MQTT o conectado a uno. Ejemplo básico de configuración:

```cpp
#include <WiFi.h>
#include <PubSubClient.h>

const char* ssid = "TuWiFi";
const char* password = "TuPassword";
const char* mqtt_server = "192.168.4.1";

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido en [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
}

void reconnect() {
  while (!client.connected()) {
    Serial.println("Conectando a MQTT...");
    if (client.connect("ESP32Client")) {
      Serial.println("Conectado");
      client.subscribe("test/topic");
    } else {
      Serial.print("Error, rc=");
      Serial.print(client.state());
      Serial.println(" reintentando en 5 segundos");
      delay(5000);
    }
  }
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}
```

## Permisos requeridos
La aplicación requiere los siguientes permisos:
- `INTERNET`: Para conectarse al broker MQTT

## Ventajas de HiveMQ MQTT Client

### Comparado con Eclipse Paho:
- **Más ligero**: Menor tamaño de librería
- **Mejor rendimiento**: Operaciones asíncronas más eficientes
- **MQTT 5.0**: Soporte completo para la versión más reciente del protocolo
- **Sin dependencias externas**: No requiere servicios adicionales
- **API más moderna**: Uso de CompletableFuture y lambdas
- **Mejor manejo de errores**: Callbacks más claros y específicos

## Solución de problemas

### Error de conexión
- Verifica que la dirección del broker sea correcta
- Asegúrate de que el ESP32 esté funcionando como broker MQTT
- Verifica la conectividad de red

### Mensajes no recibidos
- Verifica que estés suscrito al tópico correcto
- Asegúrate de que el ESP32 esté publicando en el mismo tópico
- Revisa la configuración QoS del broker

### Aplicación se cierra
- Verifica que todas las dependencias estén correctamente instaladas
- Revisa los logs de Android para errores específicos

## Notas técnicas
- La implementación utiliza HiveMQ MQTT Client v1.3.0
- Soporte completo para MQTT 5.0
- Los mensajes se envían con QoS 0 (at most once)
- La conexión se mantiene activa con keep-alive de 60 segundos
- Operaciones completamente asíncronas
- No requiere servicios de Android adicionales 