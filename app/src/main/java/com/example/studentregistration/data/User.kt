package com.example.studentregistration.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
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

    val email: String,
    val password: String,

    val dob: String,
    val gender: String,
    val parentName: String,
    val department: String,
    val semester: String,

    val role: String,
    val feesPaid: String = "0",

    val profilePhoto: String? = null
) : Parcelable