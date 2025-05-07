package com.example.myapplication.API

import com.example.myapplication.auth.models.LoginRequest
import com.example.myapplication.auth.models.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/registro")
    suspend fun register(@Body request: RegistroRequest): Response<RegistroResponse>
}