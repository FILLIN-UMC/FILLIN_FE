package com.example.fillin.data.repository

import com.example.fillin.data.kakao.KakaoApiService
import com.example.fillin.data.kakao.Place
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.domain.repository.PlaceRepository

class KakaoPlaceRepository(
    private val api: KakaoApiService,
    private val apiKey: String
) : PlaceRepository {

    override suspend fun searchPlaces(
        query: String,
        x: Double?,
        y: Double?,
        radius: Int?
    ): List<PlaceItem> {
        val response = api.searchKeyword(
            token = "KakaoAK $apiKey",
            query = query,
            x = x,
            y = y,
            radius = radius,
            size = 15
        )
        return response.documents.map { it.toPlaceItem() }
    }
}

private fun Place.toPlaceItem(): PlaceItem = PlaceItem(
    id = "${place_name}_${address_name}_${x}_${y}",
    name = place_name,
    address = address_name,
    category = "",
    x = x,
    y = y
)
