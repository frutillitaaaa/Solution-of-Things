package com.example.myapplication.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val profilePicUri: String = ""
)