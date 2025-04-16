package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.loginejemplo.databinding.ActivityLoginBinding
import com.example.loginejemplo.main.MainActivity
import com.example.loginejemplo.utils.PrefManager
import com.example.loginejemplo.utils.Validator

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)

        setupUI()
        checkSavedUser()
    }

    private fun setupUI() {
        binding.apply {
            buttonLogin.setOnClickListener { handleLogin() }
            textViewRegister.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }
    }

    private fun checkSavedUser() {
        if (prefManager.isLoggedIn()) {
            navigateToMain()
        }
    }

    private fun handleLogin() {
        val email = binding.editTextLoginEmail.text.toString().trim()
        val password = binding.editTextLoginPassword.text.toString().trim()

        when {
            !Validator.isValidEmail(email) -> {
                binding.editTextLoginEmail.error = "Email inválido"
                binding.editTextLoginEmail.requestFocus()
            }
            password.length < 6 -> {
                binding.editTextLoginPassword.error = "Mínimo 6 caracteres"
                binding.editTextLoginPassword.requestFocus()
            }
            else -> performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        // Simulación de login - en una app real aquí iría tu API call
        if (email == "usuario@ejemplo.com" && password == "123456") {
            prefManager.saveUser(email, password)
            navigateToMain()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}