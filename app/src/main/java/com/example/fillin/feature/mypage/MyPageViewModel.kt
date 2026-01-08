package com.example.fillin.feature.mypage

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MyPageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MyPageUiState>(MyPageUiState.Loading)
    val uiState: StateFlow<MyPageUiState> = _uiState

    init {
        load()
    }

    fun load() {
        // TODO: 나중에 repository + suspend로 바꾸기
        val summary = MyPageSummary(
            nickname = "방태림",
            totalReports = 5,
            totalViews = 50,
            danger = 1 to 5,
            inconvenience = 0 to 5,
            discoveryCompleted = true
        )

        val reports = listOf(
            MyReportCard(1, "행복길 122-11", "가는길 255m"),
            MyReportCard(2, "행복길 122-11", "가는길 255m"),
            MyReportCard(3, "행복길 122-11", "가는길 255m"),
        )

        _uiState.value = MyPageUiState.Success(summary, reports)
    }
}
