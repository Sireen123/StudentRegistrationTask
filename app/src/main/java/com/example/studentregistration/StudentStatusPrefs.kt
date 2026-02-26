package com.example.studentregistration

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

class StudentStatusPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("student_status_prefs", Context.MODE_PRIVATE)

    private val keyPaid = "paid_map"

    fun setPaid(studentId: Int, isPaid: Boolean) {
        val map = JSONObject(prefs.getString(keyPaid, "{}") ?: "{}")
        map.put(studentId.toString(), isPaid)
        prefs.edit { putString(keyPaid, map.toString()) }
    }

    fun isPaid(studentId: Int): Boolean {
        val map = JSONObject(prefs.getString(keyPaid, "{}") ?: "{}")
        return map.optBoolean(studentId.toString(), false)
    }
}