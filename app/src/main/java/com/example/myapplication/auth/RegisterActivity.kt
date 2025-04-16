package com.example.myapplication.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.utils.Validator

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRegister.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        when {
            name.isEmpty() -> {
                binding.editTextName.error = "Nombre requerido"
                binding.editTextName.requestFocus()
            }
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
            password.length < 6 -> {
                binding.editTextPassword.error = "Mínimo 6 caracteres"
                binding.editTextPassword.requestFocus()
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            }
            else -> completeRegistration(name, email, password)
        }
    }

    private fun completeRegistration(name: String, email: String, password: String) {
        // Aquí iría el registro real en base de datos
        Toast.makeText(this, "Registro exitoso: $email", Toast.LENGTH_SHORT).show()
        finish() // Vuelve al LoginActivity
    }
}