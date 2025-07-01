package com.example.myapplication.alimentacion

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.API.ApiClient
import com.example.myapplication.API.ApiService
import com.example.myapplication.R
import com.example.myapplication.alimentacion.models.AlimentacionRequest

class AlimentacionActivity : AppCompatActivity() {

    private lateinit var viewModel: AlimentacionViewModel
    private lateinit var txtResultado: TextView
    private var userId: Int = -1  // ID real del usuario, recibido desde LoginActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alimentacion)

        // ⬇️ Obtener userId desde el Intent
        userId = intent.getIntExtra("userId", -1)
        if (userId == -1) {
            Toast.makeText(this, "ID de usuario inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Referencias a la UI
        val btnCargar = findViewById<Button>(R.id.btnCargar)
        val btnCrear = findViewById<Button>(R.id.btnCrear)
        txtResultado = findViewById(R.id.txtResultado)

        // Instanciar Retrofit y Repository
        val apiService = ApiClient.apiService
        val repository = AlimentacionRepository(apiService)

        // Crear ViewModel con Factory
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlimentacionViewModel(repository) as T
            }
        })[AlimentacionViewModel::class.java]

        // Observar cambios en la lista de alimentaciones
        viewModel.alimentaciones.observe(this) { lista ->
            if (lista.isEmpty()) {
                txtResultado.text = "No hay alimentaciones registradas."
            } else {
                txtResultado.text = lista.joinToString("\n") {
                    "ID ${it.id}: Comida ${it.numeroComida}, Hora: ${it.hora}, Cantidad: ${it.cantidadComida}"
                }
            }
        }

        // Botón para cargar alimentaciones
        btnCargar.setOnClickListener {
            viewModel.cargarAlimentaciones(userId)
        }

        // Botón para crear una nueva alimentación de prueba
        btnCrear.setOnClickListener {
            val nueva = AlimentacionRequest(
                userId = userId,
                numeroComida = 1,
                hora = "08:00",
                cantidadComida = 2
            )
            viewModel.crearAlimentacion(userId, nueva)
            Toast.makeText(this, "Alimentación creada", Toast.LENGTH_SHORT).show()
        }
    }
}
