package com.example.fillin2.kakao

import com.example.fillin2.report.locationselect.KakaoAddressResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoApiService {
    @GET("v2/local/search/keyword.json")
    suspend fun searchPlace(
        @Header("Authorization") token: String, // "KakaoAK [내_REST_API_키]"
        @Query("query") query: String          // 검색어 (예: "강남역")
    ): KakaoSearchResponse

    // 2. [추가] 좌표를 주소로 변환하는 기능 (역지오코딩)
    @GET("v2/local/geo/coord2address.json")
    suspend fun getAddressFromCoord(  // "이 좌표(x, y)에 대한 주소를 줘!"라고 요청하는 규칙
        @Header("Authorization") token: String, // "KakaoAK [내_REST_API_키]"
        @Query("x") longitude: Double,         // 경도
        @Query("y") latitude: Double           // 위도
    ): KakaoAddressResponse // 결과 그릇: 이 안에 주소가 담겨서 돌아옴
}