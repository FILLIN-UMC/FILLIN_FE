package com.example.fillin.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.SharedReportData
import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReportType
import com.example.fillin.domain.model.ReportStatus
import com.example.fillin.ui.map.MapContent
import com.example.fillin.ui.map.PresentLocation
import com.example.fillin.ui.theme.FILLINTheme
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.geometry.LatLng
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlin.math.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import java.util.Calendar

@Composable
private fun SetStatusBarColor(color: Color, darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = color.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = darkIcons
        }
    }
}

@Composable
fun HomeScreen(navController: NavController? = null) {
    // 상태바를 밝은 배경에 어두운 아이콘으로 설정
    SetStatusBarColor(color = Color.White, darkIcons = true)
    val context = LocalContext.current
    val presentLocation = remember { PresentLocation(context) }
    var naverMap: NaverMap? by remember { mutableStateOf(null) }
    
    // 카테고리 선택 상태 (다중 선택 가능)
    var selectedCategories by remember { 
        mutableStateOf(setOf<ReportType>()) 
    }
    
    // 알림 배너 표시 여부
    var showNotificationBanner by remember { mutableStateOf(true) }
    
    // 날짜를 밀리초로 변환하는 헬퍼 함수
    fun dateToMillis(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    // 임의 제보 데이터 10개 (홍대입구역 3개, 합정역 3개, 신촌역 4개)
    val sampleReports = remember {
        listOf(
            // 홍대입구역 근처 - 1
            ReportWithLocation(
                report = Report(
                    id = 1,
                    title = "서울시 마포구 양화로 188 홍대입구역 1번 출구 앞",
                    meta = "사고 발생",
                    type = ReportType.DANGER,
                    viewCount = 15,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img,
                    createdAtMillis = dateToMillis(2026, 1, 17)
                ),
                latitude = 37.556300,
                longitude = 126.923200
            ),
            // 홍대입구역 근처 - 2
            ReportWithLocation(
                report = Report(
                    id = 2,
                    title = "서울시 마포구 홍익로 3길 15-2",
                    meta = "맨홀 뚜껑 파손",
                    type = ReportType.INCONVENIENCE,
                    viewCount = 8,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_2,
                    createdAtMillis = dateToMillis(2026, 1, 19)
                ),
                latitude = 37.557000,
                longitude = 126.924000
            ),
            // 홍대입구역 근처 - 3
            ReportWithLocation(
                report = Report(
                    id = 3,
                    title = "서울시 마포구 양화로 188 홍대입구역 9번 출구 앞",
                    meta = "고양이 발견",
                    type = ReportType.DISCOVERY,
                    viewCount = 23,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 18)
                ),
                latitude = 37.555500,
                longitude = 126.922500
            ),
            // 합정역 근처 - 1
            ReportWithLocation(
                report = Report(
                    id = 4,
                    title = "서울시 마포구 양화로 160 합정역 1번 출구 앞",
                    meta = "낙하물 위험",
                    type = ReportType.DANGER,
                    viewCount = 12,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img,
                    createdAtMillis = dateToMillis(2026, 1, 20)
                ),
                latitude = 37.550500,
                longitude = 126.914500
            ),
            // 합정역 근처 - 2
            ReportWithLocation(
                report = Report(
                    id = 5,
                    title = "서울시 마포구 양화로 140 합정역 2번 출구 앞",
                    meta = "도로 파손",
                    type = ReportType.INCONVENIENCE,
                    viewCount = 6,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_2,
                    createdAtMillis = dateToMillis(2026, 1, 16)
                ),
                latitude = 37.549500,
                longitude = 126.913500
            ),
            // 합정역 근처 - 3
            ReportWithLocation(
                report = Report(
                    id = 6,
                    title = "서울시 마포구 양화로 150 합정역 3번 출구 앞",
                    meta = "예쁜 꽃 발견",
                    type = ReportType.DISCOVERY,
                    viewCount = 18,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 19)
                ),
                latitude = 37.551000,
                longitude = 126.915000
            ),
            // 신촌역 근처 - 1
            ReportWithLocation(
                report = Report(
                    id = 7,
                    title = "서울시 서대문구 연세로 50 신촌역 1번 출구 앞",
                    meta = "전기 누전 위험",
                    type = ReportType.DANGER,
                    viewCount = 9,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img,
                    createdAtMillis = dateToMillis(2026, 1, 20)
                ),
                latitude = 37.555100,
                longitude = 126.936800
            ),
            // 신촌역 근처 - 2
            ReportWithLocation(
                report = Report(
                    id = 8,
                    title = "서울시 서대문구 연세로 60 신촌역 2번 출구 앞",
                    meta = "인도 파손",
                    type = ReportType.INCONVENIENCE,
                    viewCount = 11,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_2,
                    createdAtMillis = dateToMillis(2026, 1, 15)
                ),
                latitude = 37.556100,
                longitude = 126.937800
            ),
            // 신촌역 근처 - 3
            ReportWithLocation(
                report = Report(
                    id = 9,
                    title = "서울시 서대문구 연세로 40 신촌역 3번 출구 앞",
                    meta = "새로운 벤치 설치",
                    type = ReportType.DISCOVERY,
                    viewCount = 7,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 20)
                ),
                latitude = 37.554100,
                longitude = 126.935800
            ),
            // 신촌역 근처 - 4
            ReportWithLocation(
                report = Report(
                    id = 10,
                    title = "서울시 서대문구 연세로 55 신촌역 4번 출구 앞",
                    meta = "예쁜 벽화 발견",
                    type = ReportType.DISCOVERY,
                    viewCount = 20,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 14)
                ),
                latitude = 37.555600,
                longitude = 126.937000
            )
        )
    }
    
    // 제보 데이터를 공유 객체에 저장
    LaunchedEffect(sampleReports) {
        SharedReportData.setReports(sampleReports)
    }
    
    // 현재 시간 기준 최근 3일 제보 필터링 및 정렬
    val filteredAndSortedReports = remember(sampleReports) {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L) // 3일 전
        
        sampleReports
            .filter { reportWithLocation: ReportWithLocation ->
                // 최근 3일 필터링
                reportWithLocation.report.createdAtMillis >= threeDaysAgo
            }
            .sortedWith(
                compareBy<ReportWithLocation> { reportWithLocation: ReportWithLocation ->
                    // 타입 순서: 위험 > 불편 > 발견
                    when (reportWithLocation.report.type) {
                        ReportType.DANGER -> 0
                        ReportType.INCONVENIENCE -> 1
                        ReportType.DISCOVERY -> 2
                    }
                }.thenByDescending { reportWithLocation: ReportWithLocation ->
                    // 최신순 정렬
                    reportWithLocation.report.createdAtMillis
                }
            )
    }
    
    // 현재 표시할 알림 인덱스
    var currentNotificationIndex by remember { mutableStateOf(0) }
    
    // 2초마다 알림 변경
    LaunchedEffect(filteredAndSortedReports.size) {
        if (filteredAndSortedReports.isNotEmpty()) {
            while (true) {
                delay(2000) // 2초 대기
                currentNotificationIndex = (currentNotificationIndex + 1) % filteredAndSortedReports.size
            }
        }
    }
    
    // 현재 표시할 제보
    val currentReport = remember(currentNotificationIndex, filteredAndSortedReports) {
        if (filteredAndSortedReports.isNotEmpty()) {
            filteredAndSortedReports[currentNotificationIndex]
        } else null
    }
    
    // 커스텀 위치 핀 아이콘 생성 함수 (파란색 원, 흰색 원형 테두리, 위쪽 화살표)
    fun createLocationPinIcon(sizeDp: Int = 48): OverlayImage {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = sizePx / 2f
        
        // 1. 파란색 원형 배경 그리기 (그라데이션 효과 - 중앙이 밝고 가장자리가 어두움)
        val lightBlue = android.graphics.Color.parseColor("#81D4FA") // 밝은 파란색 (중앙)
        val darkBlue = android.graphics.Color.parseColor("#4FC3F7") // 어두운 파란색 (가장자리)
        
        val gradient = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(lightBlue, darkBlue),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            shader = gradient
            style = Paint.Style.FILL
        }
        val backgroundRect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawOval(backgroundRect, backgroundPaint)
        
        // 2. 중앙 흰색 원형 테두리 그리기 (채워지지 않은 원)
        val circleRadius = (sizePx * 0.22f) // 전체 크기의 22%
        val circlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = (2.5f * density)
        }
        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)
        
        // 3. 위쪽 화살표(삼각형) 그리기 (원형 테두리 위쪽에 위치)
        val arrowSize = (sizePx * 0.12f) // 전체 크기의 12%
        val arrowTopY = centerY - circleRadius - (arrowSize * 0.3f) // 원형 테두리 위쪽에 배치
        val arrowBottomY = arrowTopY + arrowSize
        
        val arrowPath = Path().apply {
            moveTo(centerX, arrowTopY) // 위쪽 꼭짓점
            lineTo(centerX - arrowSize * 0.5f, arrowBottomY) // 왼쪽 아래
            lineTo(centerX + arrowSize * 0.5f, arrowBottomY) // 오른쪽 아래
            close()
        }
        
        val arrowPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(arrowPath, arrowPaint)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    // 이미지를 원형으로 크롭하고 리사이즈하는 함수 (원 배경 위에 제보 이미지)
    fun createCircularMarkerIcon(resId: Int, sizeDp: Int = 42, backgroundColor: Int = android.graphics.Color.WHITE): OverlayImage {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, resId)
        val density = context.resources.displayMetrics.density
        
        // 1. 중앙 부분을 1:1로 크롭
        val size = minOf(originalBitmap.width, originalBitmap.height)
        val x = (originalBitmap.width - size) / 2
        val y = (originalBitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
        
        // 2. 지정된 크기의 원 배경 생성
        val backgroundSizeDp = sizeDp.toFloat()
        val backgroundSizePx = (backgroundSizeDp * density).toInt()
        val markerBitmap = Bitmap.createBitmap(backgroundSizePx, backgroundSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(markerBitmap)
        
        // 3. 원 배경 그리기 (선택된 카테고리에 따라 색상 변경)
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
            style = Paint.Style.FILL
        }
        val backgroundRect = RectF(0f, 0f, backgroundSizePx.toFloat(), backgroundSizePx.toFloat())
        canvas.drawOval(backgroundRect, backgroundPaint)
        
        // 4. 제보 이미지를 원형으로 크롭하여 그 위에 그리기 (약간 작게 해서 여백 생성)
        val imageSizeDp = (sizeDp - 4).toFloat() // 배경보다 4dp 작게 (2dp씩 여백)
        val imageSizePx = (imageSizeDp * density).toInt()
        val imageOffset = (backgroundSizePx - imageSizePx) / 2
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, imageSizePx, imageSizePx, true)
        
        // 5. 원형 마스크 비트맵 생성
        val circularImageBitmap = Bitmap.createBitmap(imageSizePx, imageSizePx, Bitmap.Config.ARGB_8888)
        val imageCanvas = Canvas(circularImageBitmap)
        
        // 6. 원형 마스크 그리기
        val maskPaint = Paint().apply {
            isAntiAlias = true
        }
        val imageRect = RectF(0f, 0f, imageSizePx.toFloat(), imageSizePx.toFloat())
        imageCanvas.drawOval(imageRect, maskPaint)
        
        // 7. 원본 이미지를 원형 마스크 안에 그리기
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        imageCanvas.drawBitmap(resizedBitmap, null, imageRect, maskPaint)
        
        // 8. 원형으로 크롭된 이미지를 배경 위에 그리기
        canvas.drawBitmap(circularImageBitmap, imageOffset.toFloat(), imageOffset.toFloat(), null)
        
        return OverlayImage.fromBitmap(markerBitmap)
    }
    
    // 마커를 상태로 관리
    val markers = remember { mutableListOf<Marker>() }
    
    // 카메라 줌 레벨 상태
    var cameraZoomLevel by remember { mutableStateOf(16.0) }
    
    // 클러스터 마커 생성 함수
    fun createClusterMarkerIcon(
        count: Int,
        dangerCount: Int,
        inconvenienceCount: Int,
        discoveryCount: Int,
        sizeDp: Int = 48
    ): OverlayImage {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val borderWidthPx = (4 * density).toInt()
        
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 1. 흰색 원 배경 그리기
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawOval(rect, backgroundPaint)
        
        // 2. 테두리 색상 분할 그리기 (카테고리 종류에 따라 균등 분할)
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = (sizePx - borderWidthPx) / 2f
        
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = borderWidthPx.toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        
        // 존재하는 카테고리 종류 확인 (순서: 위험(빨강) -> 불편(노랑) -> 발견(초록))
        val categories = mutableListOf<Int>() // 색상 리스트
        if (dangerCount > 0) {
            categories.add(android.graphics.Color.parseColor("#FF6060")) // 위험 (빨강)
        }
        if (inconvenienceCount > 0) {
            categories.add(android.graphics.Color.parseColor("#F5C72F")) // 불편 (노랑)
        }
        if (discoveryCount > 0) {
            categories.add(android.graphics.Color.parseColor("#29C488")) // 발견 (초록)
        }
        
        // 카테고리 종류에 따라 균등 분할
        val categoryCount = categories.size
        if (categoryCount > 0) {
            val sweepAngle = 360f / categoryCount
            var startAngle = -90f // 위쪽부터 시작
            
            categories.forEach { color ->
                borderPaint.color = color
                canvas.drawArc(
                    RectF(
                        centerX - radius,
                        centerY - radius,
                        centerX + radius,
                        centerY + radius
                    ),
                    startAngle,
                    sweepAngle,
                    false,
                    borderPaint
                )
                startAngle += sweepAngle
            }
        }
        
        // 3. 중앙에 숫자 그리기
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#252526")
            textSize = (16 * density)
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(count.toString(), centerX, textY, textPaint)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    // 두 좌표 간 거리 계산 (미터 단위)
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    // 지도 마커 표시 (줌 레벨에 따라 개별 마커 또는 클러스터 마커)
    LaunchedEffect(naverMap, sampleReports, selectedCategories, cameraZoomLevel) {
        naverMap?.let { naverMapInstance ->
            // 기존 마커 제거
            markers.forEach { it.map = null }
            markers.clear()
            
            // 줌 레벨이 14 이하이면 클러스터링
            if (cameraZoomLevel <= 14.0) {
                // 클러스터링 로직 (카테고리 구분 없이 거리만으로 묶기)
                val clusters = mutableListOf<Cluster>()
                val processed = BooleanArray(sampleReports.size) { false }
                
                sampleReports.forEachIndexed { index, reportWithLocation ->
                    if (processed[index]) return@forEachIndexed
                    
                    val cluster = Cluster(
                        centerLat = reportWithLocation.latitude,
                        centerLon = reportWithLocation.longitude,
                        reports = mutableListOf(reportWithLocation)
                    )
                    processed[index] = true
                    
                    // 가까운 제보들을 클러스터 중심점 기준으로 묶기 (100미터 이내, 카테고리 구분 없음)
                    var changed = true
                    while (changed) {
                        changed = false
                        sampleReports.forEachIndexed { otherIndex, otherReport ->
                            if (!processed[otherIndex]) {
                                val distance = calculateDistanceMeters(
                                    cluster.centerLat,
                                    cluster.centerLon,
                                    otherReport.latitude,
                                    otherReport.longitude
                                )
                                if (distance < 300) {
                                    cluster.reports.add(otherReport)
                                    processed[otherIndex] = true
                                    changed = true
                                    
                                    // 클러스터 중심점 업데이트
                                    cluster.centerLat = cluster.reports.map { reportWithLocation: ReportWithLocation -> reportWithLocation.latitude }.average()
                                    cluster.centerLon = cluster.reports.map { reportWithLocation: ReportWithLocation -> reportWithLocation.longitude }.average()
                                }
                            }
                        }
                    }
                    
                    clusters.add(cluster)
                }
                
                // 클러스터 마커 생성
                clusters.forEach { cluster ->
                    val dangerCount = cluster.reports.count { it.report.type == ReportType.DANGER }
                    val inconvenienceCount = cluster.reports.count { it.report.type == ReportType.INCONVENIENCE }
                    val discoveryCount = cluster.reports.count { it.report.type == ReportType.DISCOVERY }
                    
                    val marker = Marker().apply {
                        position = LatLng(cluster.centerLat, cluster.centerLon)
                        map = naverMapInstance
                        icon = createClusterMarkerIcon(
                            cluster.reports.size,
                            dangerCount,
                            inconvenienceCount,
                            discoveryCount
                        )
                    }
                    markers.add(marker)
                }
            } else {
                // 개별 마커 표시
                sampleReports.forEach { reportWithLocation ->
                    // 선택된 카테고리에 해당하는 마커는 2배 크기(80dp), 아니면 기본 크기(40dp)
                    val isSelected = selectedCategories.contains(reportWithLocation.report.type)
                    val markerSize = if (isSelected) 80 else 40
                    
                    // 선택된 카테고리에 따라 배경 색상 결정
                    val backgroundColor = if (isSelected) {
                        when (reportWithLocation.report.type) {
                            ReportType.DANGER -> android.graphics.Color.parseColor("#FF6060") // 위험 (빨강)
                            ReportType.INCONVENIENCE -> android.graphics.Color.parseColor("#F5C72F") // 불편 (노랑)
                            ReportType.DISCOVERY -> android.graphics.Color.parseColor("#29C488") // 발견 (초록)
                        }
                    } else {
                        android.graphics.Color.WHITE // 선택되지 않으면 흰색
                    }
                    
                    val marker = Marker().apply {
                        position = LatLng(reportWithLocation.latitude, reportWithLocation.longitude)
                        map = naverMapInstance
                        // 카테고리에 따라 다른 이미지 아이콘 설정 (원형 크롭)
                        icon = when (reportWithLocation.report.type) {
                            ReportType.DANGER -> {
                                createCircularMarkerIcon(R.drawable.ic_report_img, markerSize, backgroundColor)
                            }
                            ReportType.INCONVENIENCE -> {
                                createCircularMarkerIcon(R.drawable.ic_report_img_2, markerSize, backgroundColor)
                            }
                            ReportType.DISCOVERY -> {
                                createCircularMarkerIcon(R.drawable.ic_report_img_3, markerSize, backgroundColor)
                            }
                        }
                    }
                    markers.add(marker)
                }
            }
        }
    }
    
    // 권한 요청
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            naverMap?.let { map ->
                val locationPinIcon = createLocationPinIcon(48)
                presentLocation.setupLocationOverlay(map, locationPinIcon)
                presentLocation.moveMapToCurrentLocation(map)
                presentLocation.startLocationUpdates(map, locationPinIcon)
            }
        }
    }
    
    // 앱 시작 시 사용자 위치를 중심에 표시 및 실시간 위치 업데이트 시작
    LaunchedEffect(naverMap) {
        naverMap?.let { map ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationPinIcon = createLocationPinIcon(48)
                presentLocation.setupLocationOverlay(map, locationPinIcon)
                presentLocation.moveMapToCurrentLocation(map)
                presentLocation.startLocationUpdates(map, locationPinIcon)
            }
        }
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // 네비게이션 바 높이 계산 (BottomNavBar와 동일한 로직)
        val scale = maxWidth / 380.dp
        val expandedHeight = 162.dp * scale     // 네비게이션 바 높이
        val bottomPadding = 40.dp                // 네비게이션 바 하단 패딩
        val navBarTotalHeight = expandedHeight + bottomPadding
        
        // 지도
        MapContent(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                naverMap = map
                // 초기 줌 레벨 설정
                cameraZoomLevel = map.cameraPosition.zoom
                // 카메라 변경 리스너 추가
                map.addOnCameraIdleListener {
                    cameraZoomLevel = map.cameraPosition.zoom
                }
                
                // 커스텀 위치 핀 아이콘 설정 및 실시간 위치 업데이트 시작
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val locationPinIcon = createLocationPinIcon(48)
                    presentLocation.setupLocationOverlay(map, locationPinIcon)
                    presentLocation.startLocationUpdates(map, locationPinIcon)
                }
            }
        )
        
        // 상단 알림 배너
        if (showNotificationBanner && currentReport != null) {
            NotificationBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .aspectRatio(348f / 48f),
                report = currentReport.report,
                onDismiss = { showNotificationBanner = false }
            )
        }
        
        // 내 위치 버튼 (카테고리 필터와 동일한 높이, 네비게이션바 끝 위치)
        LocationButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = navBarTotalHeight + 20.dp) // 카테고리 필터와 동일한 높이
                .padding(end = 16.dp),
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    naverMap?.let { map ->
                        presentLocation.moveMapToCurrentLocation(map)
                    }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )
        
        // 카테고리 필터 (네비게이션 바로부터 20dp 위, 좌측 정렬)
        CategoryFilterRow(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = navBarTotalHeight + 20.dp) // 네비게이션 바 높이 + 20dp
                .padding(start = 16.dp), // 네비게이션 바와 동일한 좌측 패딩
            selectedCategories = selectedCategories,
            onCategoryToggle = { category ->
                selectedCategories = if (selectedCategories.contains(category)) {
                    selectedCategories - category
                } else {
                    selectedCategories + category
                }
            }
        )
        
    }
}

@Composable
private fun NotificationBanner(
    modifier: Modifier = Modifier,
    report: Report,
    onDismiss: () -> Unit
) {
    // 카테고리별 색상
    val categoryColor = when (report.type) {
        ReportType.DANGER -> Color(0xFFFF6060) // 위험 제보
        ReportType.INCONVENIENCE -> Color(0xFFF5C72F) // 불편 제보
        ReportType.DISCOVERY -> Color(0xFF29C488) // 발견 제보
    }
    
    // 주소에서 시/도/구 제거 (시/도/구 단위보다 작은 단위만 표시)
    val addressWithoutCityDistrict = remember(report.title) {
        // 정규식으로 "서울시 마포구", "서울특별시 마포구", "경기도 성남시" 같은 패턴 제거
        report.title.replace(Regex("^[가-힣]+(?:시|도)\\s+[가-힣]+(?:구|시)\\s*"), "")
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp), // pill 모양
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 카테고리 컬러 점
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )
                
                Spacer(Modifier.width(4.dp))
                
                // 주소와 meta를 분리하여 표시
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 주소 부분 (ellipsis 적용)
                    Text(
                        text = addressWithoutCityDistrict,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF555659), // 회색 텍스트
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // meta 부분 (항상 표시)
                    Text(
                        text = " ${report.meta}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF555659), // 회색 텍스트
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Color(0xFFE7EBF2), RoundedCornerShape(999.dp))
            .background(Color(0xFFF7FBFF))
            .clickable(onClick = onSearchClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFFAAADB3),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "내주변 제보 검색",
                color = Color(0xFFAAADB3),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    modifier: Modifier = Modifier,
    selectedCategories: Set<ReportType>,
    onCategoryToggle: (ReportType) -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 위험 (빨간색)
        CategoryFilterButton(
            iconRes = if (selectedCategories.contains(ReportType.DANGER)) {
                R.drawable.ic_warning_selected
            } else {
                R.drawable.ic_warning
            },
            label = "위험",
            isSelected = selectedCategories.contains(ReportType.DANGER),
            onClick = { onCategoryToggle(ReportType.DANGER) },
            backgroundColor = Color(0xFFFFFFFF),
            selectedBackgroundColor = Color(0xFFFF6B6B),
            iconTint = if (selectedCategories.contains(ReportType.DANGER)) Color.White else Color(0xFFFF6B6B),
            textColor = if (selectedCategories.contains(ReportType.DANGER)) Color.White else Color(0xFFFF6B6B)
        )
        
        Spacer(Modifier.width(8.dp))
        
        // 불편 (노란색)
        CategoryFilterButton(
            iconRes = if (selectedCategories.contains(ReportType.INCONVENIENCE)) {
                R.drawable.ic_inconvenience_selected
            } else {
                R.drawable.ic_inconvenience
            },
            label = "불편",
            isSelected = selectedCategories.contains(ReportType.INCONVENIENCE),
            onClick = { onCategoryToggle(ReportType.INCONVENIENCE) },
            backgroundColor = Color(0xFFFFFFFF),
            selectedBackgroundColor = Color(0xFFFFC107),
            iconTint = if (selectedCategories.contains(ReportType.INCONVENIENCE)) Color.White else Color(0xFFFFC107),
            textColor = if (selectedCategories.contains(ReportType.INCONVENIENCE)) Color.White else Color(0xFFFFC107)
        )
        
        Spacer(Modifier.width(8.dp))
        
        // 발견 (초록색)
        CategoryFilterButton(
            iconRes = R.drawable.ic_discovery,
            label = "발견",
            isSelected = selectedCategories.contains(ReportType.DISCOVERY),
            onClick = { onCategoryToggle(ReportType.DISCOVERY) },
            backgroundColor = Color(0xFFFFFFFF),
            selectedBackgroundColor = Color(0xFF4CAF50),
            iconTint = if (selectedCategories.contains(ReportType.DISCOVERY)) Color.White else Color(0xFF4CAF50),
            textColor = if (selectedCategories.contains(ReportType.DISCOVERY)) Color.White else Color(0xFF4CAF50)
        )
        }
    }
}

@Composable
private fun CategoryFilterButton(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color,
    selectedBackgroundColor: Color,
    iconTint: Color,
    textColor: Color
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) selectedBackgroundColor else backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun LocationButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "내 위치",
            tint = Color(0xFF4090E0),
            modifier = Modifier.size(24.dp)
        )
    }
}

// 제보와 위치 정보를 함께 저장하는 데이터 클래스
data class ReportWithLocation(
    val report: Report,
    val latitude: Double,
    val longitude: Double
)

// 클러스터 데이터 클래스
data class Cluster(
    var centerLat: Double,
    var centerLon: Double,
    val reports: MutableList<ReportWithLocation>
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    FILLINTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // 지도 대신 배경색으로 대체 (Preview에서는 실제 지도가 표시되지 않음)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5E7EB))
            )
            
            // 알림 배너
            NotificationBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .aspectRatio(348f / 48f),
                report = Report(
                    id = 1,
                    title = "서울시 마포구 양화로 188 홍대입구역 1번 출구 앞",
                    meta = "사고 발생",
                    type = ReportType.DANGER,
                    viewCount = 15,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img
                ),
                onDismiss = { }
            )
            
            // 하단 컨트롤 섹션
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // 검색 바
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onSearchClick = { }
                )
                
                // 카테고리 필터 (위험, 불편, 발견)
                CategoryFilterRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedCategories = emptySet(),
                    onCategoryToggle = { }
                )
                
                // 내 위치 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    LocationButton(
                        onClick = { }
                    )
                }
            }
        }
    }
}
