package com.example.studentregistration.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DatasetNetworkModule {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: UniversityDatasetApi by lazy {
        retrofit.create(UniversityDatasetApi::class.java)
    }

    const val DATASET_URL =
        "https://raw.githubusercontent.com/Hipo/university-domains-list/master/world_universities_and_domains.json"
}