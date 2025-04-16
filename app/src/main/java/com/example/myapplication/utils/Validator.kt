package com.example.myapplication.utils

object Validator {
    private const val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"

    fun isValidEmail(email: String): Boolean {
        return email.matches(EMAIL_REGEX.toRegex()) && email.isNotEmpty()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}