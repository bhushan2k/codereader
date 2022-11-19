package com.demo.yelp.Models

data class ResponseModel (
    val businesses: ArrayList<Business>,
    val total: Long,
    val region: Region
)

data class Business (
    val id: String,
    val alias: String,
    val name: String,
    val image_url: String,
    val isClosed: Boolean,
    val url: String,
    val reviewCount: Long,
    val categories: ArrayList<Category>,
    val rating: Double,
    val coordinates: Center,
    val transactions: ArrayList<String>,
    val price: String? = null,
    val location: Location,
    val phone: String,
    val displayPhone: String,
    val distance: Double
)

data class Category (
    val alias: String,
    val title: String
)

data class Center (
    val latitude: Double,
    val longitude: Double
)

data class Location (
    val address1: String,
    val address2: String,
    val address3: String,
    val city: String,
    val zipCode: String,
    val country: String,
    val state: String,
    val displayAddress: ArrayList<String>
)

data class Region (
    val center: Center
)
