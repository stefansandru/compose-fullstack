package com.example.composetutorial.data.remote

import com.example.composetutorial.data.model.Item
import com.example.composetutorial.data.model.LoginRequest
import com.example.composetutorial.data.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiInterface {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("item")
    suspend fun getItems(@Header("Authorization") token: String): List<Item>

    @POST("item")
    suspend fun addItem(@Header("Authorization") token: String, @Body item: Item): Item

    @PUT("item/{id}")
    suspend fun updateItem(@Header("Authorization") token: String, @Path("id") id: Int, @Body item: Item): Item

    @retrofit2.http.DELETE("item/{id}")
    suspend fun deleteItem(@Header("Authorization") token: String, @Path("id") id: Int)
}
