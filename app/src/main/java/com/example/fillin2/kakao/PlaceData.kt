package com.example.fillin2.kakao

// 카카오 장소 검색 API 응답 전체를 담는 그릇
data class KakaoSearchResponse(
    val documents: List<Place> // 검색 결과 리스트
)

// 개별 장소 정보
data class Place(
    val place_name: String,   // 장소 이름 (예: 강남역)
    val address_name: String, // 전체 지번 주소
    val x: String,            // 경도 (Longitude)
    val y: String             // 위도 (Latitude)
)