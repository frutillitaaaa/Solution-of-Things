package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.main.MainActivity
import com.example.myapplication.utils.PrefManager
import com.example.myapplication.utils.Validator

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
            !Validator.isValidEmail(email) -> {
                binding.editTextEmail.error = "Formato de email inválido"
                binding.editTextEmail.requestFocus()
            }
            password.isEmpty() -> {
                binding.editTextPassword.error = "Contraseña requerida"
                binding.editTextPassword.requestFocus()
            }
            !Validator.isValidPassword(password) -> {
                binding.editTextPassword.error = "La contraseña debe tener al menos 6 caracteres"
                binding.editTextPassword.requestFocus()
            }
            else -> authenticateUser(email, password)
        }
    }

    private fun authenticateUser(email: String, password: String) {
        // Para desarrollo, permitimos cualquier email válido con contraseña de al menos 6 caracteres
        if (Validator.isValidEmail(email) && password.length >= 6) {
            prefManager.saveLoginStatus(true)
            prefManager.saveUserEmail(email)
            Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
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