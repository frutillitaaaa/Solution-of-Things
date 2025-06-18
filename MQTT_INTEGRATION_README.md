# Integración MQTT Eclipse Paho para Android 12+

Este proyecto incluye una integración completa de MQTT Eclipse Paho para Android, compatible con versiones superiores a Android 12.

## Características

- ✅ Conexión MQTT con broker público (HiveMQ)
- ✅ Suscripción a tópicos MQTT
- ✅ Recepción de mensajes en tiempo real
- ✅ Gestión de QoS (0, 1, 2)
- ✅ Interfaz de usuario intuitiva
- ✅ Servicio en segundo plano
- ✅ Compatible con Android 12+

## Estructura del Proyecto

### Archivos Principales

- `MqttSubscriptionsActivity.kt` - Actividad principal para gestionar suscripciones
- `MqttService.kt` - Servicio en segundo plano para conexiones MQTT
- `MqttSubscription.kt` - Modelo de datos para suscripciones
- `MqttSubscriptionsAdapter.kt` - Adaptador para RecyclerView

### Layouts

- `activity_mqtt_subscriptions.xml` - Layout principal de la actividad
- `item_mqtt_subscription.xml` - Layout para cada suscripción
- `dialog_add_subscription.xml` - Diálogo para agregar suscripciones

### Recursos

- `ic_mqtt.xml` - Icono para MQTT
- `ic_add.xml` - Icono para agregar
- Strings en `strings.xml` - Textos localizados

## Configuración

### Dependencias

Las dependencias MQTT ya están incluidas en `build.gradle.kts`:

```kotlin
implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
```

### Permisos

Los permisos necesarios ya están configurados en `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Servicios

Se han registrado los servicios MQTT necesarios:

```xml
<service android:name="org.eclipse.paho.android.service.MqttService" />
<service android:name=".mqtt.MqttService" />
```

## Uso

### Acceso a la Funcionalidad

1. Abre la aplicación
2. Desliza desde el borde izquierdo para abrir el menú lateral
3. Toca "Suscripciones MQTT"

### Agregar una Suscripción

1. Toca el botón "+" (FloatingActionButton)
2. Ingresa el tópico MQTT (ej: `sensor/temperatura`)
3. Selecciona el nivel de QoS
4. Toca "Suscribirse"

### Ver Detalles de Suscripción

1. Toca en cualquier suscripción de la lista
2. Verás los detalles y el último mensaje recibido
3. Puedes desuscribirte desde aquí

## Configuración del Broker

Por defecto, la aplicación usa el broker público de HiveMQ. Para cambiar el broker:

1. Modifica `SERVER_URI` en `MqttSubscriptionsActivity.kt`
2. Modifica `SERVER_URI` en `MqttService.kt`

### Ejemplos de Brokers

```kotlin
// Broker público HiveMQ
private const val SERVER_URI = "tcp://broker.hivemq.com:1883"

// Broker local
private const val SERVER_URI = "tcp://192.168.1.100:1883"

// Broker con SSL
private const val SERVER_URI = "ssl://broker.example.com:8883"
```

## Características Técnicas

### Compatibilidad Android 12+

- Manejo correcto de permisos de red
- Uso de servicios en segundo plano
- Gestión de ciclo de vida de actividades

### QoS (Quality of Service)

- **QoS 0**: Máximo una vez - Sin confirmación
- **QoS 1**: Al menos una vez - Confirmación de entrega
- **QoS 2**: Exactamente una vez - Confirmación de entrega garantizada

### Manejo de Errores

- Reconexión automática
- Manejo de errores de red
- Feedback visual al usuario

## Personalización

### Cambiar el Broker

```kotlin
// En MqttSubscriptionsActivity.kt y MqttService.kt
private const val SERVER_URI = "tcp://tu-broker.com:1883"
```

### Agregar Autenticación

```kotlin
val options = MqttConnectOptions()
options.userName = "usuario"
options.password = "contraseña".toCharArray()
mqttClient.connect(options)
```

### Configurar SSL/TLS

```kotlin
val options = MqttConnectOptions()
options.socketFactory = SSLSocketFactory.getDefault()
mqttClient.connect(options)
```

## Solución de Problemas

### Error de Conexión

1. Verifica la conectividad a internet
2. Confirma que el broker esté disponible
3. Revisa los logs en Android Studio

### Mensajes No Recibidos

1. Verifica que el tópico esté correcto
2. Confirma que haya publicadores en el tópico
3. Revisa el nivel de QoS

### Problemas de Permisos

1. Verifica que los permisos estén concedidos
2. En Android 12+, asegúrate de que la app tenga permisos de red

## Contribución

Para contribuir al proyecto:

1. Fork el repositorio
2. Crea una rama para tu feature
3. Realiza los cambios
4. Envía un pull request

## Licencia

Este proyecto está bajo la licencia MIT. Ver el archivo LICENSE para más detalles. 