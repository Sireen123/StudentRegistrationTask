package com.example.studentregistration

import android.content.Context

class SessionStudentPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("student_prefs", Context.MODE_PRIVATE)

    var selectedStudentId: Int
        get() = prefs.getInt("selected_student_id", -1)    // -1 means no selection
        set(value) = prefs.edit().putInt("selected_student_id", value).apply()
}