package com.example.fillin.feature.search

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.local.RecentQueryStore
import com.example.fillin.data.location.LocationProvider
import com.example.fillin.data.repository.HotReportRepository
import com.example.fillin.domain.model.VoteType
import com.example.fillin.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    context: Context,
    private val placeRepo: PlaceRepository,
    private val hotRepo: HotReportRepository
) : ViewModel() {

    private val recentStore = RecentQueryStore(context)
    private val locationProvider = LocationProvider(context)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    init {
        observeRecent()
        switchTab(SearchTab.RECENT)
    }

    private fun observeRecent() {
        viewModelScope.launch {
            recentStore.flow().collect { list ->
                _uiState.update { s ->
                    val mode = if (s.tab == SearchTab.RECENT) {
                        if (list.isEmpty()) SearchMode.RecentEmpty else SearchMode.RecentList
                    } else s.mode
                    s.copy(recentQueries = list, mode = mode)
                }
            }
        }
    }

    fun vote(reportId: String, type: VoteType) {
        _uiState.update { s ->
            val updated = s.hotReports.map { r ->
                if (r.id != reportId) r else {
                    when (type) {
                        VoteType.STILL_DANGER -> r.copy(stillDangerCount = r.stillDangerCount + 1)
                        VoteType.NOW_SAFE -> r.copy(nowSafeCount = r.nowSafeCount + 1)
                    }
                }
            }
            s.copy(hotReports = updated)
        }

        viewModelScope.launch { hotRepo.vote(reportId, type) }
    }

    fun setQuery(q: String) {
        _uiState.update {
            it.copy(
                query = q,
                isSearchCompleted = false
            )
        }
    }

    fun clearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                isSearchCompleted = false
            )
        }
    }

    fun removeRecent(q: String) {
        viewModelScope.launch { recentStore.remove(q) }
    }

    fun switchTab(tab: SearchTab) {
        _uiState.update { s ->
            val mode = when (tab) {
                SearchTab.RECENT ->
                    if (s.recentQueries.isEmpty()) SearchMode.RecentEmpty else SearchMode.RecentList
                SearchTab.HOT -> SearchMode.HotReportList
            }
            // ✅ 탭 전환 시: 검색 에러/핫 에러를 각자만 정리
            s.copy(
                tab = tab,
                mode = mode,
                // 탭 이동할 때 검색 오버레이가 붙어있지 않도록 검색에러는 내려줌
                searchError = null
            )
        }

        if (tab == SearchTab.HOT) loadHotReports()
    }

    /**
     * ✅ 장소 검색
     * - 위치 있으면 반경 자동 확장(2km → 5km → 10km)
     * - 위치 없으면 전국 검색 fallback
     * - 실패는 searchError에만 기록 (HOT UI를 망치지 않도록)
     */
    fun search() {
        val q = _uiState.value.query.trim()
        if (q.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null, isSearchCompleted = false) }

            val latLng = locationProvider.getLatLng()

            val results = runCatching {
                if (latLng != null) {
                    val (lat, lon) = latLng
                    val radiuses = listOf(2000, 5000, 10000)
                    var last = emptyList<com.example.fillin.domain.model.PlaceItem>()

                    for (r in radiuses) {
                        Log.d("SEARCH", "searchPlaces q=$q radius=$r lat=$lat lon=$lon")
                        val res = placeRepo.searchPlaces(
                            query = q,
                            x = lon,
                            y = lat,
                            radius = r
                        )
                        last = res
                        if (res.size >= 8) break
                    }
                    last
                } else {
                    Log.d("SEARCH", "searchPlaces q=$q (no location) -> fallback")
                    placeRepo.searchPlaces(query = q)
                }
            }.getOrElse { e ->
                _uiState.update { s ->
                    s.copy(
                        isSearching = false,
                        searchError = "검색 실패: ${e.message ?: "unknown"}",
                        isSearchCompleted = true
                    )
                }
                return@launch
            }

            // ✅ 최근 검색 저장
            recentStore.push(q)

            _uiState.update { s ->
                s.copy(
                    isSearching = false,
                    places = results,
                    mode = SearchMode.ResultList,
                    searchError = null,
                    isSearchCompleted = true
                )
            }
        }
    }

    /**
     * ✅ HOT 로딩
     * - 실패는 hotError에만 기록
     * - (중요) 여기서 "검색 실패" 오버레이를 띄우면 최종 UI가 깨짐
     * - 위치 정보가 없으면 기본 좌표(서울 시청)로 fallback
     */
    private fun loadHotReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHotLoading = true, hotError = null) }

            // ✅ 위치 정보가 없으면 기본 좌표로 fallback (서울 시청)
            val (lat, lon) = locationProvider.getLatLng() 
                ?: (37.5665 to 126.9780)

            val result = runCatching { hotRepo.getHotReportsNear(lat, lon) }
                .getOrElse { e ->
                    _uiState.update { s ->
                        s.copy(
                            isHotLoading = false,
                            hotReports = emptyList(),
                            hotError = "인기 제보 로드 실패: ${e.message ?: "unknown"}"
                        )
                    }
                    return@launch
                }

            _uiState.update {
                it.copy(
                    isHotLoading = false,
                    hotReports = result.reports,
                    places = result.places, // ✅ HOT에서도 places 채워서 지도 핀 변환 가능
                    mode = SearchMode.HotReportList,
                    hotError = null
                )
            }
        }
    }
}
