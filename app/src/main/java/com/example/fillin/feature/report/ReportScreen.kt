package com.example.fillin.feature.report

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fillin.BuildConfig
import com.example.fillin.data.ai.GeminiRepository
import com.example.fillin.data.ai.GeminiViewModel
import com.example.fillin.data.ai.GeminiViewModelFactory
import com.example.fillin.data.kakao.Place
import com.example.fillin.data.kakao.RetrofitClient
import com.example.fillin.feature.report.locationselect.LocationSelectionScreen
import com.example.fillin.feature.report.pastreport.PastReportLocationScreen
import com.example.fillin.feature.report.pastreport.PastReportPhotoSelectionScreen
import com.example.fillin.feature.report.realtime.RealtimeReportScreen
import com.example.fillin.ui.components.AiLoadingOverlay
import com.example.fillin.ui.components.BottomNavBar
import com.example.fillin.ui.components.FilterAndLocationRow
import com.example.fillin.ui.components.TabSpec
import com.example.fillin.ui.map.MapContent
import com.example.fillin.ui.map.PresentLocation
import com.naver.maps.map.NaverMap

@Composable
fun ReportScreen(navController: NavController) {
    // 1. 상태 관리
    var selectedRoute by remember { mutableStateOf("home") }
    var showReportMenu by remember { mutableStateOf(false) } // 제보 메뉴 표시 여부
    var isSearching by remember { mutableStateOf(false) } // 검색 모드 상태
    var isPastFlow by remember { mutableStateOf(false) } // 현재 지난 상황 제보 흐름인지 확인
    // [추가] 카메라 화면 표시 여부 상태
    var showCamera by remember { mutableStateOf(false) }

    // [추가] 출발지/도착지 및 경로 선택 모드 상태
    var startPlace by remember { mutableStateOf<Place?>(null) }
    var endPlace by remember { mutableStateOf<Place?>(null) }
    var isRouteSelecting by remember { mutableStateOf(false) } // 경로 선택 UI 표시 여부
    // 2. 탭 데이터
    val homeTab = TabSpec(route = "home", label = "home", icon = Icons.Filled.Home)
    val myTab = TabSpec(route = "my", label = "my", icon = Icons.Filled.Person)
    val reportTab = TabSpec(route = "report", label = "report", icon = Icons.Outlined.Campaign)

    val context = LocalContext.current
    // Helper 클래스를 기억해둠
    val presentLocation = remember { PresentLocation(context) }
    // ★ 1. naverMap 객체를 저장할 변수 추가!
    var naverMap: NaverMap? by remember { mutableStateOf(null) }

    // --- [추가] AI 연동을 위한 ViewModel 설정 ---
    // 1. 우리가 만든 RetrofitClient에서 서비스 인스턴스를 가져옵니다.
    val apiService = remember { RetrofitClient.geminiApi }
    // 2. 서비스를 레포지토리에 넣어줍니다.
    val geminiRepository = remember { GeminiRepository(apiService) }
    // 3. 레포지토리를 팩토리를 통해 뷰모델에 넣어줍니다.
    val geminiViewModel: GeminiViewModel = viewModel(factory = GeminiViewModelFactory(geminiRepository))

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var currentAddress by remember { mutableStateOf("서울시 용산구 행복대로 392") } // 예시 주소

    // [추가] 위치 선택 모드 상태 관리
    var isMapPickingMode by remember { mutableStateOf(false) }
    var finalLocation by remember { mutableStateOf("") } // 확정된 주소 저장

    var isPastReportLocationMode by remember { mutableStateOf(false) } // 위치 설정 단계
    var isPastReportPhotoStage by remember { mutableStateOf(false) }     // 사진 선택 단계

    // --- [추가: DB 저장 및 상태 관리를 위한 설정] ---
    val firestoreRepository = remember { com.example.fillin.data.db.FirestoreRepository() }
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(firestoreRepository))

    // 업로드 결과 관찰 및 알림 처리
    LaunchedEffect(reportViewModel.uploadStatus) {
        if (reportViewModel.uploadStatus == true) {
            Toast.makeText(context, "제보가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
            // 등록 성공 시 상태 초기화
            capturedUri = null
            geminiViewModel.clearResult()
            reportViewModel.resetStatus()
        } else if (reportViewModel.uploadStatus == false) {
            Toast.makeText(context, "등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            reportViewModel.resetStatus()
        }
    }

    // 1. 권한 요청 도구 (Launcher) 선언
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // 권한 허용 시 지도를 내 위치로 이동시키는 로직 실행 가능
            Log.d("Permission", "위치 권한 허용됨")
        }
    }

    // [추가] 카메라 권한 요청 도구
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 허용 시 카메라 화면 띄움
            showCamera = true
        } else {
            Log.e("Permission", "카메라 권한 거부됨")
            // 필요시 여기서 사용자에게 알림(Toast 등)을 줄 수 있습니다.
        }
    }

    fun startPastFlow() {
        isPastFlow = true
        isPastReportLocationMode = true
        isPastReportPhotoStage = false
        isMapPickingMode = false
        showCamera = false
    }

    fun startRealtimeFlow() {
        isPastFlow = false
        isPastReportLocationMode = false
        isPastReportPhotoStage = false
        isMapPickingMode = false

        val permissionCheckResult =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // MainScreen(하단바)에서 전달된 "제보 플로우" 요청을 소비
    LaunchedEffect(Unit) {
        val flow = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("report_flow")
        if (!flow.isNullOrBlank()) {
            // 한 번 처리 후 제거(재진입 시 반복 실행 방지)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("report_flow")
            when (flow) {
                "past" -> startPastFlow()
                "realtime" -> startRealtimeFlow()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // [지도 영역]
        MapContent(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                naverMap = map  // 지도가 준비되면 객체를 저장
            }
        )
        if (!isSearching && !isRouteSelecting && !geminiViewModel.isAnalyzing) {
            // [하단 컨트롤 섹션]
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                // 필터 칩 & 내 위치 버튼 (네가 만든 기존 코드)
                FilterAndLocationRow(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
                    onLocationClick = {
                        // ★ 네가 질문한 그 코드를 여기에 넣어주는 거야!
                        // 1. 권한 체크 먼저!
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            // 2. 권한이 있으면 지도 이동!
                            naverMap?.let { map ->
                                presentLocation.moveMapToCurrentLocation(map)
                            }
                        } else {   // 3. 권한이 없으면 요청 팝업 띄우기
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )

                // 바텀 네비게이션
                BottomNavBar(
                    selectedRoute = selectedRoute,
                    home = homeTab,
                    report = reportTab,
                    my = myTab,
                    // onSearchClick = { isSearching = true }, // ★ 클릭 시 검색창 활성화
                    onTabClick = { route -> selectedRoute = route },
                    onReportClick = { showReportMenu = !showReportMenu } // 버튼 누르면 메뉴 토글
                )
            }
        }

        /*  // [3. 검색 오버레이] - 검색 버튼 클릭 시 전체 화면을 덮음
        if (isSearching) {
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { isSearching = false }, // 뒤로가기 시 검색 종료
                // 검색 결과에서 '출발'을 눌렀을 때
                onStartClick = { place ->
                    startPlace = place
                    isSearching = false
                    isRouteSelecting = true // 경로 선택 화면으로 전환
                },
                // 검색 결과에서 '도착'을 눌렀을 때
                onEndClick = { place ->
                    endPlace = place
                    isSearching = false
                    isRouteSelecting = true // 경로 선택 화면으로 전환
                }
            )
        }  */

        /* // [4. 경로 선택 UI 오버레이] - 추가된 부분
        if (isRouteSelecting) {
            RouteSelectionScreen(
                startPlace = startPlace,
                endPlace = endPlace,
                onBackClick = {
                    isRouteSelecting = false
                    isSearching = true // 다시 검색 화면으로 복귀
                },
                onSearchFieldClick = { isStartSearch ->
                    // 여기서 다시 검색창을 띄워 출발지나 도착지를 변경하게 할 수 있음
                    isSearching = true
                    isRouteSelecting = false
                }
            )
        } */

        // [1. 제보 등록 화면 오버레이]
        // AI 분석 결과가 있고, 지도 선택 모드가 아닐 때만 띄웁니다.
        if (geminiViewModel.aiResult.isNotEmpty() && !isMapPickingMode && !isPastReportPhotoStage && !isPastReportLocationMode && !isPastFlow) {
            ReportRegistrationScreen(
                topBarTitle = "실시간 제보", // 실시간으로 전달
                imageUri = capturedUri,
                initialTitle = geminiViewModel.aiResult, // AI가 분석한 명사 제목
                initialLocation = finalLocation.ifEmpty { "서울시 용산구 행복대로 392" }, // 주소 반영
                onLocationFieldClick = { isMapPickingMode = true }, // 클릭 시 지도 모드로 전환
                onDismiss = { geminiViewModel.clearResult() },
                onRegister = { category, title, location ->
                    // [수정] DB 업로드 로직 연결
                    capturedUri?.let { uri ->
                        reportViewModel.uploadReport(category, title, location, uri)
                    }
                }
            )
        }

        // [2. 위치 선택 화면 오버레이]
        // 등록 화면에서 장소 칸을 눌러 이 모드가 활성화되었을 때만 띄웁니다.
        if (isMapPickingMode) {
            LocationSelectionScreen(
                initialAddress = finalLocation.ifEmpty { "서울시 용산구 행복대로 392" },
                onBack = { isMapPickingMode = false },
                onLocationSet = { selectedAddress ->
                    finalLocation = selectedAddress // 선택한 주소 저장
                    isMapPickingMode = false // 다시 등록 화면으로 복귀
                }
            )
        }
        // [제보 메뉴 오버레이]
        // 메뉴가 켜졌을 때만 나타남
        if (showReportMenu && !isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)) // 배경을 약간 어둡게
                    .clickable { showReportMenu = false } // 바깥 누르면 닫기
            )

            ReportOptionMenu(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp), // 하단 바 위쪽에 배치
                onPastReportClick = {
                    showReportMenu = false          // 1. 메뉴 팝업 닫기
                    startPastFlow()
                },
                onRealtimeReportClick = {
                    showReportMenu = false
                    startRealtimeFlow()
                }
            )
        }
        // [카메라 화면 오버레이] - 가장 위에 배치
        if (showCamera) {
            // 이전에 만들어드린 RealtimeReportScreen 컴포넌트 호출
            RealtimeReportScreen(
                onDismiss = { showCamera = false },
                onReportSubmit = { uri ->
                    capturedUri = uri
                    showCamera = false
                    //  사진 촬영 완료 즉시 Gemini AI 분석 시작!
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
        //  AI 분석 중일 때 나타나는 로딩 오버레이
        if (geminiViewModel.isAnalyzing || reportViewModel.isUploading) {
            AiLoadingOverlay()
        }

        // [추가] 지난 상황 제보 - 1단계: 위치 설정 화면
        if (isPastReportLocationMode) {
            PastReportLocationScreen(
                initialAddress = finalLocation.ifEmpty { currentAddress },
                onBack = { isPastReportLocationMode = false }, // X 버튼 누르면 닫기
                onLocationSet = { selectedAddress ->
                    finalLocation = selectedAddress          // 주소 저장
                    isPastReportLocationMode = false        // 위치 화면 닫고
                    isPastReportPhotoStage = true           // 다음 단계(사진 선택)로 이동
                }
            )
        }

        // [추가] 지난 상황 제보 - 2단계: 갤러리 사진 추가 화면
        if (isPastReportPhotoStage) {
            PastReportPhotoSelectionScreen(
                onClose = { isPastReportPhotoStage = false },
                onPhotoSelected = { uri ->
                    capturedUri = uri
                    isPastReportPhotoStage = false      // 사진 선택 창을 닫음
                    // 사진 선택되면 바로 AI 분석 시작
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

        // [지난 상황 제보 전용] 분석 완료 후 등록 화면 표시 로직
        if (isPastFlow && isPastReportPhotoStage == false && isPastReportLocationMode == false && capturedUri != null &&
            geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing) {
            ReportRegistrationScreen(
                topBarTitle = "지난 상황 제보", // ★ 타이틀을 "지난 상황 제보"로 설정
                imageUri = capturedUri,
                initialTitle = geminiViewModel.aiResult, // AI가 분석한 제목
                initialLocation = finalLocation,        // ★ 유저가 선택했던 위치 주소 사용
                onLocationFieldClick = {
                    // 필요 시 다시 위치 설정 화면으로 돌아가는 로직 추가 가능
                    isPastReportLocationMode = true
                },
                onDismiss = {
                    // 모든 상태 초기화 및 닫기
                    capturedUri = null
                    geminiViewModel.clearResult()
                },
                onRegister = { category, title, location ->
                    // [수정] DB 업로드 로직 연결
                    capturedUri?.let { uri ->
                        reportViewModel.uploadReport(category, title, location, uri)
                    }
                }
            )
        }
    }
}
