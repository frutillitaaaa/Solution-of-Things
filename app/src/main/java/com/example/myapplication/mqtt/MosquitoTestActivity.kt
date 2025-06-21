package com.example.myapplication.mqtt

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private var mqttClient: MqttAndroidClient? = null
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    override fun onDestroy() {
        // Limpieza final al destruir la actividad
        closeMqttClient()
        super.onDestroy()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener { connectToMqtt() }
        binding.btnDisconnect.setOnClickListener { disconnectFromMqtt() }
        binding.btnPublish.setOnClickListener { publishMessage() }
        binding.btnSubscribe.setOnClickListener { subscribeToTopic() }
        
        // Nuevos botones para el comedero
        binding.btnFeed.setOnClickListener { feedPet() }
        binding.btnFeedMultiple.setOnClickListener { feedMultiplePortions() }
        binding.btnFeederStatus.setOnClickListener { checkFeederStatus() }
        binding.btnOpenFeeder.setOnClickListener { openFeeder() }
        binding.btnCloseFeeder.setOnClickListener { closeFeeder() }
    }

    private fun connectToMqtt() {
        val serverUri = binding.etBroker.text.toString()
        val clientId = binding.etClientId.text.toString()

        if (serverUri.isEmpty() || clientId.isEmpty()) {
            Toast.makeText(this, "Por favor completa Broker y Cliente ID", Toast.LENGTH_SHORT).show()
            return
        }

        // --- LÓGICA DE TIERRA QUEMADA ---
        // Siempre creamos un cliente nuevo para garantizar un estado limpio.
        // Primero, cerramos y destruimos cualquier cliente anterior.
        closeMqttClient()

        val webSocketUri = convertToWebSocketUri(serverUri)
        mqttClient = MqttAndroidClient(applicationContext, webSocketUri, clientId)

        mqttClient?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                isConnected = true
                runOnUiThread {
                    updateStatus("Conectado")
                    Toast.makeText(this@MosquitoTestActivity, "Conectado", Toast.LENGTH_SHORT).show()
                    binding.btnConnect.isEnabled = false
                    binding.btnDisconnect.isEnabled = true
                    binding.btnPublish.isEnabled = true
                    binding.btnSubscribe.isEnabled = true
                    
                    // Habilitar botones del comedero
                    binding.btnFeed.isEnabled = true
                    binding.btnFeedMultiple.isEnabled = true
                    binding.btnFeederStatus.isEnabled = true
                    binding.btnOpenFeeder.isEnabled = true
                    binding.btnCloseFeeder.isEnabled = true
                }
                
                // Suscribirse automáticamente al tópico del comedero
                subscribeToFeederTopic()
            }

            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                runOnUiThread {
                    val causeMsg = cause?.message ?: "desconocida"
                    updateStatus("Conexión perdida: $causeMsg")
                    binding.btnConnect.isEnabled = true
                    binding.btnDisconnect.isEnabled = false
                    binding.btnPublish.isEnabled = false
                    binding.btnSubscribe.isEnabled = false
                    
                    // Deshabilitar botones del comedero
                    binding.btnFeed.isEnabled = false
                    binding.btnFeedMultiple.isEnabled = false
                    binding.btnFeederStatus.isEnabled = false
                    binding.btnOpenFeeder.isEnabled = false
                    binding.btnCloseFeeder.isEnabled = false
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val receivedMessage = "[$timestamp] Mensaje de '$topic': ${message.toString()}"
                addMessage(receivedMessage)
                
                // Procesar mensajes específicos del comedero
                when (topic) {
                    "petfeeder/feeder/status" -> {
                        val statusMsg = message.toString()
                        when {
                            statusMsg.contains("DISPENSING") -> {
                                addMessage("🔄 Comedero dispensando comida...")
                            }
                            statusMsg.contains("READY") -> {
                                addMessage("✅ Comedero listo")
                            }
                            statusMsg.contains("BUSY") -> {
                                addMessage("⚠️ Comedero ocupado")
                            }
                            statusMsg.contains("ERROR") -> {
                                addMessage("❌ Error en el comedero: $statusMsg")
                            }
                            statusMsg.contains("Servo en posición") -> {
                                addMessage("🔧 $statusMsg")
                            }
                        }
                    }
                    "petfeeder/status" -> {
                        addMessage("📊 Estado del sistema: ${message.toString()}")
                    }
                    "test/response" -> {
                        addMessage("📨 Respuesta del ESP32: ${message.toString()}")
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 60
            isAutomaticReconnect = false
        }

        try {
            updateStatus("Conectando a $webSocketUri...")
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {}

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        val errorMsg = exception?.message ?: "Error desconocido"
                        updateStatus("Error al conectar: $errorMsg")
                        Toast.makeText(this@MosquitoTestActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        // Limpiamos el cliente de inmediato al fallar, para no dejarlo en un estado inválido.
                        closeMqttClient()
                    }
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            updateStatus("Error: ${e.message}")
        }
    }

    private fun disconnectFromMqtt() {
        if (mqttClient == null || !mqttClient!!.isConnected) {
            Toast.makeText(this, "Ya estás desconectado", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // El callback 'connectionLost' se encargará de actualizar la UI.
                    // Cerramos el cliente después de desconectar para una limpieza completa.
                    closeMqttClient()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        updateStatus("Error al desconectar: ${exception?.message}")
                    }
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun closeMqttClient() {
        if (mqttClient == null) {
            Log.d("MqttActivity", "closeMqttClient: El cliente ya era nulo. No se hace nada.")
            return
        }
        try {
            Log.d("MqttActivity", "closeMqttClient: Intentando cerrar el cliente MQTT.")
            mqttClient?.close()
            Log.d("MqttActivity", "closeMqttClient: Cliente MQTT cerrado exitosamente.")
        } catch (e: Exception) {
            Log.e("MqttActivity", "Error al cerrar el cliente MQTT.", e)
        }
        mqttClient = null
        isConnected = false
    }

    private fun publishMessage() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()
        val messageText = binding.etMessage.text.toString()

        if (topic.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(this, "Completa el tópico y el mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val message = MqttMessage()
            message.payload = messageText.toByteArray()
            message.qos = 1
            message.isRetained = false

            mqttClient?.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] Enviado a '$topic': $messageText"
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("Error al enviar a '$topic'")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()

        if (topic.isEmpty()) {
            Toast.makeText(this, "Ingresa un tópico para suscribirte", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient?.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val subscribeMsg = "[$timestamp] Suscrito a: $topic"
                    addMessage(subscribeMsg)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("Error al suscribirse a '$topic'")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    // Función para suscribirse automáticamente al tópico del comedero
    private fun subscribeToFeederTopic() {
        try {
            val topic = "petfeeder/feeder/status"
            mqttClient?.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val subscribeMsg = "[$timestamp] 🍽️ Suscrito a estado del comedero: $topic"
                    addMessage(subscribeMsg)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al suscribirse al estado del comedero")
                }
            })
            
            // También suscribirse al tópico de estado general
            val statusTopic = "petfeeder/status"
            mqttClient?.subscribe(statusTopic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val subscribeMsg = "[$timestamp] 📊 Suscrito a estado general: $statusTopic"
                    addMessage(subscribeMsg)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al suscribirse al estado general")
                }
            })
            
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    // Nuevas funciones para controlar el comedero
    private fun feedPet() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val topic = "petfeeder/control"
            val message = "FEED"
            
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 1
            mqttMessage.isRetained = false

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] 🍽️ Comando enviado: Dispensar 1 porción"
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al enviar comando de comida")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun feedMultiplePortions() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar diálogo para seleccionar número de porciones
        val portions = arrayOf("1", "2", "3", "4", "5")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🍽️ Seleccionar porciones")
        builder.setItems(portions) { _, which ->
            val selectedPortions = portions[which]
            sendFeedCommand(selectedPortions.toInt())
        }
        builder.show()
    }

    private fun sendFeedCommand(portions: Int) {
        try {
            val topic = "petfeeder/control"
            val message = "FEED:$portions"
            
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 1
            mqttMessage.isRetained = false

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] 🍽️ Comando enviado: Dispensar $portions porción(es)"
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al enviar comando de comida")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun checkFeederStatus() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val topic = "petfeeder/control"
            val message = "STATUS"
            
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 1
            mqttMessage.isRetained = false

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] 📊 Consultando estado del comedero..."
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al consultar estado")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun openFeeder() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val topic = "petfeeder/control"
            val message = "OPEN"
            
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 1
            mqttMessage.isRetained = false

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] 🔓 Comando enviado: Abrir comedero"
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al abrir comedero")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun closeFeeder() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val topic = "petfeeder/control"
            val message = "CLOSE"
            
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 1
            mqttMessage.isRetained = false

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] 🔒 Comando enviado: Cerrar comedero"
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("❌ Error al cerrar comedero")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun convertToWebSocketUri(brokerUri: String): String {
        val hostWithPort = brokerUri.removePrefix("tcp://").removePrefix("ssl://")
        val host = hostWithPort.split(":")[0]

        return when {
            host == "test.mosquitto.org" -> "ws://$host:8080"
            brokerUri.startsWith("ssl://") -> "wss://$hostWithPort"
            else -> "ws://$hostWithPort"
        }
    }

    private fun updateStatus(status: String) {
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(if (isConnected) 0xFF4CAF50.toInt() else 0xFFFF0000.toInt())
    }

    private fun addMessage(message: String) {
        runOnUiThread {
            val currentText = binding.tvMessages.text.toString()
            val newText = if (currentText.isEmpty()) message else "$currentText\n$message"
            binding.tvMessages.text = newText
        }
    }
} 