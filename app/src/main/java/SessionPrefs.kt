package com.example.studentregistration

import android.content.Context
import androidx.core.content.edit

class SessionPrefs(context: Context) {

    private val prefs =
        context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    var currentUserEmail: String?
        get() = prefs.getString("current_user_email", null)
        set(value) = prefs.edit {
            if (value == null) remove("current_user_email")
            else putString("current_user_email", value)
        }

    var collegeName: String?
        get() = prefs.getString("collegeName", "")
        set(value) = prefs.edit {
            putString("collegeName", value)
        }

    // ✅ NEW — load/save arrear status
    var hasArrears: Boolean
        get() = prefs.getBoolean("HAS_ARREARS", false)
        set(value) = prefs.edit {
            putBoolean("HAS_ARREARS", value)
        }

    // ✅ NEW — load/save arrear count
    var arrearsCount: Int
        get() = prefs.getInt("ARREARS_COUNT", 0)
        set(value) = prefs.edit {
            putInt("ARREARS_COUNT", value)
        }

    fun logout() {
        prefs.edit {
            remove("current_user_email")
            remove("collegeName")
            remove("HAS_ARREARS")
            remove("ARREARS_COUNT")
        }
    }
}