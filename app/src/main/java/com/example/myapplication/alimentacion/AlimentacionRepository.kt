package com.example.myapplication.alimentacion

import com.example.myapplication.API.ApiService
import com.example.myapplication.alimentacion.models.AlimentacionRequest
import com.example.myapplication.alimentacion.models.AlimentacionResponse

class AlimentacionRepository (private val api: ApiService) {

    suspend fun obtenerAlimentaciones(userId: Int): List<AlimentacionResponse>? {
        val response = api.getAlimentaciones(userId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun crearAlimentacion(userId: Int, request: AlimentacionRequest): AlimentacionResponse? {
        val response = api.crearAlimentaciones(userId, request)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun actualizarAlimentacion(id: Int, request: AlimentacionRequest): AlimentacionResponse? {
        val response = api.updateAlimentacion(id, request)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun eliminarAlimentacion(id: Int): AlimentacionResponse? {
        val response = api.deleteAlimentacion(id)
        return if (response.isSuccessful) response.body() else null
    }
}