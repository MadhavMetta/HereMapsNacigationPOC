package com.example.navigation_routing_fe_poc.network

import com.example.navigation_routing_fe_poc.objects.CalculatedRouteRequest
import com.example.navigation_routing_fe_poc.response.CalculatedRouteResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface APIClient {
    @POST("calculateRoute")
    fun getCalculatedRoute(@Body calculatedRouteRequest: CalculatedRouteRequest):Call<CalculatedRouteResponse>
}