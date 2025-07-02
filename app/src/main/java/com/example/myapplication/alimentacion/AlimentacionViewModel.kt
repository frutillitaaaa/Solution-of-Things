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

    fun cargarAlimentaciones(id_usuario: Int) {
        viewModelScope.launch {
            try {
                val result = repository.obtenerAlimentaciones(id_usuario)
                _alimentaciones.postValue(result ?: emptyList())
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al obtener: ${e.message}")
            }
        }
    }


    fun crearAlimentacion(id_usuario: Int, request: AlimentacionRequest) {
        viewModelScope.launch {
            try {
                repository.crearAlimentacion(id_usuario, request)
                cargarAlimentaciones(id_usuario)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al crear: ${e.message}")
            }
        }
    }



    fun eliminarAlimentacion(id: Int, id_usuario: Int) {
        viewModelScope.launch {
            try {
                repository.eliminarAlimentacion(id)
                cargarAlimentaciones(id_usuario)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al eliminar: ${e.message}")
            }
        }
    }
}