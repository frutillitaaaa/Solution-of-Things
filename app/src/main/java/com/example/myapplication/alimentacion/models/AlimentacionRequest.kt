package com.example.myapplication.alimentacion.models

data class AlimentacionRequest(
    val numero_comida: Int,
    val hora: String,
    val cantidad_comida: Int
)