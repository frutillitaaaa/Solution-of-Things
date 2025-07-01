package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "MyPawsPrefs", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_REGISTERED_USERS = "registered_users"
        private const val KEY_CURRENT_USER = "current_user"
    }

    fun saveUser(user: User) {
        val users = getRegisteredUsers().toMutableList()

        // Check if user with this email already exists
        val existingUserIndex = users.indexOfFirst { it.email == user.email }
        if (existingUserIndex != -1) {
            // Replace existing user
            users[existingUserIndex] = user
        } else {
            // Add new user
            users.add(user)
        }

        // Save updated list
        val usersJson = gson.toJson(users)
        sharedPreferences.edit().putString(KEY_REGISTERED_USERS, usersJson).apply()
    }

    fun getRegisteredUsers(): List<User> {
        val usersJson = sharedPreferences.getString(KEY_REGISTERED_USERS, null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(usersJson, type)
    }

    fun saveCurrentUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_CURRENT_USER, userJson).apply()
    }

    fun getCurrentUser(): User? {
        val userJson = sharedPreferences.getString(KEY_CURRENT_USER, null) ?: return null
        return gson.fromJson(userJson, User::class.java)
    }

    fun clearCurrentUser() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply()
    }

    fun getCurrentUserId(): Int {
        return getCurrentUser()?.id ?: -1
    }
}