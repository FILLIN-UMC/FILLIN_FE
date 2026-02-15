package com.example.fillin.feature.mypage

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.ReportStatusManager
import com.example.fillin.data.SharedReportData
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.location.LocationProvider
import com.example.fillin.data.repository.MypageRepository
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.example.fillin.domain.model.ReportStatus
import com.example.fillin.domain.model.ReportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val appPreferences: AppPreferences,
    private val mypageRepository: MypageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyPageUiState>(MyPageUiState.Loading)
    val uiState: StateFlow<MyPageUiState> = _uiState

    init {
        load(null)
        appPreferences.nicknameFlow
            .onEach { newNickname ->
                updateNickname(newNickname)
            }
            .launchIn(viewModelScope)
    }

    fun load(context: Context? = null) {
        viewModelScope.launch {
            val ctx = context
            val hasToken = ctx != null && TokenManager.isLoggedIn(ctx)

            if (hasToken) {
                loadFromApi(ctx!!)
            } else {
                loadFromLocal(ctx)
            }
        }
    }

    private suspend fun loadFromApi(context: Context) {
        _uiState.value = MyPageUiState.Loading

        val profileResult = mypageRepository.getProfile()
        val countResult = mypageRepository.getReportCount()
        val categoryResult = mypageRepository.getReportCategory()
        val rankResult = mypageRepository.getRanks()
        val missionsResult = mypageRepository.getMissions()
        val reportsResult = mypageRepository.getMyReports()
        val likedReportsResult = mypageRepository.getLikedReports()
        val expireSoonResult = mypageRepository.getReportExpireSoon()

        if (profileResult.isFailure || countResult.isFailure || reportsResult.isFailure) {
            Log.w("MyPageViewModel", "API failed, fallback to local", profileResult.exceptionOrNull() ?: countResult.exceptionOrNull() ?: reportsResult.exceptionOrNull())
            loadFromLocal(context)
            return
        }

        val profile = profileResult.getOrNull()?.data
        val count = countResult.getOrNull()?.data
        val category = categoryResult.getOrNull()?.data
        val reports = reportsResult.getOrNull()?.data ?: emptyList()
        val likedReports = likedReportsResult.getOrNull()?.data ?: emptyList()
        val expireSoon = expireSoonResult.getOrNull()?.data

        if (profile != null) {
            appPreferences.setNickname(profile.nickname ?: appPreferences.getNickname())
            profile.profileImageUrl?.let { url ->
                appPreferences.setProfileImageUri(url)
            }
        }

        val totalReports = count?.totalReportCount ?: 0
        val totalViews = count?.totalViewCount ?: 0
        val rankData = rankResult.getOrNull()?.data
        val missions = missionsResult.getOrNull()?.data ?: emptyList()
        val dangerMission = missions.find { it.category == "DANGER" }
        val inconvenMission = missions.find { it.category == "INCONVENIENCE" }
        val discoveryMission = missions.find { it.category == "DISCOVERY" }
        // 내가 한 제보: 카테고리별 제보 개수는 getReportCategory() API 우선, 실패 시 getMyReports() 결과로 계산
        val dangerCount = category?.dangerCount ?: reports.count { it.reportCategory == "DANGER" }
        val inconvenienceCount = category?.inconvenienceCount ?: reports.count { it.reportCategory == "INCONVENIENCE" }
        val discoveryCount = category?.discoveryCount ?: reports.count { it.reportCategory == "DISCOVERY" }
        val danger = dangerCount to (dangerMission?.targetCount ?: 5)
        val inconvenience = inconvenienceCount to (inconvenMission?.targetCount ?: 5)
        val discovery = discoveryCount to (discoveryMission?.targetCount ?: 5)

        val summary = MyPageSummary(
            nickname = profile?.nickname ?: appPreferences.getNickname(),
            totalReports = totalReports,
            totalViews = totalViews,
            achievement = rankData?.achievement,
            danger = danger,
            inconvenience = inconvenience,
            discovery = discovery
        )

        val userLatLng = LocationProvider(context).getLatLng()
        val reportCards = reports.map { item ->
            val address = cleanAddress(item.address ?: "")
            val distance = formatDistance(userLatLng, item.latitude, item.longitude)
            MyReportCard(
                id = item.reportId ?: 0L,
                address = address.ifEmpty { item.title ?: "" },
                distance = distance,
                reportTitle = item.title ?: "",
                imageResId = null,
                imageUrl = item.reportImageUrl,
                viewCount = item.viewCount
            )
        }

        val likedReportCards = likedReports.map { item ->
            val address = cleanAddress(item.address ?: "")
            val distance = formatDistance(userLatLng, item.latitude, item.longitude)
            MyReportCard(
                id = item.reportId ?: 0L,
                address = address.ifEmpty { item.title ?: "" },
                distance = distance,
                reportTitle = item.title ?: "",
                imageResId = null,
                imageUrl = item.reportImageUrl,
                viewCount = item.viewCount
            )
        }

        val expiringNoticeList = buildExpiringNoticeList(expireSoon)

        _uiState.value = MyPageUiState.Success(summary, reportCards, likedReportCards, expiringNoticeList)
    }

    private fun cleanAddress(address: String): String {
        return address
            .replace(Regex("^[가-힣]+(?:시|도)\\s+[가-힣]+(?:구|시)\\s*"), "")
            .replace(Regex("\\s*[가-힣]*역\\s*\\d+번\\s*출구\\s*앞"), "")
            .trim()
    }

    private fun formatDistance(userLatLng: Pair<Double, Double>?, lat: Double?, lon: Double?): String {
        if (userLatLng == null || lat == null || lon == null) return ""
        val meters = calculateDistanceMeters(userLatLng.first, userLatLng.second, lat, lon)
        return "가는길 ${meters.toInt()}m"
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun buildExpiringNoticeList(expireSoon: com.example.fillin.data.model.mypage.ReportExpireSoonData?): List<ExpiringReportNotice> {
        val listDtos = expireSoon?.listDtos ?: return emptyList()
        if (listDtos.isEmpty()) return emptyList()

        val threeDaysMillis = 3 * 24 * 60 * 60 * 1000L
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        val groupedByDaysLeft = listDtos.groupBy { item ->
            val expireTimeStr = item.expireTime ?: return@groupBy 0
            val expireMillis = try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .parse(expireTimeStr)?.time
                    ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
                        .parse(expireTimeStr)?.time
                    ?: (now + oneDayMillis)
            } catch (_: Exception) {
                now + oneDayMillis
            }
            ((expireMillis + threeDaysMillis - now) / oneDayMillis).toInt().coerceAtLeast(0)
        }

        return groupedByDaysLeft.keys.sortedDescending().map { daysLeft ->
            val groupItems = groupedByDaysLeft[daysLeft] ?: emptyList()
            val d = groupItems.count { it.reportCategory == "DANGER" }
            val i = groupItems.count { it.reportCategory == "INCONVENIENCE" }
            val s = groupItems.count { it.reportCategory == "DISCOVERY" }
            val parts = buildList {
                if (d > 0) add("위험 $d")
                if (i > 0) add("불편 $i")
                if (s > 0) add("발견 $s")
            }
            val reportImages = groupItems.take(3).map { item ->
                ExpiringReportImage(
                    imageUrl = item.reportImageUrl,
                    imageResId = null
                )
            }
            ExpiringReportNotice(
                daysLeft = daysLeft,
                summaryText = parts.joinToString(", "),
                reportImages = reportImages
            )
        }
    }

    private fun loadFromLocal(context: Context?) {
        val userReports = SharedReportData.getUserReports(context)
        val updatedUserReports = userReports.map { rwl ->
            rwl.copy(report = ReportStatusManager.updateReportStatus(rwl.report))
        }

        val totalReports = updatedUserReports.size
        val totalViews = updatedUserReports.sumOf { it.report.viewCount }
        val dangerCount = updatedUserReports.count { it.report.type == ReportType.DANGER }
        val inconvenienceCount = updatedUserReports.count { it.report.type == ReportType.INCONVENIENCE }
        val discoveryCount = updatedUserReports.count { it.report.type == ReportType.DISCOVERY }

        val summary = MyPageSummary(
            nickname = appPreferences.getNickname(),
            totalReports = totalReports,
            totalViews = totalViews,
            achievement = null,
            danger = dangerCount to 5,
            inconvenience = inconvenienceCount to 5,
            discovery = discoveryCount to 5
        )

        val userLatLng = context?.let { LocationProvider(it).getLatLng() }
        val reports = updatedUserReports.map { reportWithLocation ->
            val r = reportWithLocation.report
            val address = cleanAddress(r.title)
            val distance = formatDistance(userLatLng, reportWithLocation.latitude, reportWithLocation.longitude)
            MyReportCard(
                id = r.id,
                address = address,
                distance = distance,
                reportTitle = r.meta, // 로컬 Report: meta가 제보 제목
                imageResId = r.imageResId,
                imageUrl = r.imageUrl,
                viewCount = r.viewCount
            )
        }

        val expiringReports = updatedUserReports.filter { it.report.status == ReportStatus.EXPIRING }
        val threeDaysMillis = 3 * 24 * 60 * 60 * 1000L
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val expiringNoticeList = if (expiringReports.isEmpty()) emptyList() else {
            val groupedByDaysLeft = expiringReports.groupBy { rwl ->
                val expiringAt = rwl.report.expiringAtMillis ?: now
                ((expiringAt + threeDaysMillis - now) / oneDayMillis).toInt().coerceAtLeast(0)
            }
            groupedByDaysLeft.keys.sortedDescending().map { daysLeft ->
                val groupReports = (groupedByDaysLeft[daysLeft] ?: emptyList())
                    .sortedWith(compareBy({ it.report.createdAtMillis }, { it.report.id }))
                val parts = buildList {
                    val d = groupReports.count { it.report.type == ReportType.DANGER }
                    val i = groupReports.count { it.report.type == ReportType.INCONVENIENCE }
                    val s = groupReports.count { it.report.type == ReportType.DISCOVERY }
                    if (d > 0) add("위험 $d")
                    if (i > 0) add("불편 $i")
                    if (s > 0) add("발견 $s")
                }
                val reportImages = groupReports.take(3).map { rwl ->
                    ExpiringReportImage(
                        imageUrl = rwl.report.imageUrl,
                        imageResId = rwl.report.imageResId
                    )
                }
                ExpiringReportNotice(daysLeft = daysLeft, summaryText = parts.joinToString(", "), reportImages = reportImages)
            }
        }

        _uiState.value = MyPageUiState.Success(summary, reports, likedReports = emptyList(), expiringNoticeList)
    }

    private fun updateNickname(newNickname: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is MyPageUiState.Success -> {
                    currentState.copy(
                        summary = currentState.summary.copy(nickname = newNickname)
                    )
                }
                else -> currentState
            }
        }
    }
}
