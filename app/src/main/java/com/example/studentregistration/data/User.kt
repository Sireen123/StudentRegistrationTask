package com.example.studentregistration.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String,
    val registerNo: String,
    val rollNo: String,
    val address: String,
    val phone: String,

    // store normalized: email.trim().lowercase()
    val email: String,

    // store normalized: password.trim()  (do NOT lowercase)
    val password: String,

    val dob: String,
    val gender: String,
    val parentName: String,
    val department: String,
    val semester: String,

    // e.g. "management" or "student"
    val role: String
)
