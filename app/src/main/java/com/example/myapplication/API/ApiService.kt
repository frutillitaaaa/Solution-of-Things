package com.example.myapplication.API

import com.example.myapplication.alimentacion.models.AlimentacionRequest
import com.example.myapplication.alimentacion.models.AlimentacionResponse
import com.example.myapplication.auth.models.LoginRequest
import com.example.myapplication.auth.models.LoginResponse
import com.example.myapplication.auth.models.RegistroRequest
import com.example.myapplication.auth.models.RegistroResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/registro")
    suspend fun register(@Body request: RegistroRequest): Response<RegistroResponse>

    @GET("alimentaciones/{id}")
    suspend fun getAlimentaciones(@Path("id") userId: Int): Response<List<AlimentacionResponse>>

    @POST("alimentaciones/{id}")
    suspend fun crearAlimentaciones(@Path("id") userId: Int, @Body request: AlimentacionRequest): Response<AlimentacionResponse>

    @PUT("alimentaciones/{id}")
    suspend fun updateAlimentacion(@Path("id") id: Int, @Body request: AlimentacionRequest): Response<AlimentacionResponse>

    @DELETE("alimentaciones/{id}")
    suspend fun deleteAlimentacion(@Path("id") id: Int): Response<AlimentacionResponse>
}