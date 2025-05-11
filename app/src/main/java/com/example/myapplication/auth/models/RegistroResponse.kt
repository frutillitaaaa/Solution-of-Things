package com.example.myapplication.auth.models

data class RegistroResponse(
    val id: Int,
    val nombre: String,
    val correo: String,
    val password: String
)
