package com.example.myapplication.alimentacion

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.API.ApiClient
import com.example.myapplication.alimentacion.models.AlimentacionRequest
import com.example.myapplication.alimentacion.models.AlimentacionResponse
import com.example.myapplication.mqtt.MqttManager
import com.example.myapplication.utils.PrefManager
import kotlinx.coroutines.launch

class AlimentacionActivity : AppCompatActivity() {

    private lateinit var txtResultado: TextView
    private var id_usuario: Int = -1

    private var horaDesayuno: String? = null
    private var horaAlmuerzo: String? = null
    private var horaCuartaComida: String? = null
    private var horaCena: String? = null

    private var cantComidaDesayuno: Int? = 0
    private var cantComidaAlmuerzo: Int? = 0
    private var cantComidaCena: Int? = 0
    private var cantComidaCuartaComida: Int? = 0

    private val alimentacionIds = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alimentacion)

        val prefManager = PrefManager(this)
        id_usuario = prefManager.getUserId()
        if (id_usuario == -1) {
            Toast.makeText(this, "ID de usuario inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val cantidades = (1..10).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cantidades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        findViewById<Spinner>(R.id.spCantidadDesayuno).adapter = adapter
        findViewById<Spinner>(R.id.spCantidadAlmuerzo).adapter = adapter
        findViewById<Spinner>(R.id.spCantidadCuartaComida).adapter = adapter
        findViewById<Spinner>(R.id.spCantidadCena).adapter = adapter

        val btnCargar = findViewById<Button>(R.id.btnCargar)
        val btnGirarMotor = findViewById<Button>(R.id.btnGirarMotor)
        txtResultado = findViewById(R.id.txtResultado)

        btnCargar.setOnClickListener {
            txtResultado.text = "Simulando carga para id_usuario=$id_usuario"
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

        listOf("desayuno", "almuerzo", "cena", "cuartaComida").forEach { tipo ->
            getSeleccionarButton(tipo).setOnClickListener { mostrarTimePickerComida(tipo) }
            getConfirmarButton(tipo).setOnClickListener {
                val cantidad = when (tipo) {
                    "desayuno" -> findViewById<Spinner>(R.id.spCantidadDesayuno).selectedItem as Int
                    "almuerzo" -> findViewById<Spinner>(R.id.spCantidadAlmuerzo).selectedItem as Int
                    "cena" -> findViewById<Spinner>(R.id.spCantidadCena).selectedItem as Int
                    "cuartaComida" -> findViewById<Spinner>(R.id.spCantidadCuartaComida).selectedItem as Int
                    else -> 1
                }

                when (tipo) {
                    "desayuno" -> cantComidaDesayuno = cantidad
                    "almuerzo" -> cantComidaAlmuerzo = cantidad
                    "cena" -> cantComidaCena = cantidad
                    "cuartaComida" -> cantComidaCuartaComida = cantidad
                }

                confirmarProgramacionComida(tipo)
            }
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAlimentaciones(id_usuario)
                if (response.isSuccessful) {
                    val comidas = response.body() ?: emptyList()
                    for (comida in comidas) {
                        when (comida.numero_comida) {
                            1 -> {
                                horaDesayuno = comida.hora
                                alimentacionIds["desayuno"] = comida.id
                                findViewById<EditText>(R.id.etHoraDesayuno).setText(comida.hora)
                                findViewById<Spinner>(R.id.spCantidadDesayuno).setSelection((comida.cantidad_comida - 1).coerceAtLeast(0))
                            }
                            2 -> {
                                horaAlmuerzo = comida.hora
                                alimentacionIds["almuerzo"] = comida.id
                                findViewById<EditText>(R.id.etHoraAlmuerzo).setText(comida.hora)
                                findViewById<Spinner>(R.id.spCantidadAlmuerzo).setSelection((comida.cantidad_comida - 1).coerceAtLeast(0))
                            }
                            3 -> {
                                horaCena = comida.hora
                                alimentacionIds["cena"] = comida.id
                                findViewById<EditText>(R.id.etHoraCena).setText(comida.hora)
                                findViewById<Spinner>(R.id.spCantidadCena).setSelection((comida.cantidad_comida - 1).coerceAtLeast(0))
                            }
                            4 -> {
                                horaCuartaComida = comida.hora
                                alimentacionIds["cuartaComida"] = comida.id
                                findViewById<EditText>(R.id.etHoraCuartaComida).setText(comida.hora)
                                findViewById<Spinner>(R.id.spCantidadCuartaComida).setSelection((comida.cantidad_comida - 1).coerceAtLeast(0))
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@AlimentacionActivity, "No se pudieron obtener alimentaciones", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AlimentacionActivity, "Error al obtener alimentaciones: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun mostrarTimePickerComida(tipo: String) {
        val now = java.util.Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            val hora = String.format("%02d:%02d", h, m)
            when (tipo) {
                "desayuno" -> {
                    horaDesayuno = hora
                    findViewById<EditText>(R.id.etHoraDesayuno).setText(hora)
                }
                "almuerzo" -> {
                    horaAlmuerzo = hora
                    findViewById<EditText>(R.id.etHoraAlmuerzo).setText(hora)
                }
                "cuartaComida" -> {
                    horaCuartaComida = hora
                    findViewById<EditText>(R.id.etHoraCuartaComida).setText(hora)
                }
                "cena" -> {
                    horaCena = hora
                    findViewById<EditText>(R.id.etHoraCena).setText(hora)
                }
            }
        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
    }

    private fun confirmarProgramacionComida(tipo: String) {
        val hora = when (tipo) {
            "desayuno" -> horaDesayuno
            "almuerzo" -> horaAlmuerzo
            "cena" -> horaCena
            "cuartaComida" -> horaCuartaComida
            else -> null
        } ?: return

        val cantComida = when (tipo) {
            "desayuno" -> cantComidaDesayuno
            "almuerzo" -> cantComidaAlmuerzo
            "cena" -> cantComidaCena
            "cuartaComida" -> cantComidaCuartaComida
            else -> 0
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
            repeat(cantComida) {
                MqttManager.publish("esp32/motor", "GIRO", {}, {})
            }
        }, delay)

        lifecycleScope.launch {
            val request = AlimentacionRequest(
                numero_comida = when (tipo) {
                    "desayuno" -> 1
                    "almuerzo" -> 2
                    "cuartaComida" -> 3
                    "cena" -> 4
                    else -> 0
                },
                hora = hora,
                cantidad_comida = cantComida
            )

            try {
                val response = ApiClient.apiService.crearAlimentaciones(id_usuario, request)
                if (response.isSuccessful) {
                    val idAlimentacion = response.body()?.id
                    if (idAlimentacion != null) {
                        alimentacionIds[tipo] = idAlimentacion
                    }
                    Toast.makeText(this@AlimentacionActivity, "Horario guardado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AlimentacionActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@AlimentacionActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getConfirmarButton(tipo: String) = when (tipo) {
        "desayuno" -> findViewById<Button>(R.id.btnConfirmarDesayuno)
        "almuerzo" -> findViewById<Button>(R.id.btnConfirmarAlmuerzo)
        "cena" -> findViewById<Button>(R.id.btnConfirmarCena)
        "cuartaComida" -> findViewById<Button>(R.id.btnConfirmarCuartaComida)
        else -> findViewById<Button>(R.id.btnConfirmarDesayuno)
    }

    private fun getSeleccionarButton(tipo: String) = when (tipo) {
        "desayuno" -> findViewById<Button>(R.id.btnSeleccionarDesayuno)
        "almuerzo" -> findViewById<Button>(R.id.btnSeleccionarAlmuerzo)
        "cena" -> findViewById<Button>(R.id.btnSeleccionarCena)
        "cuartaComida" -> findViewById<Button>(R.id.btnSeleccionarCuartaComida)
        else -> findViewById<Button>(R.id.btnSeleccionarDesayuno)
    }
}
