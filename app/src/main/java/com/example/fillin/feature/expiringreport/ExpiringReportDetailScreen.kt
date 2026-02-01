
package com.example.fillin.feature.expiringreport

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fillin.R
import com.example.fillin.ui.theme.FILLINTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.fillin.ui.components.ReportCard
import com.example.fillin.ui.components.ReportCardUi
import com.example.fillin.ui.components.ValidityStatus
import com.example.fillin.data.SharedReportData
import com.example.fillin.data.ReportStatusManager
import com.example.fillin.data.db.FirestoreRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Preview(showBackground = true, name = "ExpiringReportDetail")
@Composable
private fun ExpiringReportDetailPreview() {
    FILLINTheme {
        ExpiringReportDetailScreen(navController = rememberNavController())
    }
}

@Composable
fun ExpiringReportDetailScreen(navController: NavController) {
    val backgroundColor = Color(0xFFF7FBFF)
    val context = LocalContext.current
    val firestoreRepository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    
    // 사용자 피드백 선택 상태 추적 (reportId -> "positive" | "negative" | null)
    var userFeedbackSelections by remember(context) { 
        mutableStateOf(SharedReportData.loadUserFeedbackSelections(context))
    }
    
    // 사용자 좋아요 상태 추적 (reportId -> Boolean)
    var userLikeStates by remember(context) { 
        mutableStateOf(SharedReportData.loadUserLikeStates(context))
    }
    
    // 좋아요 토글 함수
    fun toggleLike(reportId: Long) {
        val reportWithLocation = SharedReportData.getReports().find { it.report.id == reportId } ?: return
        val currentLikeState = userLikeStates[reportId] ?: reportWithLocation.report.isSaved
        val newLikeState = !currentLikeState
        userLikeStates = userLikeStates + (reportId to newLikeState)
        SharedReportData.saveUserLikeState(context, reportId, newLikeState)
        reportWithLocation.report.documentId?.let { docId ->
            scope.launch {
                firestoreRepository.updateReportLike(docId, "guest_user", newLikeState)
            }
        }
    }
    
    // 피드백 업데이트 함수 (토글 방식)
    fun updateFeedback(reportId: Long, isPositive: Boolean) {
        val currentSelection = userFeedbackSelections[reportId]
        val newSelection = when {
            // 같은 버튼을 다시 누르면 취소
            (isPositive && currentSelection == "positive") || (!isPositive && currentSelection == "negative") -> null
            // 다른 버튼을 누르면 이전 선택 취소 후 새로운 선택 적용
            else -> if (isPositive) "positive" else "negative"
        }
        
        // 사용자 피드백 선택 상태 업데이트
        userFeedbackSelections = userFeedbackSelections + (reportId to newSelection)
        
        // SharedPreferences에 사용자 선택 상태 저장
        SharedReportData.saveUserFeedbackSelection(context, reportId, newSelection)
        
        // SharedReportData에서 해당 제보 찾기
        val reports = SharedReportData.getReports()
        val now = System.currentTimeMillis()
        val updatedReports = reports.map { reportWithLocation ->
            if (reportWithLocation.report.id == reportId) {
                val currentPositiveCount = reportWithLocation.report.positiveFeedbackCount
                val currentNegativeCount = reportWithLocation.report.negativeFeedbackCount
                val currentNegativeTimestamps = reportWithLocation.report.negativeFeedbackTimestamps

                val adjustedPositiveCount = when (currentSelection) {
                    "positive" -> maxOf(0, currentPositiveCount - 1)
                    else -> currentPositiveCount
                }
                val adjustedNegativeCount = when (currentSelection) {
                    "negative" -> maxOf(0, currentNegativeCount - 1)
                    else -> currentNegativeCount
                }

                val updatedPositiveCount = when (newSelection) {
                    "positive" -> adjustedPositiveCount + 1
                    else -> adjustedPositiveCount
                }
                val updatedNegativeCount = when (newSelection) {
                    "negative" -> adjustedNegativeCount + 1
                    else -> adjustedNegativeCount
                }
                val updatedNegativeTimestamps = when {
                    newSelection == "negative" -> currentNegativeTimestamps + now
                    currentSelection == "negative" && newSelection != "negative" -> currentNegativeTimestamps.dropLast(1)
                    else -> currentNegativeTimestamps
                }

                var updatedReport = reportWithLocation.report.copy(
                    positiveFeedbackCount = updatedPositiveCount,
                    negativeFeedbackCount = updatedNegativeCount,
                    negativeFeedbackTimestamps = updatedNegativeTimestamps
                )
                updatedReport = ReportStatusManager.updateValiditySustainedTimestamps(updatedReport)
                updatedReport = ReportStatusManager.updateReportStatus(updatedReport)
                updatedReport.documentId?.let { docId ->
                    scope.launch {
                        firestoreRepository.updateReportFeedback(
                            docId,
                            updatedPositiveCount,
                            updatedNegativeCount,
                            updatedReport.positive70SustainedSinceMillis,
                            updatedReport.positive40to60SustainedSinceMillis,
                            updatedNegativeTimestamps
                        )
                    }
                } ?: run {
                    SharedReportData.saveFeedbackToPreferences(
                        context,
                        reportId,
                        updatedPositiveCount,
                        updatedNegativeCount,
                        updatedReport.positive70SustainedSinceMillis,
                        updatedReport.positive40to60SustainedSinceMillis,
                        updatedNegativeTimestamps
                    )
                }
                reportWithLocation.copy(report = updatedReport)
            } else {
                reportWithLocation
            }
        }
        SharedReportData.setReports(updatedReports)
    }

    val view = LocalView.current
    val isPreview = LocalInspectionMode.current
    if (!isPreview) {
        DisposableEffect(view, backgroundColor) {
            val window = (view.context as Activity).window

            // 백업
            val prevStatusBarColor = window.statusBarColor
            val prevNavBarColor = window.navigationBarColor

            // 이 화면에서만 상태바 영역까지 배경이 보이도록 edge-to-edge 적용
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = backgroundColor.toArgb()

            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true

            onDispose {
                // 다른 화면에 영향 없도록 원복
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                window.statusBarColor = prevStatusBarColor
                window.navigationBarColor = prevNavBarColor
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { navController.popBackStack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back_btn),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "사라질 제보",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                fontSize = 20.sp,
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // Example data (later replace with backend-connected data)
        val expiringReports = remember {
            listOf(
                ReportCardUi(
                    reportId = 1L, // 임시 ID
                    validityStatus = ValidityStatus.INVALID,
                    imageRes = R.drawable.ic_report_img,
                    views = 5,
                    typeLabel = "위험",
                    typeColor = Color(0xFFFF6060),
                    userName = "조치원 고라니",
                    userBadge = "루키",
                    title = "맨홀 뚜껑 역류",
                    createdLabel = "5일 전",
                    address = "행복길 1239-11",
                    distance = "가는 길 20m",
                    okCount = 6,
                    dangerCount = 2,
                    isLiked = true
                ),
                ReportCardUi(
                    reportId = 2L, // 임시 ID
                    validityStatus = ValidityStatus.INVALID,
                    imageRes = R.drawable.ic_report_img,
                    views = 12,
                    typeLabel = "불편",
                    typeColor = Color(0xFF4595E5),
                    userName = "조치원 고라니",
                    userBadge = "베테랑",
                    title = "인도 블록 파손",
                    createdLabel = "7일 전",
                    address = "행복길 122-11",
                    distance = "가는 길 255m",
                    okCount = 3,
                    dangerCount = 4,
                    isLiked = false
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 24.dp)
        ) {
            // Headline
            item {
                Text(
                    text = buildAnnotatedString {
                        append("총 ")
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF4595E5),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) { append("20명") }
                        append("에게 도움이 되었어요!")
                    },
                    color = Color(0xFF252526),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp)
                )

                Spacer(Modifier.height(12.dp))
            }

            // Cards
            items(expiringReports) { report ->
                ReportCard(
                    report = report,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    selectedFeedback = userFeedbackSelections[report.reportId],
                    isLiked = userLikeStates[report.reportId] ?: report.isLiked,
                    feedbackButtonsEnabled = false, // 사라질 제보 화면에서는 피드백 버튼 비활성
                    onPositiveFeedback = {},
                    onNegativeFeedback = {},
                    onLikeToggle = {
                        toggleLike(report.reportId)
                    }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
        }
    }
}
