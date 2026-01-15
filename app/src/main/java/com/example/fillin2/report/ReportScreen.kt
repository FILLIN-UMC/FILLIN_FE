package com.example.fillin2.report


import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fillin2.BuildConfig
import com.example.fillin2.R
import com.example.fillin2.ai.GeminiRepository
import com.example.fillin2.ai.GeminiViewModel
import com.example.fillin2.ai.GeminiViewModelFactory
import com.example.fillin2.components.BottomNavBar
import com.example.fillin2.components.TabSpec
import com.example.fillin2.db.FirestoreRepository
import com.example.fillin2.kakao.Place
import com.example.fillin2.kakao.RetrofitClient
import com.example.fillin2.map.MapContent
import com.example.fillin2.map.PresentLocation
import com.example.fillin2.report.locationselect.LocationSelectionScreen
import com.example.fillin2.report.pastreport.PastReportLocationScreen
import com.example.fillin2.report.pastreport.PastReportPhotoSelectionScreen
import com.example.fillin2.report.realtime.RealtimeReportScreen
import com.example.fillin2.search.RouteSelectionScreen
import com.example.fillin2.search.SearchScreen
import com.example.fillin2.search.SearchViewModel
import com.naver.maps.map.NaverMap

@Composable
fun ReportScreen(searchViewModel: SearchViewModel = viewModel()) {
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
    val firestoreRepository = remember { FirestoreRepository() }
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
    Box(modifier = Modifier.fillMaxSize()) {
        // [지도 영역]
        MapContent(modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                naverMap = map  // 지도가 준비되면 객체를 저장
            }
        )
        if (!isSearching && !isRouteSelecting && !geminiViewModel.isAnalyzing) {
            // [하단 컨트롤 섹션]
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                // 필터 칩 & 내 위치 버튼 (네가 만든 기존 코드)
                FilterAndLocationRow(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
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
                    modifier = Modifier
                        .padding(horizontal = 16.dp) // ★ 좌우 16dp 마진
                        .padding(bottom = 40.dp),    // ★ 하단 40dp 마진
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

        // [3. 검색 오버레이] - 검색 버튼 클릭 시 전체 화면을 덮음
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
        }

        // [4. 경로 선택 UI 오버레이] - 추가된 부분
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
        }

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
                    isPastFlow = true           // ★ 지난 상황 흐름 시작
                    isPastReportLocationMode = true // 2. 위치 설정 화면 켜기
                                    },
                onRealtimeReportClick = { showReportMenu = false
                    isPastFlow = false          // ★ 실시간 흐름 시작
                    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        // 이미 권한이 있으면 바로 카메라 켬
                        showCamera = true // ★ 카메라 화면 띄우기
                    } else {
                        // 권한이 없으면 요청 팝업 띄우기
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
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
                    geminiViewModel.analyzeImage(
                        context = context,
                        uri = uri,
                        apiKey = BuildConfig.GEMINI_API_KEY // 이제 자동으로 안전한 키를 불러옵니다!
                        // 보안 강화 방식 (권장): 프로젝트 루트 폴더의 local.properties 파일에 GEMINI_API_KEY=AIza... 형식으로 저장한 뒤,
                        // BuildConfig를 통해 불러오는 방식입니다.
                    )
                }
            )
        }
        // [추가] AI 분석 중일 때 나타나는 로딩 오버레이 (이미지 2번 UI)
        if (geminiViewModel.isAnalyzing|| reportViewModel.isUploading) {
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
                    //  하드코딩된 키 대신 BuildConfig.GEMINI_API_KEY를 사용하여 보안을 유지합니다.
                    geminiViewModel.analyzeImage(
                        context = context,
                        uri = uri,
                        apiKey = BuildConfig.GEMINI_API_KEY
                    )
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
                    // TODO: 서버 또는 Firebase에 데이터 저장 로직 수행
                    /*  Log.d("FILLIN_REPORT", "등록 시도: $category, $title, $location")

                    // 등록 후 상태 초기화
                    capturedUri = null
                    geminiViewModel.clearResult()*/

                    // [수정] DB 업로드 로직 연결
                    capturedUri?.let { uri ->
                        reportViewModel.uploadReport(category, title, location, uri)

                    }
                }
            )
        }

    }
}

// --- 네가 만든 하위 컴포넌트들 (그대로 유지) ---

// --- [추가] 이미지 2번의 로딩 화면 UI 컴포넌트 ---
@Composable
fun AiLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6BA4F8),
                                Color(0xFF3178D6)
                            )
                        )
                    )
                    // 전체 높이를 충분히 줘서 "상단 / 중앙 / 하단" 구조 만들기
                    .padding(horizontal = 24.dp)
                    .height(420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /* ---------- 상단 : 로고 ---------- */
                Spacer(modifier = Modifier.height(32.dp))

                Image(
                    painter = painterResource(id = R.drawable.fillin_logo),
                    contentDescription = "FILLIN Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(42.dp),
                    contentScale = ContentScale.Fit
                )

                /* ---------- 중앙 : 텍스트 ---------- */
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "분석이 다 됐어요!\n열심히 작성하고 있어요.",
                    color = Color.White,
                    fontSize = 20.sp,              // 🔥 텍스트 크기 키움
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp             // 줄 간격도 같이 키워서 시원하게
                )

                Spacer(modifier = Modifier.weight(1f))

                /* ---------- 하단 : 프로그레스 ---------- */
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun FilterAndLocationRow(modifier: Modifier = Modifier,
                         onLocationClick: () -> Unit ) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(text = "위험", icon = Icons.Outlined.Warning, color = Color(0xFFE57373))
            CategoryChip(text = "불편", icon = Icons.Outlined.RemoveCircleOutline, color = Color(0xFFFFB74D))
            CategoryChip(text = "발견", icon = Icons.Outlined.Visibility, color = Color(0xFF4DB6AC))
        }
        LocationButton(onClick = onLocationClick)
    }
}

@Composable
fun CategoryChip(text: String, icon: ImageVector, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.height(36.dp).clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
// 현재 위치 버튼
fun LocationButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() } // 클릭 시 전달받은 함수 실행
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                //  아이콘 대신 내 PNG 파일을 사용함
                painter = painterResource(id = R.drawable.location),
                contentDescription = "Current Location",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}