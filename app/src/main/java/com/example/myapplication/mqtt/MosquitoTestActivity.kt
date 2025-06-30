package com.example.myapplication.mqtt

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private lateinit var mqttClient: MqttAndroidClient
    private var isConnected = false
    private var horaSeleccionada: String? = null
    private var horaDesayuno: String? = null
    private var horaAlmuerzo: String? = null
    private var horaMerienda: String? = null
    private var horaCena: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener { connectToMqtt() }
        binding.btnDisconnect.setOnClickListener { disconnectFromMqtt() }
        binding.btnPublish.setOnClickListener { publishMessage() }
        binding.btnSubscribe.setOnClickListener { subscribeToTopic() }
        binding.btnGirarMotor.setOnClickListener { girarMotor() }
        binding.btnProgramarHora.setOnClickListener { mostrarTimePicker() }
        binding.btnConfirmarProgramacion.setOnClickListener { confirmarProgramacion() }
        binding.btnSeleccionarDesayuno.setOnClickListener { mostrarTimePickerComida("desayuno") }
        binding.btnConfirmarDesayuno.setOnClickListener { confirmarProgramacionComida("desayuno") }
        binding.btnSeleccionarAlmuerzo.setOnClickListener { mostrarTimePickerComida("almuerzo") }
        binding.btnConfirmarAlmuerzo.setOnClickListener { confirmarProgramacionComida("almuerzo") }
        binding.btnSeleccionarMerienda.setOnClickListener { mostrarTimePickerComida("merienda") }
        binding.btnConfirmarMerienda.setOnClickListener { confirmarProgramacionComida("merienda") }
        binding.btnSeleccionarCena.setOnClickListener { mostrarTimePickerComida("cena") }
        binding.btnConfirmarCena.setOnClickListener { confirmarProgramacionComida("cena") }
    }

    private fun connectToMqtt() {
        val serverUri = binding.etBroker.text.toString()
        val clientId = binding.etClientId.text.toString()

        if (serverUri.isEmpty() || clientId.isEmpty()) {
            Toast.makeText(this, "Por favor completa Broker y Cliente ID", Toast.LENGTH_SHORT).show()
            return
        }

        // La URI debe ser un WebSocket (ws:// o wss://) para Paho sobre WS
        val webSocketUri = convertToWebSocketUri(serverUri)

        mqttClient = MqttAndroidClient(applicationContext, webSocketUri, clientId)
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                isConnected = true
                runOnUiThread {
                    updateStatus("Conectado")
                    Toast.makeText(this@MosquitoTestActivity, "Conectado exitosamente a $serverURI", Toast.LENGTH_SHORT).show()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                runOnUiThread {
                    updateStatus("Conexión perdida: ${cause?.message}")
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val receivedMessage = "[$timestamp] Mensaje de '$topic': ${message.toString()}"
                addMessage(receivedMessage)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // No es necesario para este ejemplo
            }
        })

        val options = MqttConnectOptions()
        options.isCleanSession = true // Iniciar sesión limpia
        options.connectionTimeout = 10
        options.keepAliveInterval = 60

        try {
            updateStatus("Conectando a $webSocketUri...")
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // La conexión se confirma en connectComplete
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        updateStatus("Error al conectar: ${exception?.message}")
                        Toast.makeText(this@MosquitoTestActivity, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            updateStatus("Error: ${e.message}")
        }
    }

    private fun disconnectFromMqtt() {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) {
            Toast.makeText(this, "Ya estás desconectado", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    isConnected = false
                    runOnUiThread {
                        updateStatus("Desconectado")
                        Toast.makeText(this@MosquitoTestActivity, "Desconectado exitosamente", Toast.LENGTH_SHORT).show()
                    }
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
            message.qos = 1 // Calidad de servicio 1
            message.isRetained = false

            mqttClient.publish(topic, message, null, object : IMqttActionListener {
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
            mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
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

    private fun girarMotor() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }
        val topic = "esp32/motor"
        val messageText = "GIRO"
        try {
            val message = MqttMessage()
            message.payload = messageText.toByteArray()
            message.qos = 1
            message.isRetained = false
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
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

    private fun mostrarTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            horaSeleccionada = String.format("%02d:%02d", selectedHour, selectedMinute)
            binding.etHoraProgramada.setText(horaSeleccionada)
        }, hour, minute, true)
        timePicker.show()
    }

    private fun confirmarProgramacion() {
        val hora = horaSeleccionada
        if (hora.isNullOrEmpty()) {
            Toast.makeText(this, "Selecciona una hora primero", Toast.LENGTH_SHORT).show()
            return
        }
        // Calcular el tiempo hasta la hora programada
        val partes = hora.split(":")
        val horaInt = partes[0].toInt()
        val minutoInt = partes[1].toInt()
        val calendarAhora = Calendar.getInstance()
        val calendarProgramada = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, horaInt)
            set(Calendar.MINUTE, minutoInt)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(calendarAhora)) {
                // Si la hora ya pasó hoy, programa para mañana
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val delayMillis = calendarProgramada.timeInMillis - calendarAhora.timeInMillis
        Toast.makeText(this, "Giro programado para las $hora", Toast.LENGTH_LONG).show()
        addMessage("Giro programado para las $hora")
        binding.btnConfirmarProgramacion.isEnabled = false
        binding.btnProgramarHora.isEnabled = false
        binding.etHoraProgramada.isEnabled = false
        Handler(mainLooper).postDelayed({
            for (i in 1..3) {
                enviarGiroMotor()
            }
            Toast.makeText(this, "Comando de giro enviado 3 veces", Toast.LENGTH_SHORT).show()
            addMessage("Comando de giro enviado 3 veces a las $hora")
            binding.btnConfirmarProgramacion.isEnabled = true
            binding.btnProgramarHora.isEnabled = true
            binding.etHoraProgramada.isEnabled = true
        }, delayMillis)
    }

    private fun enviarGiroMotor() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }
        val topic = "esp32/motor"
        val messageText = "GIRO"
        try {
            val message = MqttMessage()
            message.payload = messageText.toByteArray()
            message.qos = 1
            message.isRetained = false
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
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

    private fun mostrarTimePickerComida(tipo: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val horaFormateada = String.format("%02d:%02d", selectedHour, selectedMinute)
            when (tipo) {
                "desayuno" -> {
                    horaDesayuno = horaFormateada
                    binding.etHoraDesayuno.setText(horaFormateada)
                }
                "almuerzo" -> {
                    horaAlmuerzo = horaFormateada
                    binding.etHoraAlmuerzo.setText(horaFormateada)
                }
                "merienda" -> {
                    horaMerienda = horaFormateada
                    binding.etHoraMerienda.setText(horaFormateada)
                }
                "cena" -> {
                    horaCena = horaFormateada
                    binding.etHoraCena.setText(horaFormateada)
                }
            }
        }, hour, minute, true)
        timePicker.show()
    }

    private fun confirmarProgramacionComida(tipo: String) {
        val hora = when (tipo) {
            "desayuno" -> horaDesayuno
            "almuerzo" -> horaAlmuerzo
            "merienda" -> horaMerienda
            "cena" -> horaCena
            else -> null
        }
        if (hora.isNullOrEmpty()) {
            Toast.makeText(this, "Selecciona una hora para $tipo", Toast.LENGTH_SHORT).show()
            return
        }
        val partes = hora.split(":")
        val horaInt = partes[0].toInt()
        val minutoInt = partes[1].toInt()
        val calendarAhora = Calendar.getInstance()
        val calendarProgramada = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, horaInt)
            set(Calendar.MINUTE, minutoInt)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(calendarAhora)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val delayMillis = calendarProgramada.timeInMillis - calendarAhora.timeInMillis
        Toast.makeText(this, "Giro programado para $tipo a las $hora", Toast.LENGTH_LONG).show()
        addMessage("Giro programado para $tipo a las $hora")
        getConfirmarButton(tipo).isEnabled = false
        getSeleccionarButton(tipo).isEnabled = false
        getEditTextHora(tipo).isEnabled = false
        Handler(mainLooper).postDelayed({
            for (i in 1..3) {
                enviarGiroMotor()
            }
            Toast.makeText(this, "Comando de giro enviado 3 veces para $tipo", Toast.LENGTH_SHORT).show()
            addMessage("Comando de giro enviado 3 veces para $tipo a las $hora")
            getConfirmarButton(tipo).isEnabled = true
            getSeleccionarButton(tipo).isEnabled = true
            getEditTextHora(tipo).isEnabled = true
        }, delayMillis)
    }

    private fun getConfirmarButton(tipo: String) = when (tipo) {
        "desayuno" -> binding.btnConfirmarDesayuno
        "almuerzo" -> binding.btnConfirmarAlmuerzo
        "merienda" -> binding.btnConfirmarMerienda
        "cena" -> binding.btnConfirmarCena
        else -> binding.btnConfirmarDesayuno
    }

    private fun getSeleccionarButton(tipo: String) = when (tipo) {
        "desayuno" -> binding.btnSeleccionarDesayuno
        "almuerzo" -> binding.btnSeleccionarAlmuerzo
        "merienda" -> binding.btnSeleccionarMerienda
        "cena" -> binding.btnSeleccionarCena
        else -> binding.btnSeleccionarDesayuno
    }

    private fun getEditTextHora(tipo: String) = when (tipo) {
        "desayuno" -> binding.etHoraDesayuno
        "almuerzo" -> binding.etHoraAlmuerzo
        "merienda" -> binding.etHoraMerienda
        "cena" -> binding.etHoraCena
        else -> binding.etHoraDesayuno
    }

    private fun convertToWebSocketUri(brokerUri: String): String {
        // Paho espera un formato como "tcp://host:port"
        val hostWithPort = brokerUri.removePrefix("tcp://").removePrefix("ssl://")
        val host = hostWithPort.split(":")[0]
        
        return when {
            host == "test.mosquitto.org" -> "tcp://$host:1883"
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

    override fun onDestroy() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
        }
        super.onDestroy()
    }
} 