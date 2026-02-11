package com.example.fillin.domain.model

data class PlaceItem(
    val id: String,
    val name: String,
    val address: String,
    val category: String = "",
    val x: String? = null,  // 경도 (카카오 API)
    val y: String? = null   // 위도
)
