package com.example.fillin2.report


import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fillin2.R
import com.example.fillin2.ai.GeminiRepository
import com.example.fillin2.ai.GeminiViewModel
import com.example.fillin2.ai.GeminiViewModelFactory
import com.example.fillin2.components.BottomNavBar
import com.example.fillin2.components.TabSpec
import com.example.fillin2.kakao.Place
import com.example.fillin2.kakao.RetrofitClient
import com.example.fillin2.map.MapContent
import com.example.fillin2.map.PresentLocation
import com.example.fillin2.report.locationselect.LocationSelectionScreen
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
                    selectedRoute = selectedRoute,
                    home = homeTab,
                    report = reportTab,
                    my = myTab,
                    onSearchClick = { isSearching = true }, // ★ 클릭 시 검색창 활성화
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
        if (geminiViewModel.aiResult.isNotEmpty() && !isMapPickingMode) {
            ReportRegistrationScreen(
                imageUri = capturedUri,
                initialTitle = geminiViewModel.aiResult, // AI가 분석한 명사 제목
                initialLocation = finalLocation.ifEmpty { "서울시 용산구 행복대로 392" }, // 주소 반영
                onLocationFieldClick = { isMapPickingMode = true }, // 클릭 시 지도 모드로 전환
                onDismiss = { geminiViewModel.clearResult() },
                onRegister = { category, title, location ->
                    // 최종 등록 로직
                    geminiViewModel.clearResult()
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
                onPastReportClick = { showReportMenu = false },
                onRealtimeReportClick = { showReportMenu = false
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
                    // ★ 사진 촬영 완료 즉시 Gemini AI 분석 시작!
                    geminiViewModel.analyzeImage(
                        context = context,
                        uri = uri,
                        apiKey = "AIzaSyCVgJTOLSfJgybdu4pn-Ftw6rbMif3Gzes"
                        // 일단은 테스트 단계이므로 직접 입력 방식 사용.
                        // 보안 강화 방식 (권장): 프로젝트 루트 폴더의 local.properties 파일에 GEMINI_API_KEY=AIza... 형식으로 저장한 뒤,
                        // BuildConfig를 통해 불러오는 방식입니다.
                    )
                }
            )
        }
        // [추가] AI 분석 중일 때 나타나는 로딩 오버레이 (이미지 2번 UI)
        if (geminiViewModel.isAnalyzing) {
            AiLoadingOverlay()
        }

        // [추가] AI 분석 완료 후 결과 화면 (이미지 2번 결과화면)
      /*  if (geminiViewModel.aiResult.isNotEmpty() && !geminiViewModel.isAnalyzing) {
            // 여기에 AI가 지어준 제목(geminiViewModel.aiResult)을 보여주는 UI를 띄우면 됩니다.
            // 예: ReportResultScreen(title = geminiViewModel.aiResult)
            ReportRegistrationScreen(
                imageUri = capturedUri,
                initialTitle = geminiViewModel.aiResult, // AI가 지어준 명사 제목
                initialLocation = currentAddress,      // 현재 위치 주소
                // [추가] 장소 칸을 클릭하면 지도 선택 모드를 켭니다
                onLocationFieldClick = {
                    isMapPickingMode = true
                },
                onDismiss = { geminiViewModel.clearResult() }, // 결과 초기화 함수 필요
                onRegister = { category, finalTitle, finalLocation ->
                    // 최종 데이터를 서버로 전송하는 로직
                    Log.d("FILLIN", "등록: $category / $finalTitle / $finalLocation")
                    geminiViewModel.clearResult()
                }
            )
        }*/

    }
}

// --- 네가 만든 하위 컴포넌트들 (그대로 유지) ---

// --- [추가] 이미지 2번의 로딩 화면 UI 컴포넌트 ---
@Composable
fun AiLoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(300.dp).padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF4090E0) // 이미지 2번의 파란색 톤
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FILLIN",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AI가 제보 사진을\n분석하고 있어요!",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
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
fun LocationButton(onClick: () -> Unit) { // onClick 추가
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
                // ★ 아이콘 대신 내 PNG 파일을 사용함
                painter = painterResource(id = R.drawable.location),
                contentDescription = "Current Location",
                // PNG가 이미 파란색이라면 tint를 Color.Unspecified로 설정해줘
                tint = Color.Unspecified,
                // ★ 여기에 아이콘 크기를 지정하는 modifier를 추가하세요! ★
                modifier = Modifier.size(20.dp)
            )
        }
    }
}