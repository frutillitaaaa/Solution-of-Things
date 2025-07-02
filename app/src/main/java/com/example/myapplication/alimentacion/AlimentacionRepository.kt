package com.example.myapplication.alimentacion

import com.example.myapplication.API.ApiService
import com.example.myapplication.alimentacion.models.AlimentacionRequest
import com.example.myapplication.alimentacion.models.AlimentacionResponse

class AlimentacionRepository (private val api: ApiService) {

    suspend fun obtenerAlimentaciones(id_usuario: Int): List<AlimentacionResponse>? {
        val response = api.getAlimentaciones(id_usuario)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun crearAlimentacion(id_usuario: Int, request: AlimentacionRequest): AlimentacionResponse? {
        val response = api.crearAlimentaciones(id_usuario, request)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun eliminarAlimentacion(id: Int): AlimentacionResponse? {
        val response = api.deleteAlimentacion(id)
        return if (response.isSuccessful) response.body() else null
    }
}