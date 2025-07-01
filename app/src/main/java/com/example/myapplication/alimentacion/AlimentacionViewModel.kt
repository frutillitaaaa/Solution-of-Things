package com.example.myapplication.alimentacion

import android.util.Log
import androidx.lifecycle.*
import com.example.myapplication.alimentacion.models.AlimentacionRequest
import com.example.myapplication.alimentacion.models.AlimentacionResponse
import kotlinx.coroutines.launch


class AlimentacionViewModel(
    private val repository: AlimentacionRepository
) : ViewModel() {

    private val _alimentaciones = MutableLiveData<List<AlimentacionResponse>>()
    val alimentaciones: LiveData<List<AlimentacionResponse>> = _alimentaciones

    fun cargarAlimentaciones(userId: Int) {
        viewModelScope.launch {
            try {
                val result = repository.obtenerAlimentaciones(userId)
                _alimentaciones.postValue(result ?: emptyList())
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al obtener: ${e.message}")
            }
        }
    }


    fun crearAlimentacion(userId: Int, request: AlimentacionRequest) {
        viewModelScope.launch {
            try {
                repository.crearAlimentacion(userId, request)
                cargarAlimentaciones(userId)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al crear: ${e.message}")
            }
        }
    }

    fun actualizarAlimentacion(id: Int, request: AlimentacionRequest, userId: Int) {
        viewModelScope.launch {
            try {
                repository.actualizarAlimentacion(id, request)
                cargarAlimentaciones(userId)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al actualizar: ${e.message}")
            }
        }
    }

    fun eliminarAlimentacion(id: Int, userId: Int) {
        viewModelScope.launch {
            try {
                repository.eliminarAlimentacion(id)
                cargarAlimentaciones(userId)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al eliminar: ${e.message}")
            }
        }
    }
}
