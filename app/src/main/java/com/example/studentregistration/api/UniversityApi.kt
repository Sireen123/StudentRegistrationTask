package com.example.studentregistration.api

import com.example.studentregistration.model.University
import retrofit2.http.GET
import retrofit2.http.Url

interface UniversityDatasetApi {
    @GET
    suspend fun getAll(@Url url: String): List<University>
}