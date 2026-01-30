package com.example.fillin.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fillin.BuildConfig
import com.example.fillin.data.kakao.RetrofitClient
import com.example.fillin.data.repository.FakeHotReportRepository
import com.example.fillin.data.repository.HotReportRepository
import com.example.fillin.data.repository.KakaoPlaceRepository

class SearchViewModelFactory(
    private val context: Context,
    private val hotRepo: HotReportRepository = FakeHotReportRepository(
        radiusMeters = 3000,
        maxItems = 20
    )
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val placeRepo = KakaoPlaceRepository(
            api = RetrofitClient.kakaoApi,
            apiKey = BuildConfig.KAKAO_REST_API_KEY
        )
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(
                context,
                placeRepo,
                hotRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
