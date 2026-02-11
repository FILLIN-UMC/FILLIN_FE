package com.example.fillin.domain.repository

import com.example.fillin.domain.model.PlaceItem

interface PlaceRepository {
    suspend fun searchPlaces(
        query: String,
        x: Double? = null,
        y: Double? = null,
        radius: Int? = null
    ): List<PlaceItem>
}
