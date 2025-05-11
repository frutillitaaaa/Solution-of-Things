package com.example.myapplication.auth.models

data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val password: String
)
