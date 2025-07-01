package com.example.myapplication.mqtt

import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.databinding.ActivityMosquitoTestBinding

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private var mqttService: MqttService? = null
    private var isServiceBound = false
    private var isConnected = false
    private var horaSeleccionada: String? = null
    private var horaDesayuno: String? = null
    private var horaAlmuerzo: String? = null
    private var horaMerienda: String? = null
    private var horaCena: String? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MqttService.LocalBinder
            mqttService = binder.getService()
            isServiceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            mqttService = null
            isServiceBound = false
        }
    }

    private val mqttMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MqttService.ACTION_MQTT_MESSAGE) {
                val topic = intent.getStringExtra(MqttService.EXTRA_TOPIC)
                val message = intent.getStringExtra(MqttService.EXTRA_MESSAGE)
                val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val receivedMessage = "[$timestamp] Mensaje de '$topic': $message"
                addMessage(receivedMessage)
            } else if (intent?.action == "MQTT_CONNECTION_LOST") {
                isConnected = false
                updateStatus("Conexión perdida")
            } else if (intent?.action == "MQTT_CONNECTION_RESTORED") {
                isConnected = true
                updateStatus("Conectado")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Iniciar y enlazar el servicio
        val intent = Intent(this, MqttService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        // Registrar el receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mqttMessageReceiver,
            IntentFilter(MqttService.ACTION_MQTT_MESSAGE)
        )
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
        mqttService?.connect(serverUri, clientId) { success, serverURI ->
            runOnUiThread {
                isConnected = success
                if (success) {
                    updateStatus("Conectado")
                    Toast.makeText(this, "Conectado exitosamente a $serverURI", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatus("Error al conectar")
                    Toast.makeText(this, "Error al conectar", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun disconnectFromMqtt() {
        mqttService?.disconnect()
        isConnected = false
        updateStatus("Desconectado")
        Toast.makeText(this, "Desconectado exitosamente", Toast.LENGTH_SHORT).show()
    }

    private fun publishMessage() {
        val topic = binding.etTopic.text.toString()
        val messageText = binding.etMessage.text.toString()
        if (topic.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(this, "Completa el tópico y el mensaje", Toast.LENGTH_SHORT).show()
            return
        }
        mqttService?.publish(topic, messageText) { success ->
            runOnUiThread {
                if (success) {
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    val sentMessage = "[$timestamp] Enviado a '$topic': $messageText"
                    addMessage(sentMessage)
                } else {
                    addMessage("Error al enviar a '$topic'")
                }
            }
        }
    }

    private fun subscribeToTopic() {
        val topic = binding.etTopic.text.toString()
        if (topic.isEmpty()) {
            Toast.makeText(this, "Ingresa un tópico para suscribirte", Toast.LENGTH_SHORT).show()
            return
        }
        mqttService?.subscribe(topic) { success ->
            runOnUiThread {
                val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                if (success) {
                    val subscribeMsg = "[$timestamp] Suscrito a: $topic"
                    addMessage(subscribeMsg)
                } else {
                    addMessage("Error al suscribirse a '$topic'")
                }
            }
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
            mqttService?.publish(topic, messageText) { success ->
                runOnUiThread {
                    if (success) {
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val sentMessage = "[$timestamp] Enviado a '$topic': $messageText"
                        addMessage(sentMessage)
                    } else {
                        addMessage("Error al enviar a '$topic'")
                    }
                }
            }
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
            mqttService?.publish(topic, messageText) { success ->
                runOnUiThread {
                    if (success) {
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val sentMessage = "[$timestamp] Enviado a '$topic': $messageText"
                        addMessage(sentMessage)
                    } else {
                        addMessage("Error al enviar a '$topic'")
                    }
                }
            }
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
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttMessageReceiver)
        super.onDestroy()
    }
} 
} 