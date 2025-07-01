// com/example/myapplication/mqtt/MosquitoTestActivity.kt
package com.example.myapplication.mqtt

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*


class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
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
        binding.btnDisconnect.setOnClickListener { MqttManager.disconnect() }
        binding.btnPublish.setOnClickListener { publishMessage() }
        binding.btnSubscribe.setOnClickListener { subscribeToTopic() }
        binding.btnGirarMotor.setOnClickListener { enviarGiroMotor() }

        listOf("desayuno", "almuerzo", "merienda", "cena").forEach { tipo ->
            getSeleccionarButton(tipo).setOnClickListener { mostrarTimePickerComida(tipo) }
            getConfirmarButton(tipo).setOnClickListener { confirmarProgramacionComida(tipo) }
        }

        binding.btnProgramarHora.setOnClickListener { mostrarTimePicker() }
        binding.btnConfirmarProgramacion.setOnClickListener { confirmarProgramacion() }
    }

    private fun connectToMqtt() {
        val serverUri = convertToWebSocketUri(binding.etBroker.text.toString())
        val clientId = binding.etClientId.text.toString()

        MqttManager.connect(this, serverUri, clientId, object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                runOnUiThread {
                    updateStatus("Conectado")
                    Toast.makeText(this@MosquitoTestActivity, "Conectado a $serverURI", Toast.LENGTH_SHORT).show()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                runOnUiThread { updateStatus("Conexión perdida: ${cause?.message}") }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val msg = "[$timestamp] Mensaje de '$topic': ${message.toString()}"
                addMessage(msg)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
    }

    private fun publishMessage() {
        val topic = binding.etTopic.text.toString()
        val msg = binding.etMessage.text.toString()
        if (topic.isBlank() || msg.isBlank()) return

        MqttManager.publish(topic, msg,
            onSuccess = {
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                addMessage("[$ts] Enviado a '$topic': $msg")
            },
            onFailure = {
                addMessage("Error al enviar a '$topic'")
            }
        )
    }

    private fun subscribeToTopic() {
        val topic = binding.etTopic.text.toString()
        if (topic.isBlank()) return

        MqttManager.subscribe(topic,
            onSuccess = {
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                addMessage("[$ts] Suscrito a: $topic")
            },
            onFailure = {
                addMessage("Error al suscribirse a '$topic'")
            }
        )
    }

    private fun enviarGiroMotor() {
        MqttManager.publish("esp32/motor", "GIRO",
            onSuccess = {
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                addMessage("[$ts] Enviado a 'esp32/motor': GIRO")
            },
            onFailure = {
                addMessage("Error al enviar comando GIRO")
            }
        )
    }

    private fun mostrarTimePicker() {
        val now = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            horaSeleccionada = String.format("%02d:%02d", h, m)
            binding.etHoraProgramada.setText(horaSeleccionada)
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun confirmarProgramacion() {
        val hora = horaSeleccionada ?: return
        val partes = hora.split(":").map { it.toInt() }
        val calendarAhora = Calendar.getInstance()
        val calendarProgramada = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, partes[0])
            set(Calendar.MINUTE, partes[1])
            set(Calendar.SECOND, 0)
            if (before(calendarAhora)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val delayMillis = calendarProgramada.timeInMillis - calendarAhora.timeInMillis
        addMessage("Giro programado para las $hora")
        disableProgramacion(true)

        Handler(mainLooper).postDelayed({
            repeat(3) { enviarGiroMotor() }
            addMessage("Giro ejecutado 3 veces a las $hora")
            disableProgramacion(false)
        }, delayMillis)
    }

    private fun disableProgramacion(disable: Boolean) {
        binding.btnConfirmarProgramacion.isEnabled = !disable
        binding.btnProgramarHora.isEnabled = !disable
        binding.etHoraProgramada.isEnabled = !disable
    }

    private fun mostrarTimePickerComida(tipo: String) {
        val now = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            val hora = String.format("%02d:%02d", h, m)
            when (tipo) {
                "desayuno" -> { horaDesayuno = hora; binding.etHoraDesayuno.setText(hora) }
                "almuerzo" -> { horaAlmuerzo = hora; binding.etHoraAlmuerzo.setText(hora) }
                "merienda" -> { horaMerienda = hora; binding.etHoraMerienda.setText(hora) }
                "cena" -> { horaCena = hora; binding.etHoraCena.setText(hora) }
            }
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun confirmarProgramacionComida(tipo: String) {
        val hora = when (tipo) {
            "desayuno" -> horaDesayuno
            "almuerzo" -> horaAlmuerzo
            "merienda" -> horaMerienda
            "cena" -> horaCena
            else -> null
        } ?: return

        val partes = hora.split(":").map { it.toInt() }
        val ahora = Calendar.getInstance()
        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, partes[0])
            set(Calendar.MINUTE, partes[1])
            set(Calendar.SECOND, 0)
            if (before(ahora)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = objetivo.timeInMillis - ahora.timeInMillis
        addMessage("Giro programado para $tipo a las $hora")
        getConfirmarButton(tipo).isEnabled = false
        getSeleccionarButton(tipo).isEnabled = false
        getEditTextHora(tipo).isEnabled = false

        Handler(mainLooper).postDelayed({
            repeat(3) { enviarGiroMotor() }
            addMessage("Comando enviado 3 veces para $tipo a las $hora")
            getConfirmarButton(tipo).isEnabled = true
            getSeleccionarButton(tipo).isEnabled = true
            getEditTextHora(tipo).isEnabled = true
        }, delay)
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
        binding.tvStatus.setTextColor(if (MqttManager.isConnected) 0xFF4CAF50.toInt() else 0xFFFF0000.toInt())
    }

    private fun addMessage(message: String) {
        val current = binding.tvMessages.text.toString()
        binding.tvMessages.text = if (current.isEmpty()) message else "$current\n$message"
    }

    // NO más onDestroy que desconecte
}