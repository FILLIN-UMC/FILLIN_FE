package com.example.fillin.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fillin.BuildConfig
import com.example.fillin.R
import com.example.fillin.data.ai.GeminiRepository
import com.example.fillin.data.ai.GeminiViewModel
import com.example.fillin.data.ai.GeminiViewModelFactory
import com.example.fillin.data.kakao.RetrofitClient
import com.example.fillin.feature.report.locationselect.LocationSelectionScreen
import com.example.fillin.feature.report.pastreport.PastReportLocationScreen
import com.example.fillin.feature.report.pastreport.PastReportPhotoSelectionScreen
import com.example.fillin.feature.report.realtime.RealtimeReportScreen
import com.example.fillin.feature.report.ReportOptionMenu
import com.example.fillin.feature.report.ReportRegistrationScreen
import com.example.fillin.feature.report.LastUploadedSnapshot
import com.example.fillin.feature.report.ReportViewModel
import com.example.fillin.feature.report.ReportViewModelFactory
import com.example.fillin.ui.components.AiLoadingOverlay
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.CameraAnimation
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.ReportStatusManager
import com.example.fillin.data.SampleReportData
import com.example.fillin.data.SharedReportData
import com.example.fillin.data.db.UploadedReportResult
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.model.mypage.MyReportItem
import com.example.fillin.data.model.report.ReportImageDetailData
import com.example.fillin.data.repository.MemberRepository
import com.example.fillin.data.repository.MypageRepository
import com.example.fillin.data.repository.ReportRepository
import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReportType
import com.example.fillin.domain.model.ReportStatus
import com.example.fillin.ui.map.MapContent
import com.example.fillin.ui.map.PresentLocation
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.fillin.ui.theme.FILLINTheme
import com.example.fillin.ui.components.ReportCard
import com.example.fillin.ui.components.ReportCardUi
import com.example.fillin.ui.components.ValidityStatus
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.geometry.LatLng
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.fillin.feature.report.locationselect.CenterPin
import java.net.URL
import retrofit2.HttpException

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
fun HomeScreen(
    navController: NavController? = null,
    onHideBottomBar: () -> Unit = {},
    onShowBottomBar: () -> Unit = {}
) {
    SetStatusBarColor(color = Color.White, darkIcons = true)
    val context = LocalContext.current

    val appPreferences = remember { AppPreferences(context) }
    val currentUserNickname by appPreferences.nicknameFlow.collectAsState()
    val currentUserProfileImageUri by appPreferences.profileImageUriFlow.collectAsState()
    val currentUserMemberId by appPreferences.currentUserMemberIdFlow.collectAsState()

    val presentLocation = remember { PresentLocation(context) }
    var naverMap: NaverMap? by remember { mutableStateOf(null) }

    var selectedCategories by remember { mutableStateOf(setOf<ReportType>()) }
    var showNotificationBanner by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<ReportWithLocation?>(null) }
    var reportDetail by remember { mutableStateOf<ReportImageDetailData?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var detailLoadError by remember { mutableStateOf<String?>(null) }
    var showLoginPrompt by remember { mutableStateOf(false) }

    var userFeedbackSelections by remember(context) {
        mutableStateOf(SharedReportData.loadUserFeedbackSelections(context))
    }

    var userDeletedFromRegistered by remember { mutableStateOf(SharedReportData.loadUserDeletedFromRegisteredIds(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userDeletedFromRegistered = SharedReportData.loadUserDeletedFromRegisteredIds(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            userDeletedFromRegistered = SharedReportData.loadUserDeletedFromRegisteredIds(context)
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var userLikeStates by remember(context) {
        mutableStateOf(SharedReportData.loadUserLikeStates(context))
    }

    var currentUserLocation by remember { mutableStateOf<android.location.Location?>(null) }

    var showReportMenu by remember { mutableStateOf(false) }
    var isPastFlow by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var currentAddress by remember { mutableStateOf("주소를 불러오는 중...") }
    var isMapPickingMode by remember { mutableStateOf(false) }
    var finalLocation by remember { mutableStateOf("") }
    var finalLatitude by remember { mutableStateOf<Double?>(null) }
    var finalLongitude by remember { mutableStateOf<Double?>(null) }
    var isPastReportLocationMode by remember { mutableStateOf(false) }
    var isPastReportPhotoStage by remember { mutableStateOf(false) }
    var savedCameraPosition: CameraPosition? by remember { mutableStateOf(null) }

    val apiService = remember { RetrofitClient.geminiApi }
    val geminiRepository = remember { GeminiRepository(apiService) }
    val geminiViewModel: GeminiViewModel = viewModel(factory = GeminiViewModelFactory(geminiRepository))
    val mypageRepository = remember(context) { MypageRepository(context) }
    val reportRepository = remember(context) { ReportRepository(context) }
    val memberRepository = remember(context) { MemberRepository(context) }
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(reportRepository))
    var writerNicknamesByWriterId by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(capturedUri) {
        capturedUri?.let { uri ->
            reportViewModel.prepareImage(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showCamera = true
    }

    fun startPastFlow() {
        capturedUri = null
        geminiViewModel.clearResult()
        finalLocation = ""
        finalLatitude = null
        finalLongitude = null

        isPastFlow = true
        isPastReportLocationMode = true
        isPastReportPhotoStage = false
        isMapPickingMode = false
        showCamera = false
    }

    fun startRealtimeFlow() {
        capturedUri = null
        geminiViewModel.clearResult()
        finalLocation = ""
        finalLatitude = null
        finalLongitude = null

        isPastFlow = false
        isPastReportLocationMode = false
        isPastReportPhotoStage = false
        isMapPickingMode = false

        naverMap?.let { map ->
            savedCameraPosition = map.cameraPosition
        }

        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val backStackEntry = navController?.currentBackStackEntry
    val savedStateHandle = backStackEntry?.savedStateHandle
    LaunchedEffect(backStackEntry) {
        userDeletedFromRegistered = SharedReportData.loadUserDeletedFromRegisteredIds(context)
    }

    val reportFlowState = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("report_flow", null)
        ?.collectAsState()

    LaunchedEffect(reportFlowState?.value) {
        val flow = reportFlowState?.value
        if (!flow.isNullOrBlank()) {
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("report_flow")
            when (flow) {
                "past" -> startPastFlow()
                "realtime" -> startRealtimeFlow()
            }
        }
    }

    var updatedSampleReports by remember {
        mutableStateOf(SharedReportData.getReports().filter { it.report.id !in SharedReportData.loadUserPermanentlyDeletedIds(context) })
    }
    var reportListVersion by remember { mutableStateOf(0) }
    var lastUploadedLatLon by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val isRealtimeReportScreenVisible = geminiViewModel.aiResult.isNotEmpty() &&
            !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow

    val isPastReportScreenVisible = isPastFlow && !isPastReportPhotoStage &&
            !isPastReportLocationMode && capturedUri != null &&
            geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing

    val shouldHideBottomBar = remember(
        showCamera, selectedReport, isMapPickingMode, isPastReportLocationMode,
        isPastReportPhotoStage, isRealtimeReportScreenVisible, isPastReportScreenVisible, geminiViewModel.isAnalyzing
    ) {
        showCamera || selectedReport != null || isMapPickingMode || isPastReportLocationMode ||
                isPastReportPhotoStage || isRealtimeReportScreenVisible || isPastReportScreenVisible || geminiViewModel.isAnalyzing
    }

    LaunchedEffect(shouldHideBottomBar) {
        if (shouldHideBottomBar) onHideBottomBar() else onShowBottomBar()
    }

    LaunchedEffect(Unit) {
        val loaded = SharedReportData.loadPersisted(context)
        if (loaded.isNotEmpty()) {
            SharedReportData.setReports(loaded)
            val deletedIds = SharedReportData.loadUserPermanentlyDeletedIds(context)
            updatedSampleReports = loaded.filter { it.report.id !in deletedIds }
            reportListVersion++
        }
    }

    val permanentlyDeleted = remember(backStackEntry) { SharedReportData.loadUserPermanentlyDeletedIds(context) }
    LaunchedEffect(updatedSampleReports, permanentlyDeleted) {
        val list = updatedSampleReports.filter { it.report.id !in permanentlyDeleted }
        SharedReportData.setReports(list)
        SharedReportData.persist(context, list)
    }

    LaunchedEffect(Unit, userDeletedFromRegistered, reportViewModel.uploadStatus, reportViewModel.lastUploadTimeMillis) {
        if (reportViewModel.uploadStatus == true) return@LaunchedEffect
        if (reportViewModel.lastUploadTimeMillis > 0 && System.currentTimeMillis() - reportViewModel.lastUploadTimeMillis < 5000L) return@LaunchedEffect

        val defaultLat = 37.5665
        val defaultLon = 126.9780
        val userDeletedIds = SharedReportData.loadUserDeletedFromRegisteredIds(context)
        val isLoggedIn = TokenManager.getBearerToken(context) != null
        val currentMemberIdValue = appPreferences.getCurrentUserMemberId()

        var reports = if (isLoggedIn) {
            val myReports = mypageRepository.getMyReports().getOrNull()?.data?.mapNotNull { item ->
                val reportId = item.reportId ?: return@mapNotNull null
                val lat = item.latitude ?: defaultLat
                val lon = item.longitude ?: defaultLon
                val isUserOwned = item.memberId != null && currentMemberIdValue != null && item.memberId == currentMemberIdValue
                val existing = updatedSampleReports.find { it.report.id == reportId }
                val addressStr = item.address?.takeIf { it.isNotBlank() } ?: existing?.report?.title ?: ""
                val reportType = when (item.reportCategory) {
                    "DANGER" -> ReportType.DANGER
                    "INCONVENIENCE" -> ReportType.INCONVENIENCE
                    else -> ReportType.DISCOVERY
                }
                ReportWithLocation(
                    report = Report(id = reportId, documentId = reportId.toString(), title = addressStr, meta = item.title ?: "",
                        type = reportType, viewCount = item.viewCount, status = ReportStatus.ACTIVE, imageUrl = item.reportImageUrl,
                        isUserOwned = isUserOwned, writerId = item.memberId, reporterInfo = if (isUserOwned) SampleReportData.currentUser else null),
                    latitude = lat, longitude = lon
                )
            } ?: emptyList()

            val popularReports = reportRepository.getPopularReports().getOrNull()?.data?.popularReports?.mapNotNull { item ->
                val reportId = item.id ?: return@mapNotNull null
                if (myReports.any { it.report.id == reportId }) return@mapNotNull null
                val reportType = when (item.category) {
                    "DANGER" -> ReportType.DANGER
                    "INCONVENIENCE" -> ReportType.INCONVENIENCE
                    else -> ReportType.DISCOVERY
                }
                ReportWithLocation(
                    report = Report(id = reportId, documentId = reportId.toString(), title = item.address ?: "", meta = item.title ?: "",
                        type = reportType, viewCount = item.viewCount, status = ReportStatus.ACTIVE),
                    latitude = item.latitude ?: defaultLat, longitude = item.longitude ?: defaultLon
                )
            } ?: emptyList()
            myReports + popularReports
        } else {
            reportRepository.getPopularReports().getOrNull()?.data?.popularReports?.mapNotNull { item ->
                val reportId = item.id ?: return@mapNotNull null
                val reportType = when (item.category) {
                    "DANGER" -> ReportType.DANGER
                    "INCONVENIENCE" -> ReportType.INCONVENIENCE
                    else -> ReportType.DISCOVERY
                }
                ReportWithLocation(
                    report = Report(id = reportId, documentId = reportId.toString(), title = item.address ?: "", meta = item.title ?: "",
                        type = reportType, viewCount = item.viewCount, status = ReportStatus.ACTIVE),
                    latitude = item.latitude ?: defaultLat, longitude = item.longitude ?: defaultLon
                )
            } ?: emptyList()
        }

        val merged = (reports.map { rwl ->
            if (rwl.report.id in userDeletedIds) rwl.copy(report = rwl.report.copy(status = ReportStatus.EXPIRED)) else rwl
        } + updatedSampleReports.filter { loc -> reports.none { it.report.id == loc.report.id } }).distinctBy { it.report.id }

        updatedSampleReports = merged
    }

    LaunchedEffect(reportViewModel.uploadStatus, reportViewModel.lastUploadedReport) {
        if (reportViewModel.uploadStatus == true) {
            val uploaded = reportViewModel.lastUploadedReport
            if (uploaded != null) {
                val reportType = when (uploaded.category) {
                    "위험" -> ReportType.DANGER
                    "불편" -> ReportType.INCONVENIENCE
                    else -> ReportType.DISCOVERY
                }
                val newId = uploaded.documentId.toLongOrNull() ?: uploaded.documentId.hashCode().toLong()
                val lat = finalLatitude ?: currentUserLocation?.latitude ?: naverMap?.cameraPosition?.target?.latitude ?: 37.5665
                val lon = finalLongitude ?: currentUserLocation?.longitude ?: naverMap?.cameraPosition?.target?.longitude ?: 126.9780
                val newWithLocation = ReportWithLocation(
                    report = Report(id = newId, documentId = uploaded.documentId, title = uploaded.location, meta = uploaded.title,
                        type = reportType, viewCount = 0, status = ReportStatus.ACTIVE, imageUrl = uploaded.imageUrl, isUserOwned = true),
                    latitude = lat, longitude = lon
                )
                updatedSampleReports = updatedSampleReports + newWithLocation
                reportListVersion++
                lastUploadedLatLon = Pair(lat, lon)
            }
            Toast.makeText(context, "제보가 등록되었습니다.", Toast.LENGTH_SHORT).show()
            capturedUri = null
            geminiViewModel.clearResult()
            reportViewModel.resetStatus()
        }
    }

    val markerIconCache = remember { mutableMapOf<String, OverlayImage>() }
    val markers = remember { mutableListOf<Marker>() }
    var cameraZoomLevel by remember { mutableStateOf(16.0) }

    LaunchedEffect(naverMap, updatedSampleReports, reportListVersion, isMapPickingMode, isPastReportLocationMode, selectedCategories, cameraZoomLevel, userDeletedFromRegistered, permanentlyDeleted) {
        naverMap?.let { naverMapInstance ->
            // 📍 [방법 A] 위치 선택 모드일 때는 마커를 모두 숨김
            if (isMapPickingMode || isPastReportLocationMode) {
                markers.forEach { it.map = null }
                markers.clear()
                return@LaunchedEffect
            }

            val activeReports = updatedSampleReports.filter {
                it.report.id !in permanentlyDeleted && it.report.status == ReportStatus.ACTIVE &&
                        !(it.report.isUserOwned && it.report.id in userDeletedFromRegistered)
            }.distinctBy { it.report.id }

            markers.forEach { it.map = null }
            markers.clear()

            activeReports.forEach { rwl ->
                val isSelected = selectedCategories.isEmpty() || selectedCategories.contains(rwl.report.type)
                if (!isSelected) return@forEach

                val marker = Marker().apply {
                    position = LatLng(rwl.latitude, rwl.longitude)
                    map = naverMapInstance
                    icon = when(rwl.report.type) {
                        ReportType.DANGER -> OverlayImage.fromResource(R.drawable.ic_warning_selected)
                        ReportType.INCONVENIENCE -> OverlayImage.fromResource(R.drawable.ic_inconvenience_selected)
                        else -> OverlayImage.fromResource(R.drawable.ic_discovery)
                    }
                    setOnClickListener {
                        selectedReport = updatedSampleReports.find { it.report.id == rwl.report.id }
                        true
                    }
                }
                markers.add(marker)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            naverMap?.let { map ->
                presentLocation.setupLocationOverlay(map)
                presentLocation.moveMapToCurrentLocation(map)
                presentLocation.startLocationUpdates(map)
            }
        }
    }

    LaunchedEffect(naverMap) {
        naverMap?.let { map ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                presentLocation.setupLocationOverlay(map)
                presentLocation.moveMapToCurrentLocation(map)
                presentLocation.startLocationUpdates(map)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expandedHeight = 162.dp * (maxWidth / 380.dp)
        val navBarTotalHeight = expandedHeight + 40.dp

        MapContent(
            modifier = Modifier.fillMaxSize(),
            viewModel = reportViewModel,
            onMapReady = { map ->
                naverMap = map
                cameraZoomLevel = map.cameraPosition.zoom
                map.addOnCameraIdleListener {
                    cameraZoomLevel = map.cameraPosition.zoom
                    val pos = map.cameraPosition.target
                    // 📍 주소 실시간 업데이트
                    presentLocation.getAddressFromCoords(pos.latitude, pos.longitude) { address ->
                        currentAddress = address
                    }
                }
            }
        )

        if (!isMapPickingMode && !isPastReportLocationMode) {
            LocationButton(
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = navBarTotalHeight + 20.dp).padding(end = 16.dp),
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        naverMap?.let { presentLocation.moveMapToCurrentLocation(it) }
                    } else {
                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            )

            CategoryFilterRow(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = navBarTotalHeight + 20.dp).padding(start = 16.dp),
                selectedCategories = selectedCategories,
                onCategoryToggle = { category ->
                    selectedCategories = if (selectedCategories.contains(category)) selectedCategories - category else selectedCategories + category
                }
            )
        }

        // 📍 [방법 A] 위치 선택 모드 UI 오버레이
        if (isMapPickingMode || isPastReportLocationMode) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 35.dp)) {
                CenterPin()
            }

            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = Color.White, shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isMapPickingMode = false; isPastReportLocationMode = false }) {
                        Icon(painter = painterResource(id = R.drawable.btn_close), contentDescription = "닫기", tint = Color.Unspecified)
                    }
                    Text(
                        text = if (isPastReportLocationMode) "지난 상황 제보" else "위치 선택",
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    )
                    Spacer(Modifier.size(48.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.9f), shadowElevation = 4.dp) {
                    Text("지도를 움직여 제보 위치를 설정해주세요.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, color = colorResource(R.color.grey5)))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.fillMaxWidth().height(48.dp), color = Color(0xFFF8FAFF), shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4090E0), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(text = currentAddress, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val target = naverMap?.cameraPosition?.target
                                if (target != null) {
                                    finalLocation = currentAddress; finalLatitude = target.latitude; finalLongitude = target.longitude
                                    if (isPastReportLocationMode) {
                                        isPastReportLocationMode = false
                                        if (capturedUri == null) isPastReportPhotoStage = true
                                    } else { isMapPickingMode = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(53.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4090E0)), shape = RoundedCornerShape(30.dp)
                        ) { Text("해당 위치로 설정", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }

        if (geminiViewModel.aiResult.isNotEmpty() && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow) {
            ReportRegistrationScreen(
                topBarTitle = "실시간 제보", viewModel = reportViewModel, imageUri = capturedUri, initialTitle = geminiViewModel.aiResult,
                initialLocation = finalLocation.ifEmpty { currentAddress }, onLocationFieldClick = { isMapPickingMode = true },
                onDismiss = { geminiViewModel.clearResult() },
                onRegister = { category, title, location, uri ->
                    val lat = finalLatitude ?: currentUserLocation?.latitude ?: naverMap?.cameraPosition?.target?.latitude ?: 37.5665
                    val lon = finalLongitude ?: currentUserLocation?.longitude ?: naverMap?.cameraPosition?.target?.longitude ?: 126.9780
                    reportViewModel.uploadReport(category, title, uri, location, lat, lon)
                }
            )
        }

        if (showReportMenu) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)).clickable { showReportMenu = false })
            ReportOptionMenu(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navBarTotalHeight + 20.dp),
                onPastReportClick = { showReportMenu = false; startPastFlow() },
                onRealtimeReportClick = { showReportMenu = false; startRealtimeFlow() })
        }

        if (showCamera) {
            RealtimeReportScreen(onDismiss = { showCamera = false }, onReportSubmit = { uri ->
                capturedUri = uri; showCamera = false
                BuildConfig.GEMINI_API_KEY.let { if(it.isNotEmpty()) geminiViewModel.analyzeImage(context, uri, it) }
            })
        }

        if (isPastReportPhotoStage) {
            PastReportPhotoSelectionScreen(onClose = { isPastReportPhotoStage = false }, onPhotoSelected = { uri ->
                capturedUri = uri; isPastReportPhotoStage = false
                BuildConfig.GEMINI_API_KEY.let { if(it.isNotEmpty()) geminiViewModel.analyzeImage(context, uri, it) }
            })
        }

        if (isPastFlow && !isPastReportPhotoStage && !isPastReportLocationMode && capturedUri != null &&
            geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing) {
            ReportRegistrationScreen(
                topBarTitle = "지난 상황 제보", viewModel = reportViewModel, imageUri = capturedUri, initialTitle = geminiViewModel.aiResult,
                initialLocation = finalLocation, onLocationFieldClick = { isPastReportLocationMode = true },
                onDismiss = { capturedUri = null; geminiViewModel.clearResult() },
                onRegister = { category, title, location, uri ->
                    val lat = finalLatitude ?: 37.5665; val lon = finalLongitude ?: 126.9780
                    reportViewModel.uploadReport(category, title, uri, location, lat, lon)
                }
            )
        }

        if (geminiViewModel.isAnalyzing || reportViewModel.isUploading) AiLoadingOverlay(isUploading = reportViewModel.isUploading)

        selectedReport?.let { rwl ->
            val reportCardUi = convertToReportCardUi(rwl, currentUserLocation, currentUserNickname, currentUserProfileImageUri, currentUserMemberId)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { selectedReport = null })
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ReportCard(report = reportCardUi, isLiked = userLikeStates[rwl.report.id] ?: rwl.report.isSaved,
                        onPositiveFeedback = { /* 피드백 로직 */ }, onNegativeFeedback = { /* 피드백 로직 */ }, onLikeToggle = { /* 좋아요 로직 */ })
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White).clickable { selectedReport = null }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "닫기", tint = Color.Black)
                    }
                }
            }
        }
    }
}

/** ReportWithLocation을 ReportCardUi로 변환.
 * 표시하는 닉네임/프로필/뱃지는 항상 해당 제보 등록자(작성자) 정보.
 * 본인 제보 여부: writerId == currentUserMemberId 로 판단 (둘 다 있을 때), 없으면 report.isUserOwned 사용.
 */
private fun convertToReportCardUi(
    reportWithLocation: ReportWithLocation,
    currentUserLocation: android.location.Location?,
    currentUserNickname: String = "사용자",
    currentUserProfileImageUri: String? = null,
    currentUserMemberId: Long? = null
): ReportCardUi {
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
    
    val report = reportWithLocation.report
    val isUserOwned = if (report.writerId != null && currentUserMemberId != null)
        report.writerId == currentUserMemberId
    else
        report.isUserOwned

    // 타입에 따른 라벨과 색상
    val (typeLabel, typeColor) = when (report.type) {
        ReportType.DANGER -> "위험" to Color(0xFFFF6060)
        ReportType.INCONVENIENCE -> "불편" to Color(0xFF4595E5)
        ReportType.DISCOVERY -> "발견" to Color(0xFF29C488)
    }
    
    // 날짜 포맷팅 (예: "5일 전")
    val daysAgo = (System.currentTimeMillis() - report.createdAtMillis) / (24 * 60 * 60 * 1000)
    val createdLabel = if (daysAgo == 0L) "오늘" else "${daysAgo}일 전"
    
    // 상세 배너 주소: 도로명 주소로 도로명과 건물번호만 표기 (예: "양화로 188"), 패턴 없으면 시/구·역 출구 설명 제거한 값
    val addressDisplay = formatRoadAddressOnly(report.title).ifBlank {
        report.title.replace(Regex("^[가-힣]+(?:시|도)\\s+[가-힣]+(?:구|시)\\s*"), "")
            .replace(Regex("\\s*[가-힣]*역\\s*\\d+번\\s*출구\\s*앞.*"), "").trim()
    }
    
    // 제목: report.meta가 실제 제목 (예: "맨홀 뚜껑 역류")
    val title = report.meta // meta가 제목
    
    // 유효성 상태 계산
    val validityStatus = calculateValidityStatus(report)
    
    // 거리 계산
    val distance = if (currentUserLocation != null) {
        val distanceMeters = calculateDistanceMeters(
            currentUserLocation.latitude,
            currentUserLocation.longitude,
            reportWithLocation.latitude,
            reportWithLocation.longitude
        )
        "가는 길 ${distanceMeters.toInt()}m"
    } else {
        ""
    }
    
    return ReportCardUi(
        reportId = report.id,
        validityStatus = validityStatus,
        imageRes = report.imageResId ?: R.drawable.ic_report_img,
        imageUrl = report.imageUrl,
        imageUri = report.imageUri,
        views = report.viewCount,
        typeLabel = typeLabel,
        typeColor = typeColor,
        userName = if (isUserOwned) currentUserNickname else (report.reporterInfo?.nickname ?: "사용자"),
        userBadge = if (isUserOwned) SharedReportData.getBadgeName() else "루키",
        profileImageUrl = report.reporterInfo?.profileImageUrl,
        profileImageUri = if (isUserOwned && report.reporterInfo?.profileImageUrl.isNullOrBlank() && !currentUserProfileImageUri.isNullOrBlank())
            Uri.parse(currentUserProfileImageUri) else null,
        title = title,
        createdLabel = createdLabel,
        address = addressDisplay,
        distance = distance,
        okCount = report.positiveFeedbackCount,
        dangerCount = report.negativeFeedbackCount,
        isLiked = report.isSaved
    )
}

/** 제보 상세 API 응답을 ReportCardUi로 변환.
 * 표시하는 닉네임/프로필/뱃지는 항상 해당 제보 등록자(작성자) 정보.
 * 본인 제보일 때만 fallbackNickname/fallbackProfileImageUri로 현재 사용자 앱 저장값 사용.
 * @param fallbackAddress API에 주소가 없을 때 사용할 주소
 * @param fallbackNickname 작성자 닉네임 fallback (본인=앱 저장 닉네임, 타인=writerId 조회 등)
 * @param fallbackProfileImageUri 작성자 프로필 이미지 fallback (본인 제보일 때만 앱 저장 이미지)
 */
private fun convertDetailToReportCardUi(
    detail: ReportImageDetailData,
    currentUserLocation: android.location.Location?,
    fallbackAddress: String = "",
    fallbackNickname: String? = null,
    fallbackProfileImageUri: Uri? = null,
    isLiked: Boolean
): ReportCardUi {
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    val reportId = detail.reportId ?: 0L
    val lat = detail.latitude ?: 0.0
    val lon = detail.longitude ?: 0.0

    val validityStatus = when (detail.validType) {
        "최근에도 확인됐어요" -> ValidityStatus.VALID
        "제보 의견이 나뉘어요" -> ValidityStatus.INTERMEDIATE
        "오래된 제보일 수 있어요" -> ValidityStatus.INVALID
        else -> ValidityStatus.VALID
    }

    val (typeLabel, typeColor) = when (detail.reportCategory) {
        "DANGER" -> "위험" to Color(0xFFFF6060)
        "INCONVENIENCE" -> "불편" to Color(0xFF4595E5)
        "DISCOVERY" -> "발견" to Color(0xFF29C488)
        else -> "발견" to Color(0xFF29C488)
    }

    val userBadge = when (detail.achievement) {
        "ROOKIE" -> "루키"
        "VETERAN" -> "베테랑"
        "MASTER" -> "마스터"
        else -> "루키"
    }

    val createdLabel = try {
        val createAt = detail.createAt ?: ""
        if (createAt.isBlank()) "오늘" else {
            val parsed = java.time.LocalDateTime.parse(createAt.take(19))
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
            val now = java.time.Instant.now()
            val daysAgo = java.time.Duration.between(parsed, now).toDays()
            if (daysAgo == 0L) "오늘" else "${daysAgo}일 전"
        }
    } catch (_: Exception) {
        "오늘"
    }

    // API의 address가 없으면 로컬 주소(fallbackAddress) 우선 사용 (새 제보 등)
    val addressDisplay = when {
        !detail.address.isNullOrBlank() -> formatRoadAddressOnly(detail.address!!).ifBlank { detail.address!! }
        !fallbackAddress.isBlank() -> formatRoadAddressOnly(fallbackAddress).ifBlank { fallbackAddress }
        else -> ""
    }

    val distance = if (currentUserLocation != null) {
        val distanceMeters = calculateDistanceMeters(
            currentUserLocation.latitude, currentUserLocation.longitude, lat, lon
        )
        "가는 길 ${distanceMeters.toInt()}m"
    } else ""

    return ReportCardUi(
        reportId = reportId,
        validityStatus = validityStatus,
        imageRes = R.drawable.ic_report_img,
        imageUrl = detail.reportImageUrl,
        imageUri = null,
        views = detail.viewCount,
        typeLabel = typeLabel,
        typeColor = typeColor,
        userName = detail.nickname?.takeIf { it.isNotBlank() } ?: fallbackNickname?.takeIf { it.isNotBlank() } ?: "사용자",
        userBadge = userBadge,
        profileImageUrl = detail.profileImageUrl,
        profileImageUri = fallbackProfileImageUri, // 본인 제보일 때만 전달됨 → 해당 제보 등록자(현재 사용자) 프로필 표시
        title = detail.title ?: "",
        createdLabel = createdLabel,
        address = addressDisplay.ifBlank { fallbackAddress }.ifBlank { detail.address ?: "" },
        distance = distance,
        okCount = detail.doneCount,
        dangerCount = detail.nowCount,
        isLiked = isLiked
    )
}

/** 주소를 짧게: 도로명 주소면 "도로명 + 건물번호", 지번이면 "동 + 번지"만 표기 (시/구 등 제거) */
private fun formatRoadAddressOnly(fullAddress: String): String {
    if (fullAddress.isBlank()) return fullAddress
    // 0. "가는 길 000m" 등 거리 문구가 붙어 있으면 제거 (주소만 사용)
    var s = fullAddress.replace(Regex("\\s+가는\\s+길\\s+\\S+$"), "").trim()
    // 1. 앞부분 제거: "대한민국 ", "서울 ", "서울특별시 ", "경기도 ", "영등포구 ", "성남시 분당구 " 등
    s = s
        .replace(Regex("^(?:대한민국\\s+)?"), "")
        .replace(Regex("^(?:서울|부산|대구|인천|광주|대전|울산|세종)\\s+"), "")  // "서울 " 등 (시 없이 쓴 경우)
        .replace(Regex("^(?:[가-힣]+(?:시|도|특별시|광역시)\\s*)+"), "")
        .replace(Regex("^(?:[가-힣]+(?:구|시|군)\\s*)+"), "")
        .trim()
    // 2. 뒤쪽 설명 제거: " 홍대입구역 1번 출구 앞", " 00역 2번 출구" 등
    s = s.replace(Regex("\\s+[가-힣]*역\\s*\\d*번?\\s*출구.*"), "").trim()
    s = s.replace(Regex("\\s+앞\\s*$"), "").trim()
    // 3. 도로명 + 건물번호 (로/대로/길 + 숫자) 우선
    val roadPattern = Regex("[가-힣]+(?:로|대로|길)\\s*\\d+(?:-\\d+)?")
    val roadMatch = roadPattern.find(s)
    if (roadMatch != null) {
        val raw = roadMatch.value.replace(Regex("\\s+"), " ")
        return raw.replace(Regex("(로|대로|길)(\\d)"), "$1 $2").trim()
    }
    // 4. 지번 주소면 "동 + 번지"만 (예: "여의도동 84-2")
    val dongPattern = Regex("[가-힣]+동\\s*\\d+(?:-\\d+)?")
    val dongMatch = dongPattern.find(s)
    if (dongMatch != null) return dongMatch.value.replace(Regex("\\s+"), " ").trim()
    return s
}

// 유효성 상태 계산 함수
// - 긍정 70% 이상 3일 이상 유지 -> 최근에도 확인됐어요
// - 긍정 40~60% 3일 이상 유지 -> 제보 의견이 나뉘어요
// - 등록 2주 이상 -> 오래된 제보일 수 있어요
private fun calculateValidityStatus(report: Report): ValidityStatus {
    val currentTimeMillis = System.currentTimeMillis()
    val twoWeeksInMillis = 14 * 24 * 60 * 60 * 1000L // 2주
    val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L // 3일
    
    // 조건 1: 등록한지 2주 이상 된 제보는 "오래된 제보일 수 있어요"
    val daysSinceCreation = currentTimeMillis - report.createdAtMillis
    if (daysSinceCreation >= twoWeeksInMillis) {
        return ValidityStatus.INVALID
    }
    
    // 조건 2: 피드백 비율 + 3일 유지로 판단
    val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount
    if (totalFeedback == 0) {
        return ValidityStatus.VALID
    }
    
    val positiveRatio = report.positiveFeedbackCount.toDouble() / totalFeedback
    
    // 긍정 70% 이상 3일 이상 유지 -> "최근에도 확인됐어요"
    report.positive70SustainedSinceMillis?.let { since ->
        if (positiveRatio >= 0.7 && (currentTimeMillis - since) >= threeDaysInMillis) {
            return ValidityStatus.VALID
        }
    }
    
    // 긍정 40~60% 3일 이상 유지 -> "제보 의견이 나뉘어요"
    report.positive40to60SustainedSinceMillis?.let { since ->
        if (positiveRatio >= 0.4 && positiveRatio <= 0.6 && (currentTimeMillis - since) >= threeDaysInMillis) {
            return ValidityStatus.INTERMEDIATE
        }
    }
    
    // 3일 유지 미달 또는 그 외 비율 -> 기본 유효
    return ValidityStatus.VALID
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
    
    // 상세 배너 주소: 도로명과 건물번호만 표기 (패턴 없으면 원본 주소에서 시/구 제거한 값 사용)
    val addressDisplay = remember(report.title) {
        formatRoadAddressOnly(report.title).ifBlank {
            report.title.replace(Regex("^[가-힣]+(?:시|도)\\s+[가-힣]+(?:구|시)\\s*"), "")
                .replace(Regex("\\s*[가-힣]*역\\s*\\d+번\\s*출구\\s*앞.*"), "").trim()
        }
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp), // pill 모양
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // 카테고리 컬러 점
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )
                
            Spacer(Modifier.width(8.dp))
                
            // 주소와 제보 내용(meta) 표시
                Row(
                modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // 도로명 + 건물번호만 표시
                    Text(
                        text = addressDisplay,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF555659), // 회색 텍스트
                        maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                // 제보 내용(meta) 표시
                    Text(
                        text = " ${report.meta}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF555659), // 회색 텍스트
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
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
        Image(
            painter = painterResource(id = R.drawable.ic_user_location),
            contentDescription = "내 위치",
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
                    .padding(horizontal = 32.dp)
                    .wrapContentWidth(),
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
