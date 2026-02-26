package com.example.studentregistration

import android.content.Context
import androidx.core.content.edit

class SessionPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    var currentUserEmail: String?
        get() = prefs.getString("current_user_email", null)
        set(value) = prefs.edit {
            if (value == null) remove("current_user_email")
            else putString("current_user_email", value)
        }

    fun logout() {
        prefs.edit {
            remove("current_user_email")
        }
    }
}