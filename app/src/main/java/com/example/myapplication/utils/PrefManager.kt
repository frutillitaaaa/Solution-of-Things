package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {
    private val pref: SharedPreferences =
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    fun saveLoginStatus(isLoggedIn: Boolean) {
        pref.edit().putBoolean("IS_LOGGED_IN", isLoggedIn).apply()
    }

    fun saveUserEmail(email: String) {
        pref.edit().putString("USER_EMAIL", email).apply()
    }

    fun getUserEmail(): String? = pref.getString("USER_EMAIL", null)

    fun isLoggedIn(): Boolean = pref.getBoolean("IS_LOGGED_IN", false)

    fun clearSession() {
        pref.edit().clear().apply()
    }

    fun saveUserName(name: String) {
        pref.edit().putString("USER_NAME", name).apply()
    }


}