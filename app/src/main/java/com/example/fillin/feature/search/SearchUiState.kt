package com.example.fillin.feature.search

import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem

enum class SearchTab { RECENT, HOT }

sealed class SearchMode {
    data object RecentEmpty : SearchMode()
    data object RecentList : SearchMode()
    data object ResultList : SearchMode()
    data object HotReportList : SearchMode()
}

data class SearchUiState(
    val tab: SearchTab = SearchTab.RECENT,
    val query: String = "",

    // ✅ 로딩도 분리 (최종 UI에서 탭 콘텐츠는 유지)
    val isSearching: Boolean = false,
    val isHotLoading: Boolean = false,

    val recentQueries: List<String> = emptyList(),
    val places: List<PlaceItem> = emptyList(),
    val hotReports: List<HotReportItem> = emptyList(),

    val mode: SearchMode = SearchMode.RecentEmpty,

    // ✅ 에러 분리
    val searchError: String? = null,
    val hotError: String? = null,

    val isSearchCompleted: Boolean = false
)
