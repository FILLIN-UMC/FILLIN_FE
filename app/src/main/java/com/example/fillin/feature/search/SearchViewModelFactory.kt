package com.example.fillin.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fillin.data.remote.kakao.KakaoRetrofit
import com.example.fillin.data.repository.FakeHotReportRepository
import com.example.fillin.data.repository.HotReportRepository
import com.example.fillin.data.repository.KakaoPlaceRepository

class SearchViewModelFactory(
    private val context: Context,
    // ✅ 위치 기반 HOT 더미 데이터: 반경 3km 로 변경
    private val hotRepo: HotReportRepository = FakeHotReportRepository(
        radiusMeters = 3000,
        maxItems = 20
    )
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // ✅ PlaceRepo (Kakao)
        val placeRepo = KakaoPlaceRepository(KakaoRetrofit.api)

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
