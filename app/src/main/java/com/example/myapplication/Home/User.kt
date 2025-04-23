package com.example.myapplication.model

data class User(
    val name: String,
    val email: String,
    val password: String,
    val profilePicUri: String = ""
)