package com.example.myapplication.historial.models

data class HistorialResponse(
    val id: Int,
    val id_alimentacion: Int,
    val fecha_alimentacion: String,
    val is_completado: Boolean,
)
