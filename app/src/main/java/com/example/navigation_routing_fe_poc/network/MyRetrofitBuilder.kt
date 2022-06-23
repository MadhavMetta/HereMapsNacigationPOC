package com.example.navigation_routing_fe_poc.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object MyRetrofitBuilder {
    private const val API_BASE_URL = "http://172.18.3.172:3000/api/aws/"
    private val httpClient:OkHttpClient.Builder = OkHttpClient.Builder()
    private var builder: Retrofit.Builder = Retrofit.Builder()
        .baseUrl(API_BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create()
        )
    private var retrofit: Retrofit = builder
        .client(httpClient.build())
        .build()
    fun getRetrofit():Retrofit{
        return retrofit
    }
}