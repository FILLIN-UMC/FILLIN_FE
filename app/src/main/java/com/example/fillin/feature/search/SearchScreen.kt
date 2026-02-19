package com.example.fillin.feature.search

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.model.report.ReportImageDetailData
import com.example.fillin.data.repository.MemberRepository
import com.example.fillin.data.repository.ReportRepository
import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.ui.components.ReportCard
import com.example.fillin.ui.components.ReportCardUi
import com.example.fillin.ui.components.ValidityStatus
import com.example.fillin.ui.map.MapContent
import com.example.fillin.ui.map.PresentLocation
import com.example.fillin.ui.theme.FILLINTheme
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import retrofit2.HttpException
import kotlin.math.*

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSelectPlace: (PlaceItem) -> Unit, // (외부 전달용 - 사용 안하면 무시됨)
    onClickHotReport: (HotReportItem) -> Unit,
    onSearchInCurrentLocation: () -> Unit = {},
    vm: SearchViewModel = run {
        val ctx = LocalContext.current
        viewModel(factory = SearchViewModelFactory(ctx))
    }
) {
    val uiState by vm.uiState.collectAsState()

    SearchScreenContent(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = { vm.setQuery(it) },
        onSearch = { vm.search() },
        onClear = { vm.clearQuery() },
        onTabChange = { vm.switchTab(it) },
        onRemoveRecent = { vm.removeRecent(it) },
        // 🌟 [수정] 여기서 onSelectPlace를 호출하지 않고 내부 상태만 변경하도록 처리할 수도 있지만
        // 아래 Content에서 selectedPlace 상태를 관리하므로 여기선 빈 람다 혹은 로깅
        onSelectPlace = onSelectPlace,
        onClickHotReport = { item ->
            vm.onSelectHotReport(item)
        },
        onSearchInCurrentLocation = onSearchInCurrentLocation
    )
}

@Composable
private fun SearchScreenContent(
    uiState: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onTabChange: (SearchTab) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onSelectPlace: (PlaceItem) -> Unit,
    onClickHotReport: (HotReportItem) -> Unit,
    onSearchInCurrentLocation: () -> Unit
) {
    val context = LocalContext.current
    val hasQuery = uiState.query.isNotBlank()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // === 🌟 [추가] 상세 카드 표시를 위한 상태 관리 ===
    var selectedPlace by remember { mutableStateOf<PlaceItem?>(null) }
    var reportDetail by remember { mutableStateOf<ReportImageDetailData?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var showLoginPrompt by remember { mutableStateOf(false) }

    // Repositories & Preferences (상세 조회용)
    val reportRepository = remember(context) { ReportRepository(context) }
    val memberRepository = remember(context) { MemberRepository(context) }
    val appPreferences = remember { AppPreferences(context) }
    val currentUserMemberId by appPreferences.currentUserMemberIdFlow.collectAsState()
    val currentUserNickname by appPreferences.nicknameFlow.collectAsState()
    val currentUserProfileImageUri by appPreferences.profileImageUriFlow.collectAsState()

    // 🌟 [추가] 마커 클릭 시(selectedPlace 변경 시) API 호출
    LaunchedEffect(selectedPlace) {
        reportDetail = null
        val place = selectedPlace ?: return@LaunchedEffect
        // PlaceItem의 ID가 Long으로 변환 가능해야 백엔드 Report ID임 (HotReport는 가능)
        val docId = place.id.toLongOrNull()

        if (docId == null) {
            // 일반 검색 결과(네이버 지도 등)라면 상세 API 조회 스킵
            return@LaunchedEffect
        }

        isLoadingDetail = true
        val result = reportRepository.getReportDetail(docId)
        isLoadingDetail = false

        result.onSuccess { response ->
            response.data?.let { data ->
                reportDetail = data
            }
        }.onFailure { e ->
            val isUnauthorized = (e as? HttpException)?.code() == 401
            if (isUnauthorized) showLoginPrompt = true
        }
    }

    // 🌟 [추가] 로그인 안내 토스트
    LaunchedEffect(showLoginPrompt) {
        if (showLoginPrompt) {
            Toast.makeText(context, "로그인하면 더 자세한 정보를 볼 수 있어요", Toast.LENGTH_SHORT).show()
            showLoginPrompt = false
        }
    }

    // ... 기존 애니메이션/키보드 로직 ...
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }
    val transition = updateTransition(transitionState, label = "SearchEnter")

    val searchBarOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 400) },
        label = "SearchBarOffset"
    ) { state ->
        if (state) 0.dp else (-120).dp
    }

    val handleBackgroundTap = {
        keyboardController?.hide()
        focusManager.clearFocus()
        if (hasQuery && !uiState.isSearchCompleted) {
            onSearch()
        }
    }

    val showMapView = uiState.isSearchCompleted && uiState.places.isNotEmpty()

    val handleBack = {
        if (selectedPlace != null) {
            selectedPlace = null // 카드 닫기
        } else if (uiState.isSearchCompleted) {
            onClear() // 지도 -> 리스트 복귀
        } else {
            onBack() // 리스트 -> 홈으로 나가기
        }
    }

    val handleClearAction = {
        if (showMapView) {
            onBack()
        } else {
            onClear()
        }
    }

    BackHandler(enabled = uiState.isSearchCompleted || selectedPlace != null) {
        handleBack()
    }

    val isPreview = LocalInspectionMode.current
    var isMapReadyToLoad by remember { mutableStateOf(isPreview) }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            kotlinx.coroutines.delay(400)
            isMapReadyToLoad = true
        }
    }

    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val presentLocation = remember { PresentLocation(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            naverMap?.let { map -> presentLocation.moveMapToCurrentLocation(map) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { handleBackgroundTap() })
            }
    ) {
        // 1. 지도 화면 (Background)
        if (isMapReadyToLoad) {
            MapOverlay(
                results = uiState.places,
                onClick = { place ->
                    // 🌟 [수정] 마커 클릭 시 selectedPlace 상태 업데이트 -> 오버레이 표시
                    selectedPlace = place
                },
                onMapReady = { map -> naverMap = map }
            )
        }

        // 2. 리스트 화면 (최근 검색 / 인기 제보)
        if (!showMapView) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                SearchTabs(tab = uiState.tab, onTabChange = onTabChange)

                Box(modifier = Modifier.weight(1f)) {
                    val listContentPadding = PaddingValues(bottom = 80.dp)

                    if (uiState.isSearching) {
                        OverlayLoading()
                    } else if (uiState.searchError != null) {
                        OverlayError(message = uiState.searchError, onRetry = onSearch)
                    } else if (uiState.isSearchCompleted && uiState.places.isEmpty()) {
                        OverlayEmpty()
                    } else {
                        when (uiState.tab) {
                            SearchTab.RECENT -> {
                                RecentContent(
                                    recent = uiState.recentQueries,
                                    onClick = { q -> onQueryChange(q); onSearch() },
                                    onRemove = onRemoveRecent,
                                    onEmptySpaceClick = handleBackgroundTap,
                                    contentPadding = listContentPadding
                                )
                            }
                            SearchTab.HOT -> {
                                HotReportGridContent(
                                    hotReports = uiState.hotReports,
                                    hotError = uiState.hotError,
                                    isLoading = uiState.isHotLoading,
                                    onClickHotReport = { item ->
                                        // 1. 기존 로직: 상세 정보 로드 및 상태 업데이트
                                        onClickHotReport(item)

                                        // 2. 📍 추가 로직: 카드 클릭 시 해당 위치로 카메라 이동
                                        val lat = item.latitude
                                        val lon = item.longitude

                                        if (lat != 0.0 && lon != 0.0) { // 좌표가 유효한 경우에만 이동
                                            naverMap?.let { map ->
                                                val cameraUpdate = CameraUpdate.scrollTo(LatLng(lat, lon))
                                                    .animate(CameraAnimation.Easing, 600) // 0.6초 동안 부드럽게 이동
                                                map.moveCamera(cameraUpdate)
                                            }
                                        }
                                    },
                                    onEmptySpaceClick = handleBackgroundTap,
                                    contentPadding = listContentPadding
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 현위치 검색 버튼 & 내 위치 버튼
        // (카드가 떠있으면 숨김)
        AnimatedVisibility(
            visible = showMapView && selectedPlace == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SearchInCurrentLocationButton(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = onSearchInCurrentLocation
                )

                LocationButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = {
                        if (naverMap == null) return@LocationButton
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            naverMap?.let { map -> presentLocation.moveMapToCurrentLocation(map) }
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
            }
        }

        // 4. 검색바 (최상단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = searchBarOffsetY)
        ) {
            BottomSearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onClear = handleClearAction,
                onBack = handleBack,
                isVisible = transitionState,
                isSearchCompleted = showMapView
            )
        }

        // 🌟 5. [추가] 제보 상세 카드 오버레이 (HomeScreen 로직 이식)
        selectedPlace?.let { place ->
            // UI 모델 변환 (API 데이터가 있으면 사용, 없으면 PlaceItem 기본 정보 사용)
            val reportCardUi = remember(place, reportDetail, currentUserNickname) {
                val detail = reportDetail
                // API 결과가 있고, 현재 선택된 마커 ID와 일치하면 상세 정보 사용
                if (detail != null && detail.reportId.toString() == place.id) {
                    convertDetailToReportCardUi(
                        detail = detail,
                        fallbackAddress = place.address,
                        currentUserNickname = currentUserNickname,
                        isLiked = false // 검색에선 좋아요 상태 연동 복잡하면 일단 false 처리
                    )
                } else {
                    // API 로딩 전/실패 시 PlaceItem 기반 기본 정보 표시
                    convertPlaceToReportCardUi(place)
                }
            }

            // 배경 어둡게 처리 & 클릭 시 닫기
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { selectedPlace = null }
            )

            // 카드 UI 표시
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ReportCard(
                        report = reportCardUi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}, // 카드 내부 클릭 무시
                        selectedFeedback = null, // 검색에선 피드백 상태 표시 안함 (필요 시 추가 구현)
                        isLiked = reportCardUi.isLiked,
                        showLikeButton = false, // 검색화면에서 좋아요/피드백 기능은 일단 비활성화 (필요하면 추가)
                        onPositiveFeedback = {},
                        onNegativeFeedback = {},
                        onLikeToggle = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 닫기 버튼
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { selectedPlace = null },
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

                // 로딩 중 표시
                if (isLoadingDetail) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

// ... (SearchTabs, RecentContent, BottomSearchBar 등 기존 컴포저블 유지) ...

// 🌟 [추가] 변환 함수들 (HomeScreen 로직 가져옴)

/** PlaceItem(기본 정보) -> ReportCardUi 변환 */
private fun convertPlaceToReportCardUi(place: PlaceItem): ReportCardUi {
    // 카테고리 색상/라벨 매핑
    val (typeLabel, typeColor) = when {
        place.category.contains("위험") -> "위험" to Color(0xFFFF6060)
        place.category.contains("불편") -> "불편" to Color(0xFF4595E5)
        else -> "발견" to Color(0xFF29C488)
    }

    return ReportCardUi(
        reportId = place.id.toLongOrNull() ?: 0L,
        validityStatus = ValidityStatus.VALID,
        imageRes = R.drawable.ic_report_img, // 기본 이미지
        imageUrl = null,
        imageUri = null,
        views = 0,
        typeLabel = typeLabel,
        typeColor = typeColor,
        userName = "정보 없음",
        userBadge = "루키",
        profileImageUrl = null,
        profileImageUri = null,
        title = place.name,
        createdLabel = "",
        address = place.address,
        distance = "",
        okCount = 0,
        dangerCount = 0,
        isLiked = false
    )
}

/** API 상세 정보 -> ReportCardUi 변환 */
private fun convertDetailToReportCardUi(
    detail: ReportImageDetailData,
    fallbackAddress: String,
    currentUserNickname: String,
    isLiked: Boolean
): ReportCardUi {
    val validityStatus = when (detail.validType) {
        "최근에도 확인됐어요" -> ValidityStatus.VALID
        "제보 의견이 나뉘어요" -> ValidityStatus.INTERMEDIATE
        "오래된 제보일 수 있어요" -> ValidityStatus.INVALID
        else -> ValidityStatus.VALID
    }

    val (typeLabel, typeColor) = when (detail.reportCategory) {
        "DANGER" -> "위험" to Color(0xFFFF6060)
        "INCONVENIENCE" -> "불편" to Color(0xFF4595E5)
        else -> "발견" to Color(0xFF29C488)
    }

    val userBadge = when (detail.achievement) {
        "VETERAN" -> "베테랑"
        "MASTER" -> "마스터"
        else -> "루키"
    }

    // 날짜 포맷팅 로직 (간소화)
    val createdLabel = "최근"

    return ReportCardUi(
        reportId = detail.reportId ?: 0L,
        validityStatus = validityStatus,
        imageRes = R.drawable.ic_report_img,
        imageUrl = detail.reportImageUrl,
        imageUri = null,
        views = detail.viewCount,
        typeLabel = typeLabel,
        typeColor = typeColor,
        userName = detail.nickname ?: "사용자",
        userBadge = userBadge,
        profileImageUrl = detail.profileImageUrl,
        profileImageUri = null,
        title = detail.title ?: "",
        createdLabel = createdLabel,
        address = detail.address ?: fallbackAddress,
        distance = "", // 거리 계산 로직은 필요시 추가
        okCount = detail.doneCount,
        dangerCount = detail.nowCount,
        isLiked = isLiked
    )
}

@Composable
private fun SearchTabs(tab: SearchTab, onTabChange: (SearchTab) -> Unit) {
    val selectedIndex = if (tab == SearchTab.RECENT) 0 else 1

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.White,
        edgePadding = 0.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                val currentTab = tabPositions[selectedIndex]
                val indicatorOffset = if (selectedIndex == 1) (-12).dp else 0.dp

                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(currentTab)
                        .offset(x = indicatorOffset)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tab_indicator),
                        contentDescription = null,
                        modifier = Modifier.width(42.dp).height(4.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
    ) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onTabChange(SearchTab.RECENT) },
            text = {
                Text(
                    text = "최근",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedIndex == 0) colorResource(R.color.main) else colorResource(R.color.grey4)
                )
            }
        )
        Tab(
            selected = selectedIndex == 1,
            onClick = { onTabChange(SearchTab.HOT) },
            modifier = Modifier.offset(x = (-12).dp),
            text = {
                Text(
                    text = "인기 제보",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedIndex == 1) colorResource(R.color.main) else colorResource(R.color.grey4)
                )
            }
        )
    }
}

@Composable
private fun RecentContent(
    recent: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onEmptySpaceClick: () -> Unit,
    contentPadding: PaddingValues
) {
    if (recent.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onEmptySpaceClick() }) }
        ) {
            GuideBlock()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onEmptySpaceClick() }) },
            contentPadding = contentPadding
        ) {
            lazyItems(recent) { query ->
                RecentRow(
                    text = query,
                    onClick = { onClick(query) },
                    onRemove = { onRemove(query) }
                )
            }
        }
    }
}

@Composable
private fun RecentRow(text: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                if (text == "위험 요소") {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(colorResource(R.color.grey2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➖", fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6B6B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                    }
                } else {
                    val (icon, bgColor) = when {
                        text.contains("경사로") -> "➖" to Color(0xFFFFD93D)
                        else -> "👀" to Color(0xFF2DBE7A)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, fontSize = 18.sp)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "삭제",
                    tint = colorResource(id = R.color.grey4),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = colorResource(id = R.color.grey2)
        )
    }
}

@Composable
private fun BottomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    isVisible: MutableTransitionState<Boolean>? = null,
    isSearchCompleted: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val transition = updateTransition(
        transitionState = isVisible ?: MutableTransitionState(true),
        label = "SearchBarTransition"
    )

    val buttonOffsetX by transition.animateDp(
        transitionSpec = { tween(400) },
        label = "ButtonOffset"
    ) { state ->
        if (state) 0.dp else 48.dp
    }

    val searchBarPadding by transition.animateDp(
        transitionSpec = { tween(400) },
        label = "SearchBarPadding"
    ) { state ->
        if (state) 60.dp else 0.dp
    }

    val buttonAlpha by transition.animateFloat(
        transitionSpec = { tween(400) },
        label = "ButtonAlpha"
    ) { state ->
        if (state) 1f else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = buttonOffsetX - 4.dp)
                .alpha(buttonAlpha)
                .size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSearchCompleted) Color.White else colorResource(id = R.color.grey1),
                border = if (isSearchCompleted) null else BorderStroke(1.dp, colorResource(id = R.color.grey2)),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "뒤로가기",
                        tint = colorResource(id = R.color.grey3),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = searchBarPadding)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (isSearchCompleted) Color.White else colorResource(id = R.color.grey1),
            border = if (isSearchCompleted) null else BorderStroke(1.dp, colorResource(id = R.color.grey2)),
            shadowElevation = 2.dp
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                singleLine = true,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.grey3)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "내주변 제보 검색",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colorResource(id = R.color.grey3).copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_clear),
                                    contentDescription = "지우기"
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HotReportGridContent(
    hotReports: List<HotReportItem>,
    hotError: String?,
    isLoading: Boolean,
    onClickHotReport: (HotReportItem) -> Unit,
    onEmptySpaceClick: () -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onEmptySpaceClick() }) }
    ) {
        Text(
            text = "내 주변 인기 장소",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = contentPadding.calculateBottomPadding())
        ) {
            gridItems(hotReports) { item ->
                HotReportCard(item, onClick = { onClickHotReport(item) })
            }
        }
    }
}

@Composable
private fun HotReportCard(item: HotReportItem, onClick: () -> Unit) {
    val GreenBadge = Color(0xFF00C795)
    val YellowBadge = Color(0xFFFFD231)

    val (badgeText, badgeColor) = when (item.category) {
        "DANGER" -> "불편" to YellowBadge
        else -> "발견" to GreenBadge
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_eye),
                        contentDescription = "views",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.viewCount.toString(),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = item.address,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "가는길 ${item.distanceMeters}m",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun OverlayResultList(results: List<PlaceItem>, onClick: (PlaceItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        lazyItems(results) { item ->
            PlaceCard(item, onClick = { onClick(item) })
        }
    }
}

@Composable
private fun PlaceCard(item: PlaceItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)
    ) {
        Text(item.name, style = MaterialTheme.typography.titleMedium)
        Text(item.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        if (item.category.isNotBlank()) {
            Text(item.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = Color(0xFFF5F5F5))
    }
}

@Composable
private fun GuideBlock() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("어떤 장소를 찾고 계신가요?", color = Color.Gray)
    }
}

@Composable
private fun OverlayLoading() {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun OverlayEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning_none),
                    contentDescription = null,
                    tint = colorResource(id = R.color.grey4)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "검색 결과가 없어요",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(id = R.color.grey4)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OverlayError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(message)
        Button(onClick = onRetry) { Text("재시도") }
    }
}

@Composable
private fun MapOverlay(
    results: List<PlaceItem>,
    onClick: (PlaceItem) -> Unit,
    onMapReady: (NaverMap) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val markers = remember { mutableListOf<Marker>() }
    val markerIconCache = remember { mutableMapOf<String, OverlayImage>() }

    fun createCircularMarkerIcon(resId: Int, sizeDp: Int = 42, backgroundColor: Int = android.graphics.Color.WHITE): OverlayImage {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, resId)
        val density = context.resources.displayMetrics.density

        val size = min(originalBitmap.width, originalBitmap.height)
        val x = (originalBitmap.width - size) / 2
        val y = (originalBitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)

        val backgroundSizePx = (sizeDp * density).toInt()
        val markerBitmap = Bitmap.createBitmap(backgroundSizePx, backgroundSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(markerBitmap)

        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(0f, 0f, backgroundSizePx.toFloat(), backgroundSizePx.toFloat()), backgroundPaint)

        val imageSizePx = ((sizeDp - 4) * density).toInt()
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5E7EB))
            )
        } else {
            MapContent(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { map ->
                    naverMap = map
                    onMapReady(map)
                }
            )
        }

        LaunchedEffect(naverMap, results) {
            naverMap?.let { map ->
                markers.forEach { it.map = null }
                markers.clear()

                if (results.isNotEmpty()) {
                    results.forEach { item ->
                        val lat = item.y?.toDoubleOrNull()
                        val lon = item.x?.toDoubleOrNull()

                        if (lat != null && lon != null) {
                            val categoryStr = item.category ?: ""
                            val backgroundColor = when {
                                categoryStr.contains("위험") -> android.graphics.Color.parseColor("#FF6060")
                                categoryStr.contains("불편") -> android.graphics.Color.parseColor("#F5C72F")
                                else -> android.graphics.Color.parseColor("#29C488")
                            }
                            val iconRes = when {
                                categoryStr.contains("위험") -> R.drawable.ic_report_img
                                categoryStr.contains("불편") -> R.drawable.ic_report_img_2
                                else -> R.drawable.ic_report_img_3
                            }

                            val cacheKey = "${iconRes}_40_${backgroundColor}"
                            val cachedIcon = markerIconCache[cacheKey] ?: createCircularMarkerIcon(iconRes, 40, backgroundColor).also {
                                markerIconCache[cacheKey] = it
                            }

                            val marker = Marker().apply {
                                position = LatLng(lat, lon)
                                this.map = map
                                this.icon = cachedIcon

                                setOnClickListener {
                                    onClick(item)
                                    true
                                }
                            }
                            markers.add(marker)
                        }
                    }

//                    val firstValidItem = results.firstOrNull {
//                        it.y?.toDoubleOrNull() != null && it.x?.toDoubleOrNull() != null
//                    }
//
//                    firstValidItem?.let { item ->
//                        val cameraUpdate = CameraUpdate.scrollTo(
//                            LatLng(item.y!!.toDouble(), item.x!!.toDouble())
//                        ).animate(CameraAnimation.Easing)
//                        map.moveCamera(cameraUpdate)
//                    }
                }
            }
        }
    }
}

// 🌟 현위치에서 찾기 버튼
@Composable
private fun SearchInCurrentLocationButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "현위치에서 찾기",
                fontSize = 16.sp,
                color = colorResource(id = R.color.main),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 🌟 내 위치 버튼
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

// 🌟 1. 프리뷰: 검색 전
@Preview(showBackground = true, name = "1. 검색 전 (기본 화면)")
@Composable
fun SearchScreenInitialPreview() {
    FILLINTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                recentQueries = listOf("위험 요소", "경사로", "주변 놀거리", "팝업", "붕어빵")
            ),
            onBack = {}, onQueryChange = {}, onSearch = {}, onClear = {}, onTabChange = {}, onRemoveRecent = {}, onSelectPlace = {}, onClickHotReport = {}, onSearchInCurrentLocation = {}
        )
    }
}

// 🌟 2. 프리뷰: 검색 완료 후
@Preview(showBackground = true, name = "2. 검색 후 (지도 화면)")
@Composable
fun SearchScreenMapPreview() {
    FILLINTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                query = "홍대입구",
                isSearchCompleted = true,
                isSearching = false,
                places = listOf(
                    PlaceItem(
                        id = "1",
                        name = "홍대역",
                        address = "서울시 마포구",
                        x = "126.9",
                        y = "37.5",
                        category = "위험"
                    )
                )
            ),
            onBack = {}, onQueryChange = {}, onSearch = {}, onClear = {}, onTabChange = {}, onRemoveRecent = {}, onSelectPlace = {}, onClickHotReport = {}, onSearchInCurrentLocation = {}
        )
    }
}

// 🌟 3. 프리뷰: 검색 결과 없음
@Preview(showBackground = true, name = "3. 검색 결과 없음")
@Composable
fun SearchScreenEmptyPreview() {
    FILLINTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                query = "qqqqqqqqq",
                isSearchCompleted = true,
                isSearching = false,
                places = emptyList()
            ),
            onBack = {}, onQueryChange = {}, onSearch = {}, onClear = {}, onTabChange = {}, onRemoveRecent = {}, onSelectPlace = {}, onClickHotReport = {}, onSearchInCurrentLocation = {}
        )
    }
}

// 🌟 4. 프리뷰: 인기 제보 탭 (데이터 모델 변경 반영)
@Preview(showBackground = true, name = "4. 인기 제보 탭 (Hot)")
@Composable
fun SearchScreenHotPreview() {
    val sampleHotReports = listOf(
        HotReportItem(
            id = 1L,
            title = "성수동 카페거리 입구",
            imageUrl = "dummy_url",
            address = "서울시 성동구 성수동",
            category = "DANGER",
            latitude = 37.5445,
            longitude = 127.0559,
            viewCount = 120,
            distanceMeters = 250
        ),
        HotReportItem(
            id = 2L,
            title = "강남역 11번 출구 앞",
            imageUrl = "dummy_url",
            address = "서울시 강남구 역삼동",
            category = "CAUTION",
            latitude = 37.4980,
            longitude = 127.0276,
            viewCount = 850,
            distanceMeters = 100
        )
    )

    FILLINTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                tab = SearchTab.HOT,
                hotReports = sampleHotReports,
                isSearchCompleted = false,
                isSearching = false
            ),
            onBack = {},
            onQueryChange = {},
            onSearch = {},
            onClear = {},
            onTabChange = {},
            onRemoveRecent = {},
            onSelectPlace = {},
            onClickHotReport = {},
            onSearchInCurrentLocation = {}
        )
    }
}