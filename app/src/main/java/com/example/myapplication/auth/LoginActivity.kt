package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.API.ApiClient
import com.example.myapplication.auth.models.LoginRequest
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.main.MainActivity
import com.example.myapplication.utils.PrefManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)

        binding.apply {
            buttonLogin.setOnClickListener { handleLogin() }
            textViewRegister.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }

        if (prefManager.isLoggedIn()) {
            navigateToMain()
        }
    }

    private fun handleLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.editTextEmail.error = "Email requerido"
                binding.editTextEmail.requestFocus()
            }
            password.isEmpty() -> {
                binding.editTextPassword.error = "Contraseña requerida"
                binding.editTextPassword.requestFocus()
            }
            else -> authenticateUser(email, password)
        }
    }

    private fun authenticateUser(email: String, password: String) {
        val request = LoginRequest(correo = email, clave = password)

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.login(request)
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        prefManager.saveLoginStatus(true)
                        prefManager.saveUserEmail(email)
                        prefManager.saveUserToken(token)
                        navigateToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, "Token inválido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }

        // Autenticación de ejemplo (en producción usar Firebase/API)
        if (email == "usuario@ejemplo.com" && password == "123456") {
            prefManager.saveLoginStatus(true)
            prefManager.saveUserEmail(email)
            navigateToMain()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish() // Move this outside the apply block
    }
}