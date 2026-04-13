package com.example.studentregistration

data class ReferralModel(
    val name: String? = "",
    val phone: String? = "",
    val email: String? = "",
    val department: String? = "",
    val note: String? = "",
    val referrerUid: String? = "",
    val timestamp: Long? = 0
)