package com.example.myapplication.alimentacion.models

data class AlimentacionResponse(
    val id: Int,
    val userId: Int,
    val numeroComida: Int,
    val hora: String,
    val cantidadComida: Int
)
