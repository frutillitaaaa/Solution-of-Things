package com.example.myapplication.alimentacion.models

data class AlimentacionResponse(
    val id: Int,
    val id_usuario: Int,
    val numero_comida: Int,
    val hora: String,
    val cantidad_comida: Int
)