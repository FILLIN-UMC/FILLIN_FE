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
    // 상태바를 밝은 배경에 어두운 아이콘으로 설정
    SetStatusBarColor(color = Color.White, darkIcons = true)
    val context = LocalContext.current
    
    // 앱 설정에서 현재 사용자 닉네임·프로필 이미지 가져오기
    val appPreferences = remember { AppPreferences(context) }
    val currentUserNickname by appPreferences.nicknameFlow.collectAsState()
    val currentUserProfileImageUri by appPreferences.profileImageUriFlow.collectAsState()
    val currentUserMemberId by appPreferences.currentUserMemberIdFlow.collectAsState()

    val presentLocation = remember { PresentLocation(context) }
    var naverMap: NaverMap? by remember { mutableStateOf(null) }
    
    // 카테고리 선택 상태 (다중 선택 가능)
    var selectedCategories by remember { 
        mutableStateOf(setOf<ReportType>()) 
    }
    
    // 알림 배너 표시 여부
    var showNotificationBanner by remember { mutableStateOf(true) }
    
    // 선택된 제보 (마커 클릭 시 표시할 제보)
    var selectedReport by remember { mutableStateOf<ReportWithLocation?>(null) }
    // 제보 상세 API 응답 (마커 클릭 시 조회, 목록 데이터보다 상세 정보 우선 표시)
    var reportDetail by remember { mutableStateOf<ReportImageDetailData?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var detailLoadError by remember { mutableStateOf<String?>(null) }
    var showLoginPrompt by remember { mutableStateOf(false) }
    
    // 사용자 피드백 선택 상태 추적 (reportId -> "positive" | "negative" | null)
    var userFeedbackSelections by remember(context) { 
        mutableStateOf(SharedReportData.loadUserFeedbackSelections(context))
    }
    
    // 나의 제보에서 사용자가 삭제(사라진 제보로 이동)한 제보 ID (지도/마이페이지에서 숨김)
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

    // 사용자 좋아요 상태 추적 (reportId -> Boolean)
    var userLikeStates by remember(context) { 
        mutableStateOf(SharedReportData.loadUserLikeStates(context))
    }
    
    // 사용자 현재 위치
    var currentUserLocation by remember { mutableStateOf<android.location.Location?>(null) }
    
    // === [제보 기능 관련 상태] ===
    var showReportMenu by remember { mutableStateOf(false) } // 제보 메뉴 표시 여부
    var isPastFlow by remember { mutableStateOf(false) } // 현재 지난 상황 제보 흐름인지 확인
    var showCamera by remember { mutableStateOf(false) } // 카메라 화면 표시 여부
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var currentAddress by remember { mutableStateOf("서울시 용산구 행복대로 392") } // 예시 주소
    var isMapPickingMode by remember { mutableStateOf(false) } // 위치 선택 모드 상태
    var finalLocation by remember { mutableStateOf("") } // 확정된 주소 저장
    var finalLatitude by remember { mutableStateOf<Double?>(null) } // 지난 상황 제보 선택 좌표
    var finalLongitude by remember { mutableStateOf<Double?>(null) }
    var isPastReportLocationMode by remember { mutableStateOf(false) } // 위치 설정 단계
    var isPastReportPhotoStage by remember { mutableStateOf(false) } // 사진 선택 단계
    var savedCameraPosition: CameraPosition? by remember { mutableStateOf(null) } // 카메라 실행 전 지도 위치 저장
    
    // === [AI 및 DB 관련 ViewModel] ===
    val apiService = remember { RetrofitClient.geminiApi }
    val geminiRepository = remember { GeminiRepository(apiService) }
    val geminiViewModel: GeminiViewModel = viewModel(factory = GeminiViewModelFactory(geminiRepository))
    val mypageRepository = remember(context) { MypageRepository(context) }
    val reportRepository = remember(context) { ReportRepository(context) }
    val memberRepository = remember(context) { MemberRepository(context) }
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(reportRepository))
    // writerId로 조회한 작성자 닉네임 캐시 (타인 제보 카드에서 상세 API에 nickname 없을 때 사용)
    var writerNicknamesByWriterId by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // ✨ [추가] 사진이 촬영되거나 선택되자마자 전처리(모자이크)를 시작하는 로직
    LaunchedEffect(capturedUri) {
        capturedUri?.let { uri ->
            Log.d("ReportDebug", "CapturedUri 감지 - 전처리(번호판 감지)를 시작합니다: $uri")
            reportViewModel.prepareImage(uri)
        }
    }
    // === [권한 Launcher] ===
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            Log.e("Permission", "카메라 권한 거부됨")
        }
    }
    
    //  === [제보 플로우 함수 수정] ===
    fun startPastFlow() {
        // [추가] 새로운 제보를 위해 이전 데이터 초기화
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
        // [추가] 새로운 제보를 위해 이전 데이터 초기화
        capturedUri = null
        geminiViewModel.clearResult()
        finalLocation = ""
        finalLatitude = null
        finalLongitude = null

        isPastFlow = false
        isPastReportLocationMode = false
        isPastReportPhotoStage = false
        isMapPickingMode = false
        
        // 카메라 실행 전 현재 지도 위치 저장
        naverMap?.let { map ->
            savedCameraPosition = map.cameraPosition
        }
        
        val permissionCheckResult =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // === [MainScreen에서 전달된 제보 플로우 처리] ===
    val backStackEntry = navController?.currentBackStackEntry
    val savedStateHandle = backStackEntry?.savedStateHandle
    LaunchedEffect(backStackEntry) {
        userDeletedFromRegistered = SharedReportData.loadUserDeletedFromRegisteredIds(context)
    }
    
    // savedStateHandle의 변경을 감지하기 위해 주기적으로 체크
    // savedStateHandle.set() 후 즉시 감지되지 않을 수 있으므로 주기적 체크 사용
    var lastReportFlow by remember(backStackEntry) { mutableStateOf<String?>(null) }
    var lastSelectedReportId by remember(backStackEntry) { mutableStateOf<Long?>(null) }

    // 샘플 제보 데이터 (마이그레이션 완료 시 사용 안 함)
    // SharedReportData에서 초기값 로드: 마이페이지 갔다가 홈 복귀 시 새로 등록한 제보 보존
    var updatedSampleReports by remember {
        mutableStateOf(SharedReportData.getReports().filter { it.report.id !in SharedReportData.loadUserPermanentlyDeletedIds(context) })
    }
    var reportListVersion by remember { mutableStateOf(0) } // 업로드 시 마커 갱신 강제
    var lastUploadedLatLon by remember { mutableStateOf<Pair<Double, Double>?>(null) } // 업로드 후 카메라 이동용

    // [수정] 본인의 저장소(savedStateHandle)에서 "report_flow"를 실시간 관찰하는 상태 생성
    val reportFlowState = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("report_flow", null)
        ?.collectAsState()

    // [수정] 이제 reportFlowState.value가 바뀔 때마다 이 블록이 자동으로 실행됩니다.
    LaunchedEffect(reportFlowState?.value) {
        val flow = reportFlowState?.value
        if (!flow.isNullOrBlank()) {
            Log.d("HomeScreen", "Received flow: $flow")

            // 신호를 받았으니 즉시 삭제 (중복 실행 방지)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("report_flow")

            when (flow) {
                "past" -> startPastFlow()
                "realtime" -> startRealtimeFlow()
            }
        }
    }
 /*   LaunchedEffect(backStackEntry) {
        if (backStackEntry == null) return@LaunchedEffect
        
        // backStackEntry가 변경되면 lastReportFlow 초기화
        lastReportFlow = null
        lastSelectedReportId = null
        
        while (true) {
            val flow = savedStateHandle?.get<String>("report_flow")
            // flow가 있고, 이전에 처리하지 않은 값인 경우에만 처리
            if (!flow.isNullOrBlank() && flow != lastReportFlow) {
                Log.d("HomeScreen", "Detected report_flow: $flow")
                // 먼저 remove하여 중복 실행 방지
                savedStateHandle?.remove<String>("report_flow")
                // lastReportFlow 업데이트하여 중복 처리 방지
                lastReportFlow = flow
                
                when (flow) {
                    "past" -> {
                        Log.d("HomeScreen", "Starting past flow")
                        startPastFlow()
                    }
                    "realtime" -> {
                        Log.d("HomeScreen", "Starting realtime flow")
                        // naverMap이 있으면 위치 저장, 없어도 startRealtimeFlow 호출
                        if (naverMap != null) {
                            savedCameraPosition = naverMap?.cameraPosition
                        }
                        startRealtimeFlow()
                    }
                }
            }
            
            delay(50) // 50ms마다 체크
        }
    } */
    
    val isRealtimeReportScreenVisible = geminiViewModel.aiResult.isNotEmpty() &&
            !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow

// [223라인] 지난 상황 제보 등록 화면이 보여야 하는 조건
    val isPastReportScreenVisible = isPastFlow && !isPastReportPhotoStage &&
            !isPastReportLocationMode && capturedUri != null &&
            geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing

    // [수정] 모든 오버레이 상태를 감시하여 네비게이션 바 표시 여부를 한 번에 결정합니다.
    val shouldHideBottomBar = remember(
        showCamera,
        selectedReport,
        isMapPickingMode,
        isPastReportLocationMode,
        isPastReportPhotoStage,
        isRealtimeReportScreenVisible,
        isPastReportScreenVisible,
        geminiViewModel.isAnalyzing, // AI 분석 상태 추가
    ) {
        showCamera ||
                selectedReport != null ||
                isMapPickingMode ||
                isPastReportLocationMode ||
                isPastReportPhotoStage ||
                isRealtimeReportScreenVisible ||
                isPastReportScreenVisible ||
                geminiViewModel.isAnalyzing  // 분석 중일 때 숨김

    }

// 통합된 네비게이션 바 제어 로직
    LaunchedEffect(shouldHideBottomBar) {
        if (shouldHideBottomBar) {
            onHideBottomBar()
        } else {
            onShowBottomBar()
        }
    }
    // === [카메라가 켜지면 네비게이션 바 숨기기, 닫히면 저장된 지도 위치로 복원] ===
 /*   LaunchedEffect(showCamera) {
        if (showCamera) {
            onHideBottomBar()
        } else {
            // 카메라가 닫힐 때 lastReportFlow 초기화하여 다시 실시간 제보를 누를 수 있도록 함
            lastReportFlow = null
            savedCameraPosition?.let { savedPosition ->
                naverMap?.let { map ->
                    val cameraUpdate = CameraUpdate.scrollAndZoomTo(
                        savedPosition.target,
                        savedPosition.zoom
                    ).animate(CameraAnimation.Easing)
                    map.moveCamera(cameraUpdate)
                }
            }
            // ReportRegistrationScreen이 표시되지 않을 때만 네비게이션 바를 다시 보이게 함
            val isRealtimeReportScreenVisible = geminiViewModel.aiResult.isNotEmpty() && 
                !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow
            val isPastReportScreenVisible = isPastFlow && !isPastReportPhotoStage && 
                !isPastReportLocationMode && capturedUri != null && 
                geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing
            
            // 위치 선택, 사진 선택, 위치 설정 모드도 확인
            if (!isRealtimeReportScreenVisible && !isPastReportScreenVisible && 
                !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode) {
                onShowBottomBar()
            }
        }
    } */
    
    // === [제보 등록 화면 표시 여부 확인] ===
/*    val isRealtimeReportScreenVisible = geminiViewModel.aiResult.isNotEmpty() &&
        !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow
    val isPastReportScreenVisible = isPastFlow && !isPastReportPhotoStage && 
        !isPastReportLocationMode && capturedUri != null && 
        geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing */
    
    // === [제보 카드 상태를 savedStateHandle에 저장 및 네비게이션 바 숨기기] ===
/*    LaunchedEffect(selectedReport) {
        navController?.currentBackStackEntry?.savedStateHandle?.set(
            "report_card_visible",
            selectedReport != null
        )
    // 제보 카드가 표시될 때 네비게이션 바 숨기기
        if (selectedReport != null) {
            onHideBottomBar()
        } else {
            // 제보 카드가 닫힐 때, 다른 오버레이가 표시되지 않을 때만 네비게이션 바를 다시 보이게 함
            if (!showCamera && !isRealtimeReportScreenVisible && !isPastReportScreenVisible && 
                !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode) {
            onShowBottomBar()
        }
    }
    } */
    
/*    // === [제보 등록 화면이 표시될 때 네비게이션 바 숨기기] ===
    LaunchedEffect(isRealtimeReportScreenVisible, isPastReportScreenVisible) {
        if (isRealtimeReportScreenVisible || isPastReportScreenVisible) {
            onHideBottomBar()
        } else if (!showCamera && selectedReport == null && !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode) {
            // 카메라도 닫혀있고 제보 등록 화면도 닫혀있고 제보 카드도 닫혀있고 위치 선택/사진 선택 모드도 아닐 때만 네비게이션 바를 다시 보이게 함
            onShowBottomBar()
        }
    }
    
    // === [위치 선택 모드, 사진 선택 모드, 위치 설정 모드일 때 네비게이션 바 숨기기] ===
    LaunchedEffect(isMapPickingMode, isPastReportPhotoStage, isPastReportLocationMode) {
        if (isMapPickingMode || isPastReportPhotoStage || isPastReportLocationMode) {
            onHideBottomBar()
        } else if (!showCamera && selectedReport == null && !isRealtimeReportScreenVisible && !isPastReportScreenVisible) {
            // 다른 제보 관련 화면이 모두 닫혀있을 때만 네비게이션 바를 다시 보이게 함
            onShowBottomBar()
        }
    } */
    
    // 앱 재실행 시: 저장된 제보 목록 복원 (지도·마이페이지 총 제보 수 유지)
    LaunchedEffect(Unit) {
        val loaded = SharedReportData.loadPersisted(context)
        if (loaded.isNotEmpty()) {
            SharedReportData.setReports(loaded)
            val deletedIds = SharedReportData.loadUserPermanentlyDeletedIds(context)
            updatedSampleReports = loaded.filter { it.report.id !in deletedIds }
            reportListVersion++
            Log.d("HomeScreen", "제보 목록 복원: ${loaded.size}건")
        }
    }

    // 제보 데이터를 공유 객체에 저장 (완전 삭제한 제보 제외) + 디스크에 persist (앱 재실행 시 복원용)
    val permanentlyDeleted = remember(backStackEntry) { SharedReportData.loadUserPermanentlyDeletedIds(context) }
    LaunchedEffect(updatedSampleReports, permanentlyDeleted) {
        val list = updatedSampleReports.filter { it.report.id !in permanentlyDeleted }
        SharedReportData.setReports(list)
        SharedReportData.persist(context, list)
    }

    // === [백엔드 API에서만 제보 로드] ===
    // uploadStatus가 true일 때, 또는 업로드 직후 5초 이내에는 덮어쓰지 않음 (새 제보 마커 보존, ViewModel에 두어 재진입 시에도 유지)
    LaunchedEffect(Unit, userDeletedFromRegistered, reportViewModel.uploadStatus, reportViewModel.lastUploadTimeMillis) {
        if (reportViewModel.uploadStatus == true) return@LaunchedEffect
        if (reportViewModel.lastUploadTimeMillis > 0 && System.currentTimeMillis() - reportViewModel.lastUploadTimeMillis < 5000L) return@LaunchedEffect
        val defaultLat = 37.5665
        val defaultLon = 126.9780
        val userDeletedIds = SharedReportData.loadUserDeletedFromRegisteredIds(context)

        val isLoggedIn = TokenManager.getBearerToken(context) != null
        val currentUserMemberId = appPreferences.getCurrentUserMemberId()
        var reports = if (isLoggedIn) {
            // 로그인: 내 제보 + 인기 제보 (다른 사람 제보에 좋아요 가능하도록)
            val myResult = mypageRepository.getMyReports()
            val myData = myResult.getOrNull()?.data
            val myReports = myData?.mapNotNull { item ->
                val reportId = item.reportId ?: return@mapNotNull null
                val lat = item.latitude ?: defaultLat
                val lon = item.longitude ?: defaultLon
                val writerId = item.memberId
                val isUserOwned = writerId != null && currentUserMemberId != null && writerId == currentUserMemberId
                // API에 주소가 없으면 기존 목록의 주소 유지 (마이페이지 다녀온 후 새 제보 주소 사라짐 방지)
                val existing = updatedSampleReports.find { it.report.id == reportId }
                val addressStr = item.address?.takeIf { it.isNotBlank() } ?: existing?.report?.title ?: ""
                val reportType = when (item.reportCategory) {
                    "DANGER" -> ReportType.DANGER
                    "INCONVENIENCE" -> ReportType.INCONVENIENCE
                    "DISCOVERY" -> ReportType.DISCOVERY
                    else -> ReportType.DISCOVERY
                }
                ReportWithLocation(
                    report = Report(
                        id = reportId,
                        documentId = reportId.toString(),
                        title = addressStr,
                        meta = item.title ?: "",
                        type = reportType,
                        viewCount = item.viewCount,
                        status = ReportStatus.ACTIVE,
                        imageUrl = item.reportImageUrl,
                        isUserOwned = isUserOwned,
                        writerId = writerId,
                        reporterInfo = if (isUserOwned) SampleReportData.currentUser else null
                    ),
                    latitude = lat,
                    longitude = lon
                )
            }?.distinctBy { it.report.id } ?: emptyList()
            val popResult = reportRepository.getPopularReports()
            val popularList = popResult.getOrNull()?.data?.popularReports
            val myIds = myReports.map { it.report.id }.toSet()
            val popularReports = popularList?.mapNotNull { item ->
                val reportId = item.id ?: return@mapNotNull null
                if (reportId in myIds) return@mapNotNull null
                val lat = item.latitude ?: defaultLat
                val lon = item.longitude ?: defaultLon
                val reportType = when (item.category) {
                    "DANGER" -> ReportType.DANGER
                    "INCONVENIENCE" -> ReportType.INCONVENIENCE
                    "DISCOVERY" -> ReportType.DISCOVERY
                    else -> ReportType.DISCOVERY
                }
                ReportWithLocation(
                    report = Report(
                        id = reportId,
                        documentId = reportId.toString(),
                        title = item.address ?: "",
                        meta = item.title ?: "",
                        type = reportType,
                        viewCount = item.viewCount,
                        status = ReportStatus.ACTIVE,
                        imageUrl = null,
                        isUserOwned = false,
                        writerId = null,
                        reporterInfo = null
                    ),
                    latitude = lat,
                    longitude = lon
                )
            } ?: emptyList()
            Log.d("HomeScreen", "[제보 로드] 로그인됨 → 내 제보 ${myReports.size}개 + 인기 제보 ${popularReports.size}개 (저장 가능)")
            myReports + popularReports
        } else {
            // 비로그인: 인기 제보 (최대 6개)
            val result = reportRepository.getPopularReports()
            val popularList = result.getOrNull()?.data?.popularReports
            Log.d("HomeScreen", "[제보 로드] 비로그인 → getPopularReports() 호출, 응답 개수=${popularList?.size ?: 0}, 성공=${result.isSuccess}")
            popularList?.mapNotNull { item ->
                val reportId = item.id ?: return@mapNotNull null
                val lat = item.latitude ?: defaultLat
                val lon = item.longitude ?: defaultLon
                val reportType = when (item.category) {
                    "DANGER" -> ReportType.DANGER
                    "INCONVENIENCE" -> ReportType.INCONVENIENCE
                    "DISCOVERY" -> ReportType.DISCOVERY
                    else -> ReportType.DISCOVERY
                }
                ReportWithLocation(
                    report = Report(
                        id = reportId,
                        documentId = reportId.toString(),
                        title = item.address ?: "",
                        meta = item.title ?: "",
                        type = reportType,
                        viewCount = item.viewCount,
                        status = ReportStatus.ACTIVE,
                        imageUrl = null,
                        isUserOwned = false,
                        writerId = null,
                        reporterInfo = null
                    ),
                    latitude = lat,
                    longitude = lon
                )
            }?.distinctBy { it.report.id } ?: emptyList()
        }

        val reportsWithExpired = reports.map { rwl ->
            if (rwl.report.id in userDeletedIds) {
                rwl.copy(report = rwl.report.copy(status = ReportStatus.EXPIRED))
            } else rwl
        }
        // API 응답으로 덮어쓸 때, 업로드 LaunchedEffect에서 추가한 제보는 보존 (레이스 컨디션 방지)
        val apiIds = reportsWithExpired.map { it.report.id }.toSet()
        val locallyAdded = updatedSampleReports.filter { it.report.id !in apiIds }
        var merged = reportsWithExpired + locallyAdded
        // 방금 올린 제보가 API 결과에 없을 수 있음(지연/캐시) → ViewModel 스냅샷으로 재추가
        reportViewModel.lastUploadedReportId?.let { guardId ->
            if (merged.none { it.report.id == guardId }) {
                reportViewModel.lastUploadedReportSnapshot?.let { snapshot ->
                    val reportType = when (snapshot.category) {
                        "위험" -> ReportType.DANGER
                        "불편" -> ReportType.INCONVENIENCE
                        else -> ReportType.DISCOVERY
                    }
                    val report = Report(
                        id = snapshot.id,
                        documentId = snapshot.documentId,
                        title = snapshot.location,
                        meta = snapshot.title,
                        type = reportType,
                        viewCount = 0,
                        status = ReportStatus.ACTIVE,
                        imageUrl = null,
                        imageUri = null,
                        isUserOwned = true,
                        writerId = currentUserMemberId,
                        reporterInfo = SampleReportData.currentUser
                    )
                    merged = merged + ReportWithLocation(report = report, latitude = snapshot.latitude, longitude = snapshot.longitude)
                    Log.d("HomeScreen", "[제보 로드] 방금 올린 제보 보존: id=${snapshot.id}")
                }
            }
        }
        updatedSampleReports = merged
    }

    // === [업로드 결과 관찰 및 알림 처리 + 지도에 새 제보 추가] ===
    LaunchedEffect(reportViewModel.uploadStatus, reportViewModel.lastUploadedReport) {
        if (reportViewModel.uploadStatus == true) {
            val uploaded = reportViewModel.lastUploadedReport
            if (uploaded != null) {
                val reportType = when (uploaded.category) {
                    "위험" -> ReportType.DANGER
                    "불편" -> ReportType.INCONVENIENCE
                    else -> ReportType.DISCOVERY
                }
                val newId = uploaded.documentId.toLongOrNull()
                    ?: uploaded.documentId.hashCode().toLong().and(0x7FFFFFFFL).coerceAtLeast(10000L)
                // 지난 상황 제보: 선택한 좌표 사용 / 실시간 제보: 현재 위치 사용
                val lat = finalLatitude ?: currentUserLocation?.latitude ?: naverMap?.cameraPosition?.target?.latitude ?: 37.5665
                val lon = finalLongitude ?: currentUserLocation?.longitude ?: naverMap?.cameraPosition?.target?.longitude ?: 126.9780
                val currentMemberId = appPreferences.getCurrentUserMemberId()
                val newReport = Report(
                    id = newId,
                    documentId = uploaded.documentId,
                    title = uploaded.location,
                    meta = uploaded.title,
                    type = reportType,
                    viewCount = 0,
                    status = ReportStatus.ACTIVE,
                    imageUrl = uploaded.imageUrl,
                    imageUri = uploaded.imageUri,
                    isUserOwned = true,
                    writerId = currentMemberId,
                    reporterInfo = SampleReportData.currentUser
                )
                val newWithLocation = ReportWithLocation(
                    report = newReport,
                    latitude = lat,
                    longitude = lon
                )
                updatedSampleReports = updatedSampleReports + newWithLocation
                reportListVersion++
                reportViewModel.setUploadGuard(newId, lat, lon, uploaded.category, uploaded.title, uploaded.location, uploaded.documentId)
                lastUploadedLatLon = Pair(lat, lon)
                Log.d("HomeScreen", "새 제보 추가됨: id=$newId, lat=$lat, lon=$lon, total=${updatedSampleReports.size}")
            }
            Toast.makeText(context, "제보가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
            capturedUri = null
            geminiViewModel.clearResult()
            delay(600)
            // API에서 최신 목록 재조회 후 병합 (서버에 반영된 새 제보 포함, 마커 표시 보장)
            if (TokenManager.getBearerToken(context) != null) {
                val userDeletedIds = SharedReportData.loadUserDeletedFromRegisteredIds(context)
                val currentMemberId = appPreferences.getCurrentUserMemberId()
                mypageRepository.getMyReports().getOrNull()?.data?.let { items ->
                    val defaultLat = 37.5665
                    val defaultLon = 126.9780
                    // 방금 업로드한 제보(newId)는 API에 좌표가 아직 null일 수 있음 → 업로드 시 사용한 좌표 유지
                    val uploadLat = finalLatitude ?: currentUserLocation?.latitude ?: naverMap?.cameraPosition?.target?.latitude ?: defaultLat
                    val uploadLon = finalLongitude ?: currentUserLocation?.longitude ?: naverMap?.cameraPosition?.target?.longitude ?: defaultLon
                    val apiReports = items.mapNotNull { item ->
                        val reportId = item.reportId ?: return@mapNotNull null
                        val isNewUpload = (uploaded != null && reportId == (uploaded.documentId.toLongOrNull() ?: uploaded.documentId.hashCode().toLong().and(0x7FFFFFFFL).coerceAtLeast(10000L)))
                        val itemLat = item.latitude ?: if (isNewUpload) uploadLat else defaultLat
                        val itemLon = item.longitude ?: if (isNewUpload) uploadLon else defaultLon
                        val writerId = item.memberId
                        val isUserOwned = writerId != null && currentMemberId != null && writerId == currentMemberId
                        val existing = updatedSampleReports.find { it.report.id == reportId }
                        val addressStr = item.address?.takeIf { it.isNotBlank() }
                            ?: if (isNewUpload) (uploaded?.location ?: "") else (existing?.report?.title ?: "")
                        val itemType = when (item.reportCategory) {
                            "DANGER" -> ReportType.DANGER
                            "INCONVENIENCE" -> ReportType.INCONVENIENCE
                            "DISCOVERY" -> ReportType.DISCOVERY
                            else -> ReportType.DISCOVERY
                        }
                        // 💡 [핵심 해결 로직] 서버가 준 URL(item.reportImageUrl)보다
                        // 로컬에 이미 떠 있는 모자이크 URL(existingLocally?.report?.imageUrl)을 우선 사용합니다.
                        val finalDisplayUrl = if (isNewUpload || existing?.report?.imageUrl != null) {
                            existing?.report?.imageUrl ?: item.reportImageUrl
                        } else {
                            item.reportImageUrl
                        }
                        ReportWithLocation(
                            report = Report(
                                id = reportId,
                                documentId = reportId.toString(),
                                title = addressStr,
                                meta = item.title ?: "",
                                type = itemType,
                                viewCount = item.viewCount,
                                status = ReportStatus.ACTIVE,
                                imageUrl = finalDisplayUrl, // 👈 서버 데이터 대신 로컬 정답을 유지!
                                isUserOwned = isUserOwned,
                                writerId = writerId,
                                reporterInfo = if (isUserOwned) SampleReportData.currentUser else null
                            ),
                            latitude = itemLat,
                            longitude = itemLon
                        )
                    }
                    val reportsWithExpired = apiReports.map { rwl ->
                        if (rwl.report.id in userDeletedIds) {
                            rwl.copy(report = rwl.report.copy(status = ReportStatus.EXPIRED))
                        } else rwl
                    }
                    val apiIds = reportsWithExpired.map { it.report.id }.toSet()
                    val locallyAdded = updatedSampleReports.filter { it.report.id !in apiIds }
                    updatedSampleReports = reportsWithExpired + locallyAdded
                    Log.d("HomeScreen", "업로드 후 API 재조회: total=${updatedSampleReports.size}")
                }
            }
            reportListVersion++
            delay(400)
            reportListVersion++
            reportViewModel.resetStatus()
            delay(300)
            lastUploadedLatLon = null // 카메라 이동 후 초기화
            reportViewModel.scheduleClearUploadGuard(5000L) // 5초 후 가드 해제 (그동안 API 덮어쓰기 방지)
        } else if (reportViewModel.uploadStatus == false) {
            val errorMsg = reportViewModel.uploadErrorMessage
                ?: "등록에 실패했습니다. 다시 시도해주세요."
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            reportViewModel.resetStatus()
        }
    }
    
    // === [알림에서 제보 선택 처리] ===
    LaunchedEffect(backStackEntry, updatedSampleReports) {
        if (backStackEntry == null) return@LaunchedEffect
        
        // backStackEntry가 변경되면 lastSelectedReportId 초기화
        lastSelectedReportId = null
        
        while (true) {
            // selected_report_id 감지 및 처리
            val reportId = savedStateHandle?.get<Long>("selected_report_id")
            if (reportId != null && reportId != lastSelectedReportId) {
                Log.d("HomeScreen", "Detected selected_report_id: $reportId")
                // 먼저 remove하여 중복 실행 방지
                savedStateHandle?.remove<Long>("selected_report_id")
                // lastSelectedReportId 업데이트하여 중복 처리 방지
                lastSelectedReportId = reportId
                
                // 해당 제보 찾기
                val targetReport = updatedSampleReports.find { it.report.id == reportId }
                if (targetReport != null) {
                    // 제보 선택 및 지도 이동
                    selectedReport = targetReport
                    naverMap?.let { map ->
                        val cameraUpdate = CameraUpdate.scrollTo(
                            LatLng(targetReport.latitude, targetReport.longitude)
                        ).animate(CameraAnimation.Easing)
                        map.moveCamera(cameraUpdate)
                    }
                } else {
                    Log.w("HomeScreen", "Report with id $reportId not found")
                }
            }
            
            delay(50) // 50ms마다 체크
        }
    }
    
    // 제보 상세 API 호출 (마커 클릭 시)
    LaunchedEffect(selectedReport) {
        reportDetail = null
        detailLoadError = null
        showLoginPrompt = false
        val report = selectedReport?.report ?: return@LaunchedEffect
        val docId = report.documentId?.toLongOrNull()
        // documentId가 Long으로 변환 불가면 백엔드에 없는 제보(Firestore 등) → API 호출 스킵
        if (docId == null) {
            isLoadingDetail = false
            Log.d("HomeScreen", "제보 상세 API 스킵: documentId가 백엔드 ID가 아님 (reportId=${report.id})")
            return@LaunchedEffect
        }
        isLoadingDetail = true
        val result = reportRepository.getReportDetail(docId)
        isLoadingDetail = false
        result.onSuccess { response ->
            response.data?.let { data ->
                reportDetail = data
                Log.d("HomeScreen", "제보 상세 API 성공: reportId=${data.reportId}")
            }
        }.onFailure { e ->
            Log.w("HomeScreen", "제보 상세 API 실패: reportId=${report.id}, docId=$docId", e)
            val isUnauthorized = (e as? HttpException)?.code() == 401
            val isNotFound = (e as? HttpException)?.code() == 404
            when {
                isUnauthorized -> showLoginPrompt = true
                isNotFound -> Log.d("HomeScreen", "제보를 찾을 수 없음 (404) - 기본 정보만 표시")
                else -> detailLoadError = "상세 정보를 불러오지 못했습니다"
            }
        }
    }

    // 타인 제보: 상세에 nickname 없을 때 writerId로 회원 API 조회 후 캐시 (해당 제보 등록자 닉네임 표시용)
    LaunchedEffect(reportDetail) {
        val detail = reportDetail ?: return@LaunchedEffect
        val writerId = detail.writerId ?: return@LaunchedEffect
        if (!detail.nickname.isNullOrBlank()) return@LaunchedEffect
        val nickname = memberRepository.getMemberNickname(writerId) ?: return@LaunchedEffect
        writerNicknamesByWriterId = writerNicknamesByWriterId + (writerId to nickname)
    }

    // 에러/비로그인 안내 Toast 표시
    LaunchedEffect(detailLoadError, showLoginPrompt) {
        detailLoadError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            detailLoadError = null
        }
        if (showLoginPrompt) {
            Toast.makeText(context, "로그인하면 더 자세한 정보를 볼 수 있어요", Toast.LENGTH_SHORT).show()
            showLoginPrompt = false
        }
    }
    
    // 좋아요 토글 함수
    fun toggleLike(reportId: Long) {
        val reportWithLocation = updatedSampleReports.find { it.report.id == reportId } ?: return
        val currentLikeState = userLikeStates[reportId] ?: reportWithLocation.report.isSaved
        val newLikeState = !currentLikeState
        userLikeStates = userLikeStates + (reportId to newLikeState)
        SharedReportData.saveUserLikeState(context, reportId, newLikeState)
        val docId = reportWithLocation.report.documentId
        scope.launch {
            val backendReportId = docId?.toLongOrNull()
            if (backendReportId != null && TokenManager.getBearerToken(context) != null) {
                reportRepository.likeToggle(backendReportId)
            } else {
                SharedReportData.saveUserLikeState(context, reportId, newLikeState)
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
        
        // 제보 데이터 업데이트 (부정 피드백 시점 목록: 부정 추가 시 append, 부정 취소 시 최근 1건 제거)
        val now = System.currentTimeMillis()
        updatedSampleReports = updatedSampleReports.map { reportWithLocation ->
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
                // 백엔드 제보는 API 호출, 그 외는 SharedPreferences
                val backendReportId = updatedReport.documentId?.toLongOrNull()
                if (backendReportId != null && newSelection != null && TokenManager.getBearerToken(context) != null) {
                    scope.launch {
                        val feedbackType = if (newSelection == "positive") "NOW" else "DONE"
                        reportRepository.createFeedback(backendReportId, feedbackType)
                    }
                } else {
                    SharedReportData.saveFeedbackToPreferences(
                        context,
                        reportId,
                        updatedPositiveCount,
                        updatedNegativeCount,
                        updatedReport.positive70SustainedSinceMillis,
                        updatedReport.positive40to60SustainedSinceMillis,
                        updatedNegativeTimestamps,
                        updatedReport.feedbackConditionMetAtMillis,
                        updatedReport.expiringAtMillis
                    )
                }
                reportWithLocation.copy(report = updatedReport)
            } else {
                reportWithLocation
            }
        }
        // SharedReportData에도 반영 (완전 삭제한 제보 제외) + 디스크에 persist
        val permanentlyDeletedIds = SharedReportData.loadUserPermanentlyDeletedIds(context)
        val listToStore = updatedSampleReports.filter { it.report.id !in permanentlyDeletedIds }
        SharedReportData.setReports(listToStore)
        SharedReportData.persist(context, listToStore)
    }
    
    // 현재 시간 기준 최근 3일 제보 필터링 및 정렬 (ACTIVE 상태만)
    val filteredAndSortedReports = remember(updatedSampleReports) {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L) // 3일 전
        
        val filtered = updatedSampleReports
            .filter { reportWithLocation: ReportWithLocation ->
                // ACTIVE 상태이고 최근 3일 필터링
                reportWithLocation.report.status == ReportStatus.ACTIVE &&
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
        
        Log.d("HomeScreen", "Filtered reports count: ${filtered.size}, Total reports: ${updatedSampleReports.size}")
        filtered
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
            val index = currentNotificationIndex.coerceIn(0, filteredAndSortedReports.size - 1)
            filteredAndSortedReports[index]
        } else {
            Log.d("HomeScreen", "No filtered reports available for notification banner")
            null
        }
    }
    
    // 디버깅: currentReport 상태 로깅
    LaunchedEffect(currentReport, filteredAndSortedReports.size) {
        Log.d("HomeScreen", "Current report: ${currentReport?.report?.meta}, Filtered count: ${filteredAndSortedReports.size}, Index: $currentNotificationIndex")
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
    
    // URL에서 비트맵 로드 (등록 제보 이미지로 마커 아이콘 생성용)
    suspend fun loadBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.e("HomeScreen", "loadBitmapFromUrl failed: ${e.message}")
            null
        }
    }
    
    // 비트맵을 원형 마커 아이콘으로 변환 (등록 시 사용한 이미지와 동일하게 표시)
    fun createCircularMarkerIconFromBitmap(bitmap: Bitmap, sizeDp: Int = 42, backgroundColor: Int = android.graphics.Color.WHITE): OverlayImage {
        val density = context.resources.displayMetrics.density
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
        val backgroundSizeDp = sizeDp.toFloat()
        val backgroundSizePx = (backgroundSizeDp * density).toInt()
        val markerBitmap = Bitmap.createBitmap(backgroundSizePx, backgroundSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(markerBitmap)
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(0f, 0f, backgroundSizePx.toFloat(), backgroundSizePx.toFloat()), backgroundPaint)
        val imageSizeDp = (sizeDp - 4).toFloat()
        val imageSizePx = (imageSizeDp * density).toInt()
        val imageOffset = (backgroundSizePx - imageSizePx) / 2
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, imageSizePx, imageSizePx, true)
        val circularImageBitmap = Bitmap.createBitmap(imageSizePx, imageSizePx, Bitmap.Config.ARGB_8888)
        val imageCanvas = Canvas(circularImageBitmap)
        val maskPaint = Paint().apply { isAntiAlias = true }
        val imageRect = RectF(0f, 0f, imageSizePx.toFloat(), imageSizePx.toFloat())
        imageCanvas.drawOval(imageRect, maskPaint)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        imageCanvas.drawBitmap(resizedBitmap, null, imageRect, maskPaint)
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
    
    // 사용자 현재 위치 가져오기
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(5000)
                .build()
            
            try {
                fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token)
                    .addOnSuccessListener { location ->
                        location?.let {
                            currentUserLocation = it
                        }
                    }
            } catch (e: Exception) {
                Log.e("Location", "현재 위치를 가져오는데 실패했습니다", e)
            }
        }
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
    
    // imageUrl 마커 아이콘 캐시 (줌/확대 시 깜빡임 방지)
    val markerIconCache = remember { mutableMapOf<String, OverlayImage>() }
    
    // 업로드 후 새 제보 위치로 카메라 이동 (마커가 화면 밖에 있을 수 있어 시인성 보장)
    LaunchedEffect(naverMap, lastUploadedLatLon) {
        val (lat, lon) = lastUploadedLatLon ?: return@LaunchedEffect
        naverMap?.let { map ->
            val cameraUpdate = CameraUpdate.scrollTo(LatLng(lat, lon)).animate(CameraAnimation.Easing)
            map.moveCamera(cameraUpdate)
            Log.d("HomeScreen", "카메라 이동: 새 제보 위치 (lat=$lat, lon=$lon)")
        }
    }
    
    // 지도 마커 표시 (줌 레벨에 따라 개별 마커 또는 클러스터 마커)
    // reportViewModel.lastUploadTimeMillis, lastUploadedLatLon: 업로드 직후 naverMap이 아직 null일 수 있어, 지도 준비 후에도 마커 갱신 보장
    LaunchedEffect(naverMap, updatedSampleReports, reportListVersion, reportViewModel.lastUploadTimeMillis, lastUploadedLatLon, selectedCategories, cameraZoomLevel, userDeletedFromRegistered, permanentlyDeleted) {
        naverMap?.let { naverMapInstance ->
            // ACTIVE 상태인 제보만 필터링 (나의 제보에서 삭제한 제보, 완전 삭제한 제보는 지도에 표시 안 함)
            // 서버 DB 중복 제거: report.id 기준으로 첫 번째만 유지
            val activeReports = updatedSampleReports
                .filter {
                    it.report.id !in permanentlyDeleted &&
                    it.report.status == ReportStatus.ACTIVE &&
                    !(it.report.isUserOwned && it.report.id in userDeletedFromRegistered)
                }
                .distinctBy { it.report.id }
            
            // 줌 레벨이 14 이하이면 클러스터링 (단, 카테고리 선택 시에는 개별 마커 표시하여 크기 변화 보임)
            if (cameraZoomLevel <= 14.0 && selectedCategories.isEmpty()) {
            // 기존 마커 제거
            markers.forEach { it.map = null }
            markers.clear()
                // 클러스터링 로직 (카테고리 구분 없이 거리만으로 묶기)
                val clusters = mutableListOf<Cluster>()
                val processed = BooleanArray(activeReports.size) { false }
                
                activeReports.forEachIndexed { index, reportWithLocation ->
                    if (processed[index]) return@forEachIndexed
                    
                    val cluster = Cluster(
                        centerLat = reportWithLocation.latitude,
                        centerLon = reportWithLocation.longitude,
                        reports = mutableListOf(reportWithLocation)
                    )
                    processed[index] = true
                    
                    // 가까운 제보들을 클러스터 중심점 기준으로 묶기 (300m 이내, 카테고리 구분 없음)
                    var changed = true
                    while (changed) {
                        changed = false
                        activeReports.forEachIndexed { otherIndex, otherReport ->
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
                // imageUrl 제보: 40dp(비선택) / 80dp(선택) 아이콘을 미리 로드해 캐시 → 앱 시작·카테고리 탭 시 깜빡임 제거
                val reportsWithImage = activeReports.filter { it.report.imageUrl?.isNotBlank() == true }
                coroutineScope {
                    reportsWithImage.forEach { reportWithLocation ->
                        launch {
                            val url = reportWithLocation.report.imageUrl!!
                            val bitmap = loadBitmapFromUrl(url) ?: return@launch
                            val white = android.graphics.Color.WHITE
                            val selectedBg = when (reportWithLocation.report.type) {
                                ReportType.DANGER -> android.graphics.Color.parseColor("#FF6060")
                                ReportType.INCONVENIENCE -> android.graphics.Color.parseColor("#F5C72F")
                                ReportType.DISCOVERY -> android.graphics.Color.parseColor("#29C488")
                            }
                            if (markerIconCache["${url}_40_$white"] == null) {
                                markerIconCache["${url}_40_$white"] = createCircularMarkerIconFromBitmap(bitmap, 40, white)
                            }
                            if (markerIconCache["${url}_80_$selectedBg"] == null) {
                                markerIconCache["${url}_80_$selectedBg"] = createCircularMarkerIconFromBitmap(bitmap, 80, selectedBg)
                            }
                        }
                    }
                }
                // 기존 마커 제거 후 개별 마커 생성 (캐시에 아이콘이 있으므로 한 번에 올바른 이미지 표시)
                markers.forEach { it.map = null }
                markers.clear()
                activeReports.forEach { reportWithLocation ->
                    val isSelected = selectedCategories.contains(reportWithLocation.report.type)
                    val markerSize = if (isSelected) 80 else 40
                    val backgroundColor = if (isSelected) {
                        when (reportWithLocation.report.type) {
                            ReportType.DANGER -> android.graphics.Color.parseColor("#FF6060")
                            ReportType.INCONVENIENCE -> android.graphics.Color.parseColor("#F5C72F")
                            ReportType.DISCOVERY -> android.graphics.Color.parseColor("#29C488")
                        }
                    } else {
                        android.graphics.Color.WHITE
                    }
                    val defaultIcon = when (reportWithLocation.report.type) {
                        ReportType.DANGER -> createCircularMarkerIcon(R.drawable.ic_report_img, markerSize, backgroundColor)
                        ReportType.INCONVENIENCE -> createCircularMarkerIcon(R.drawable.ic_report_img_2, markerSize, backgroundColor)
                        ReportType.DISCOVERY -> createCircularMarkerIcon(R.drawable.ic_report_img_3, markerSize, backgroundColor)
                    }
                    val imageUrl = reportWithLocation.report.imageUrl
                    val cacheKey = if (!imageUrl.isNullOrBlank()) "${imageUrl}_${markerSize}_$backgroundColor" else null
                    val cachedIcon = cacheKey?.let { markerIconCache[it] }
                    val marker = Marker().apply {
                        position = LatLng(reportWithLocation.latitude, reportWithLocation.longitude)
                        map = naverMapInstance
                        icon = cachedIcon ?: defaultIcon
                        setOnClickListener {
                            val latestReport = updatedSampleReports.find {
                                it.report.id == reportWithLocation.report.id
                            } ?: reportWithLocation
                            selectedReport = latestReport
                            true
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
                    // 지도 위치가 변경될 때마다 savedStateHandle에 저장 (위도, 경도, 줌 레벨)
                    val position = map.cameraPosition
                    // [추가] 지도 중앙 좌표를 주소로 변환하여 currentAddress 업데이트
                    // 유저님의 PresentLocation이나 별도의 Geocoder 함수를 사용하세요.
                    presentLocation.getAddressFromCoords(
                        position.target.latitude,
                        position.target.longitude
                    ) { address ->
                        currentAddress = address // 실시간 주소 반영
                    }
                    navController?.currentBackStackEntry?.savedStateHandle?.apply {
                        set("home_camera_lat", position.target.latitude)
                        set("home_camera_lng", position.target.longitude)
                        set("home_camera_zoom", position.zoom)
                    }
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
        
        // 내 위치 버튼 (카테고리 필터와 동일한 높이, 네비게이션바 끝 위치)
        LocationButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = navBarTotalHeight + 20.dp) // 카테고리 필터와 동일한 높이
                .padding(end = 16.dp),
            onClick = {
                // naverMap이 null이면 아무 동작도 하지 않음
                if (naverMap == null) {
                    return@LocationButton
                }
                
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 권한이 있으면 바로 위치로 이동
                    naverMap?.let { map ->
                        presentLocation.moveMapToCurrentLocation(map)
                    }
                } else {
                    // 권한이 없으면 권한 요청
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
        
        // === [제보 관련 UI 오버레이] ===
        
        // [1. 제보 등록 화면 오버레이] - 실시간 제보
        // ReportRegistrationScreen을 항상 유지하고, 위치 선택 시 LocationSelectionScreen을 그 위에 오버레이
        // → 장소 변경 후 돌아와도 사용자가 수정한 제목이 유지됨
        if (geminiViewModel.aiResult.isNotEmpty() && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow) {
            Box(modifier = Modifier.fillMaxSize()) {
                ReportRegistrationScreen(
                    topBarTitle = "실시간 제보",
                    viewModel = reportViewModel,
                    imageUri = capturedUri,
                    initialTitle = geminiViewModel.aiResult,
                    initialLocation = finalLocation.ifEmpty { currentAddress },
                    onLocationFieldClick = { isMapPickingMode = true },
                    onDismiss = { geminiViewModel.clearResult() },
                    onRegister = { category, title, location, uri ->
                        val accessToken = TokenManager.getAccessToken(context)
                        if (accessToken != null) {
                            val lat = finalLatitude ?: currentUserLocation?.latitude ?: naverMap?.cameraPosition?.target?.latitude ?: 37.5665
                            val lon = finalLongitude ?: currentUserLocation?.longitude ?: naverMap?.cameraPosition?.target?.longitude ?: 126.9780
                            reportViewModel.uploadReport(category, title, uri, location, lat, lon)
                        } else if (TokenManager.getTempToken(context) != null) {
                            Toast.makeText(context, "온보딩을 완료한 후 제보를 등록할 수 있습니다.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "로그인 후 제보를 등록할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                if (isMapPickingMode) {
                    LocationSelectionScreen(
                        initialAddress = finalLocation.ifEmpty { currentAddress },
                        onBack = { isMapPickingMode = false },
                        onLocationSet = { selectedAddress, lat, lon ->
                            finalLocation = selectedAddress
                            finalLatitude = lat
                            finalLongitude = lon
                            isMapPickingMode = false
                        }
                    )
                }
            }
        }
        
        // [2. 제보 메뉴 오버레이]
        if (showReportMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable { showReportMenu = false }
            )
            ReportOptionMenu(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarTotalHeight + 20.dp),
                onPastReportClick = {
                    showReportMenu = false
                    startPastFlow()
                },
                onRealtimeReportClick = {
                    showReportMenu = false
                    startRealtimeFlow()
                }
            )
        }
        
        // [3. 카메라 화면 오버레이]
        if (showCamera) {
            RealtimeReportScreen(
                onDismiss = { showCamera = false },
                onReportSubmit = { uri ->
                    capturedUri = uri
                    showCamera = false
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isNotEmpty()) {
                        geminiViewModel.analyzeImage(
                            context = context,
                            uri = uri,
                            apiKey = apiKey
                        )
                    } else {
                        Toast.makeText(context, "GEMINI_API_KEY가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        

        
        // [5. 지난 상황 제보 - 위치 설정 화면]
        if (isPastReportLocationMode) {
            PastReportLocationScreen(
                initialAddress = finalLocation.ifEmpty { currentAddress },
                onBack = { isPastReportLocationMode = false },
                onLocationSet = { selectedAddress, lat, lon ->
                    finalLocation = selectedAddress
                    finalLatitude = lat
                    finalLongitude = lon
                    isPastReportLocationMode = false
                    // [핵심 로직] 이미 사진이 있다면(등록 화면에서 위치 수정을 위해 온 경우)
                    // 사진 선택 단계로 가지 않고 바로 등록 화면으로 돌아갑니다.
                    if (capturedUri == null) {
                        isPastReportPhotoStage = true       // 사진이 없을 때만 사진 선택 단계로 이동
                    }
                    // 사진이 이미 있다면, isPastReportScreenVisible 조건에 의해
                    // 자동으로 ReportRegistrationScreen이 다시 뜹니다.
                }
            )
        }
        
        // [6. 지난 상황 제보 - 갤러리 사진 선택 화면]
        if (isPastReportPhotoStage) {
            PastReportPhotoSelectionScreen(
                onClose = { isPastReportPhotoStage = false },
                onPhotoSelected = { uri ->
                    capturedUri = uri
                    isPastReportPhotoStage = false
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isNotEmpty()) {
                        geminiViewModel.analyzeImage(
                            context = context,
                            uri = uri,
                            apiKey = apiKey
                        )
                    } else {
                        Toast.makeText(context, "GEMINI_API_KEY가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        
        // [7. 지난 상황 제보 - 등록 화면]
        if (isPastFlow && !isPastReportPhotoStage && !isPastReportLocationMode && capturedUri != null &&
            geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing) {
            ReportRegistrationScreen(
                topBarTitle = "지난 상황 제보",
                viewModel = reportViewModel,
                imageUri = capturedUri,
                initialTitle = geminiViewModel.aiResult,
                initialLocation = finalLocation,
                onLocationFieldClick = {
                    isPastReportLocationMode = true
                },
                onDismiss = {
                    capturedUri = null
                    geminiViewModel.clearResult()
                },
                onRegister = { category, title, location, uri ->
                    val accessToken = TokenManager.getAccessToken(context)
                    if (accessToken != null) {
                        val lat = finalLatitude ?: currentUserLocation?.latitude ?: naverMap?.cameraPosition?.target?.latitude ?: 37.5665
                        val lon = finalLongitude ?: currentUserLocation?.longitude ?: naverMap?.cameraPosition?.target?.longitude ?: 126.9780
                        reportViewModel.uploadReport(category, title, uri, location, lat, lon)
                    } else if (TokenManager.getTempToken(context) != null) {
                        Toast.makeText(context, "온보딩을 완료한 후 제보를 등록할 수 있습니다.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "로그인 후 제보를 등록할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // [4. AI 분석 중 / 제보 등록 중 로딩 오버레이]
        if (geminiViewModel.isAnalyzing || reportViewModel.isUploading) {
            AiLoadingOverlay(isUploading = reportViewModel.isUploading)
        }

        // 상단 알림 배너 (제보 카드가 표시되어도 그대로 표시, 단 제보 흐름 진행 중에는 숨김)
        val isReportFlowActive = showCamera || isRealtimeReportScreenVisible || isPastReportScreenVisible || 
            isPastReportPhotoStage || isPastReportLocationMode || isMapPickingMode
        if (showNotificationBanner && currentReport != null && !isReportFlowActive) {
            NotificationBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .padding(horizontal = 32.dp)
                    .wrapContentWidth(),
                report = currentReport.report,
                onDismiss = { showNotificationBanner = false }
            )
        }
        
        // 제보 카드 표시 (마커 클릭 시) - 가장 마지막에 렌더링하여 최상위 레벨에 표시
        selectedReport?.let { reportWithLocation ->
            // 피드백 업데이트 시 selectedReport도 업데이트
            // updatedSampleReports에서 최신 데이터를 가져와서 항상 최신 피드백 수를 표시
            val currentReportWithLocation = remember(updatedSampleReports, reportWithLocation.report.id) {
                updatedSampleReports.find { it.report.id == reportWithLocation.report.id } 
                    ?: reportWithLocation
            }
            // 제보 상세 API 응답이 있으면 우선 사용 (viewCount, doneCount, nowCount, validType 등 최신 반영)
            val reportCardUi = remember(
                reportDetail, currentReportWithLocation, currentUserLocation, currentUserNickname, currentUserProfileImageUri, currentUserMemberId, userLikeStates, writerNicknamesByWriterId
            ) {
                val detail = reportDetail
                val reportId = currentReportWithLocation.report.id
                // 상세 API가 있으면 writerId로 본인 제보 여부 판단, 없으면 목록의 isUserOwned 사용
                val isOwnReport = if (detail != null && (detail.reportId ?: 0L) == reportId)
                    detail.writerId != null && currentUserMemberId != null && detail.writerId == currentUserMemberId
                else
                    currentReportWithLocation.report.isUserOwned
                if (detail != null && (detail.reportId ?: 0L) == reportId) {
                    // 주소: API 없으면 로컬 우선
                    val fallbackAddr = currentReportWithLocation.report.title.ifBlank { reportWithLocation.report.title }
                    // 닉네임: 항상 해당 제보 등록자(작성자) 표시. 본인 제보만 앱 저장 닉네임 fallback, 타인은 API 또는 writerId 조회 결과
                    val fallbackNickname = if (isOwnReport) currentUserNickname
                        else (detail.writerId?.let { writerNicknamesByWriterId[it] } ?: currentReportWithLocation.report.reporterInfo?.nickname)
                    // 프로필 이미지: 본인 제보일 때만 앱에 저장된 프로필 fallback (타인 제보는 항상 API의 profileImageUrl만 사용)
                    val fallbackProfileUri = if (isOwnReport && !currentUserProfileImageUri.isNullOrBlank())
                        Uri.parse(currentUserProfileImageUri) else null
                    convertDetailToReportCardUi(
                        detail = detail,
                        currentUserLocation = currentUserLocation,
                        fallbackAddress = fallbackAddr,
                        fallbackNickname = fallbackNickname,
                        fallbackProfileImageUri = fallbackProfileUri,
                        isLiked = userLikeStates[reportId] ?: currentReportWithLocation.report.isSaved
                    )
                } else {
                    convertToReportCardUi(currentReportWithLocation, currentUserLocation, currentUserNickname, currentUserProfileImageUri, currentUserMemberId)
                }
            }
            // 배경 오버레이 (전체 화면을 덮어 네비게이션 바까지 어둡게 처리)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { selectedReport = null }
            )
            // 제보 카드 (배경 위에 표시)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ReportCard(
                            report = reportCardUi,
                    modifier = Modifier
                        .fillMaxWidth()
                                .clickable(enabled = false) { }, // 카드 내부 클릭 방지
                        selectedFeedback = userFeedbackSelections[reportCardUi.reportId],
                        isLiked = userLikeStates[reportCardUi.reportId] ?: reportCardUi.isLiked,
                        showLikeButton = !(reportDetail?.let { it.reportId == currentReportWithLocation.report.id && it.writerId != null && currentUserMemberId != null && it.writerId == currentUserMemberId } ?: currentReportWithLocation.report.isUserOwned),
                        onPositiveFeedback = {
                            updateFeedback(reportCardUi.reportId, true)
                            // selectedReport 업데이트
                            selectedReport = updatedSampleReports.find { it.report.id == reportCardUi.reportId }
                        },
                        onNegativeFeedback = {
                            updateFeedback(reportCardUi.reportId, false)
                            // selectedReport 업데이트
                            selectedReport = updatedSampleReports.find { it.report.id == reportCardUi.reportId }
                        },
                        onLikeToggle = {
                            toggleLike(reportCardUi.reportId)
                        }
                    )
                    // 상세 로딩 중 표시
                    if (isLoadingDetail) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                    
                    // 제보 카드 닫기 버튼
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { selectedReport = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "닫기",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
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
