package com.example.studentregistration

data class Student(
    val id: Int,
    val name: String,
    val hasPaidFees: Boolean
) {
    val badgeText: String get() = if (hasPaidFees) "PAID" else "DUE"
}