package com.example.myapplication.wifi

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.R

class WifiPasswordDialog(
    context: Context,
    private val onPasswordSubmit: (String) -> Unit
) {
    private var dialog: AlertDialog? = null
    private lateinit var editTextPassword: EditText

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_wifi_password, null)
        editTextPassword = view.findViewById(R.id.editTextPassword)

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setPositiveButton("Conectar") { _, _ ->
                val password = editTextPassword.text.toString()
                if (password.isNotEmpty()) {
                    onPasswordSubmit(password)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    fun show() {
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
} 