package com.demo.yelp.network

import com.demo.yelp.models.ResponseModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface RetroService {

    @GET("v3/businesses/search")
    fun search(
        @Header("Authorization") authHeader: String?,
        @Query("location") location: String?,
        @Query("latitude") latitude	: String?,
        @Query("longitude") longitude: String?,
        @Query("limit") limit: String?,
        @Query("radius") radius: String?,
        @Query("sort_by") sort_by: String?,
        @Query("term") term: String?,
        @Query("offset") offset: String?
    ): Call<ResponseModel>

}