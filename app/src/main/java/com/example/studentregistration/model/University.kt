package com.example.studentregistration.model

import com.google.gson.annotations.SerializedName

data class University(
    val name: String,
    val country: String,
    @SerializedName(value = "state_province", alternate = ["state-province"])
    val state_province: String? = null,
    val alpha_two_code: String? = null,
    val domains: List<String> = emptyList(),
    val web_pages: List<String> = emptyList()
)