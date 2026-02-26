package com.example.studentregistration.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val registerNo: String,
    val rollNo: String,
    val address: String,
    val phone: String,
    val email: String,
    val password: String,
    val dob: String,
    val gender: String,
    val parentName: String,
    val department: String,
    val semester: String
)

