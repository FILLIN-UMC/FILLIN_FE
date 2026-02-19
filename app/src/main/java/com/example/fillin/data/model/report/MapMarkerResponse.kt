package com.example.fillin.data.model.report

import com.google.gson.annotations.SerializedName

data class MapMarkerResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("category") val category: String,
    @SerializedName("imageUrl") val imageUrl: String?
)