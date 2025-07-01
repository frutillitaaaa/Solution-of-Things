package com.example.myapplication.alimentacion

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.API.ApiClient
import com.example.myapplication.R
import com.example.myapplication.mqtt.MqttManager

class AlimentacionActivity : AppCompatActivity() {

    private lateinit var txtResultado: TextView
    private var userId: Int = -1

    private var horaSeleccionada: String? = null
    private var horaDesayuno: String? = null
    private var horaAlmuerzo: String? = null
    private var horaMerienda: String? = null
    private var horaCena: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alimentacion)

        userId = intent.getIntExtra("userId", -1)
        if (userId == -1) {
            Toast.makeText(this, "ID de usuario inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnCargar = findViewById<Button>(R.id.btnCargar)
        val btnCrear = findViewById<Button>(R.id.btnCrear)
        val btnGirarMotor = findViewById<Button>(R.id.btnGirarMotor)
        txtResultado = findViewById(R.id.txtResultado)

        btnCargar.setOnClickListener {
            txtResultado.text = "Simulando carga para userId=$userId"
        }

        btnCrear.setOnClickListener {
            txtResultado.text = "Simulando creación para userId=$userId"
        }

        btnGirarMotor.setOnClickListener {
            MqttManager.publish("esp32/motor", "GIRO",
                onSuccess = {
                    Toast.makeText(this, "Comando GIRO enviado", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(this, "Error al enviar comando GIRO", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<Button>(R.id.btnProgramarHora).setOnClickListener { mostrarTimePicker() }
        findViewById<Button>(R.id.btnConfirmarProgramacion).setOnClickListener { confirmarProgramacion() }

        listOf("desayuno", "almuerzo", "merienda", "cena").forEach { tipo ->
            getSeleccionarButton(tipo).setOnClickListener { mostrarTimePickerComida(tipo) }
            getConfirmarButton(tipo).setOnClickListener { confirmarProgramacionComida(tipo) }
        }
    }

    private fun mostrarTimePicker() {
        val now = java.util.Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            horaSeleccionada = String.format("%02d:%02d", h, m)
            findViewById<EditText>(R.id.etHoraProgramada).setText(horaSeleccionada)
        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
    }

    private fun confirmarProgramacion() {
        val hora = horaSeleccionada ?: return
        val partes = hora.split(":").map { it.toInt() }
        val ahora = java.util.Calendar.getInstance()
        val objetivo = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, partes[0])
            set(java.util.Calendar.MINUTE, partes[1])
            set(java.util.Calendar.SECOND, 0)
            if (before(ahora)) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val delay = objetivo.timeInMillis - ahora.timeInMillis
        Toast.makeText(this, "Giro programado para las $hora", Toast.LENGTH_SHORT).show()
        Handler(mainLooper).postDelayed({
            repeat(3) {
                MqttManager.publish("esp32/motor", "GIRO", {}, {})
            }
        }, delay)
    }

    private fun mostrarTimePickerComida(tipo: String) {
        val now = java.util.Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            val hora = String.format("%02d:%02d", h, m)
            when (tipo) {
                "desayuno" -> { horaDesayuno = hora; findViewById<EditText>(R.id.etHoraDesayuno).setText(hora) }
                "almuerzo" -> { horaAlmuerzo = hora; findViewById<EditText>(R.id.etHoraAlmuerzo).setText(hora) }
                "merienda" -> { horaMerienda = hora; findViewById<EditText>(R.id.etHoraMerienda).setText(hora) }
                "cena" -> { horaCena = hora; findViewById<EditText>(R.id.etHoraCena).setText(hora) }
            }
        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
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
        val ahora = java.util.Calendar.getInstance()
        val objetivo = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, partes[0])
            set(java.util.Calendar.MINUTE, partes[1])
            set(java.util.Calendar.SECOND, 0)
            if (before(ahora)) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val delay = objetivo.timeInMillis - ahora.timeInMillis
        Toast.makeText(this, "Giro programado para $tipo a las $hora", Toast.LENGTH_SHORT).show()
        Handler(mainLooper).postDelayed({
            repeat(3) {
                MqttManager.publish("esp32/motor", "GIRO", {}, {})
            }
        }, delay)
    }

    private fun getConfirmarButton(tipo: String) = when (tipo) {
        "desayuno" -> findViewById<Button>(R.id.btnConfirmarDesayuno)
        "almuerzo" -> findViewById<Button>(R.id.btnConfirmarAlmuerzo)
        "merienda" -> findViewById<Button>(R.id.btnConfirmarMerienda)
        "cena" -> findViewById<Button>(R.id.btnConfirmarCena)
        else -> findViewById<Button>(R.id.btnConfirmarDesayuno)
    }

    private fun getSeleccionarButton(tipo: String) = when (tipo) {
        "desayuno" -> findViewById<Button>(R.id.btnSeleccionarDesayuno)
        "almuerzo" -> findViewById<Button>(R.id.btnSeleccionarAlmuerzo)
        "merienda" -> findViewById<Button>(R.id.btnSeleccionarMerienda)
        "cena" -> findViewById<Button>(R.id.btnSeleccionarCena)
        else -> findViewById<Button>(R.id.btnSeleccionarDesayuno)
    }
}
