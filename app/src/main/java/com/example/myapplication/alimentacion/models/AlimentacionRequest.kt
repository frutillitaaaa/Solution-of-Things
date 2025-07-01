package com.example.myapplication.alimentacion.models

data class AlimentacionRequest(
    val userId: Int,
    val numeroComida: Int,
    val hora: String,
    val cantidadComida: Int
)
