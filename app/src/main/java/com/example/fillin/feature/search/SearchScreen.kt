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
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.ui.theme.FILLINTheme
import com.example.fillin.R
import com.example.fillin.ui.map.MapContent
import com.example.fillin.ui.map.PresentLocation
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import kotlin.math.min
import androidx.activity.compose.BackHandler

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSelectPlace: (PlaceItem) -> Unit,
    onClickHotReport: (HotReportItem) -> Unit,
    onSearchInCurrentLocation: () -> Unit = {}, // 현위치에서 찾기 콜백
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
        onSelectPlace = onSelectPlace,
        onClickHotReport = onClickHotReport,
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
    val isSearchTab = uiState.tab == SearchTab.RECENT
    val hasQuery = uiState.query.isNotBlank()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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

    val handleBack = {
        if (uiState.isSearchCompleted) {
            onClear() // 지도 화면 -> 기본 검색 화면으로
        } else {
            onBack()  // 기본 화면 -> 홈 화면으로
        }
    }

    val handleClearAction = {
        if (uiState.isSearchCompleted) {
            // 지도 화면(검색 완료 상태)에서 X를 누르면 -> 홈 화면으로 완전히 나가기!
            onBack()
        } else {
            // 기본 검색 화면에서 X를 누르면 -> 검색창 텍스트만 지우기!
            onClear()
        }
    }

    BackHandler(enabled = uiState.isSearchCompleted) {
        handleBack()
    }

    // 🌟 프리뷰 환경 감지 및 딜레이 처리
    val isPreview = LocalInspectionMode.current
    var isMapReadyToLoad by remember { mutableStateOf(isPreview) }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            kotlinx.coroutines.delay(400)
            isMapReadyToLoad = true
        }
    }

    val context = LocalContext.current
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val presentLocation = remember { PresentLocation(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            naverMap?.let { map -> presentLocation.moveMapToCurrentLocation(map) }
        }
    }

    // 🌟 1. 최상위 레이아웃
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

        // 🌟 2. 지도 화면 (가장 바닥에 배치)
        if (isMapReadyToLoad) {
            MapOverlay(
                results = uiState.places,
                onClick = onSelectPlace,
                onMapReady = { map -> naverMap = map }
            )
        }

        // 🌟 3. 기본 화면 (최근/인기 탭) - 여기에만 하얀 배경을 줍니다!
        if (!uiState.isSearching && !uiState.isSearchCompleted && uiState.searchError == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                SearchTabs(tab = uiState.tab, onTabChange = onTabChange)
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.tab) {
                        SearchTab.RECENT -> {
                            RecentContent(
                                recent = uiState.recentQueries,
                                onClick = { q -> onQueryChange(q); onSearch() },
                                onRemove = onRemoveRecent,
                                onEmptySpaceClick = handleBackgroundTap
                            )
                        }
                        SearchTab.HOT -> {
                            HotReportGridContent(
                                hotReports = uiState.hotReports,
                                hotError = uiState.hotError,
                                isLoading = uiState.isHotLoading,
                                onClickHotReport = onClickHotReport,
                                onEmptySpaceClick = handleBackgroundTap
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(72.dp)) // 하단 검색바 여백
            }
        }

        // 4. 로딩 / 에러 / 결과 없음 오버레이 (얘네들도 각자 하얀 배경을 가지고 있어야 함)
        if (uiState.isSearching) {
            OverlayLoading()
        } else if (uiState.searchError != null) {
            OverlayError(message = uiState.searchError, onRetry = onSearch)
        } else if (uiState.isSearchCompleted && uiState.places.isEmpty()) {
            OverlayEmpty()
        }

        // 🌟 현위치 검색 버튼 & 내 위치 버튼 나란히 배치 (검색바 16dp 위)
        AnimatedVisibility(
            visible = uiState.isSearchCompleted,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 }),
            modifier = Modifier
                .align(Alignment.BottomCenter) // 중앙 하단 정렬
                .padding(bottom = 84.dp)
        ) {
            // 👇 Row 대신 Box를 사용하여 각 버튼의 위치를 개별적으로 지정합니다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) // 양옆 여백
            ) {
                // 1. 현위치에서 찾기 버튼 (화면 정중앙 고정)
                SearchInCurrentLocationButton(
                    modifier = Modifier.align(Alignment.Center), // 🌟 완벽한 중앙 정렬
                    onClick = onSearchInCurrentLocation
                )

                // 2. 기존 LocationButton (오른쪽 끝 고정)
                LocationButton(
                    modifier = Modifier.align(Alignment.CenterEnd), // 🌟 오른쪽 정렬
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

        // 5. 플로팅 하단 검색바 레이어 (최상단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter) // 화면 바닥 중앙에 고정
                .offset(y = searchBarOffsetY)
        ) {
            BottomSearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onClear = handleClearAction,
                onBack = handleBack,
                isVisible = transitionState,
                isSearchCompleted = uiState.isSearchCompleted // 🌟 상태 전달
            )
        }
    }
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
    onEmptySpaceClick: () -> Unit
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
                .pointerInput(Unit) { detectTapGestures(onTap = { onEmptySpaceClick() }) }
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
    isSearchCompleted: Boolean = false // 🌟 지도 화면 상태값 추가
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
                // 🌟 지도 뷰일 때 흰 배경, 테두리 없음
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
            // 🌟 지도 뷰일 때 흰 배경, 테두리 없음
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
    onEmptySpaceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onEmptySpaceClick() }) }
    ) {
        Text("내 주변 인기 장소", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(hotReports) { item -> HotReportCard(item, onClick = { onClickHotReport(item) }) }
        }
    }
}

@Composable
private fun HotReportCard(item: HotReportItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            AsyncImage(model = item.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Text(item.title, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), color = Color.White, fontWeight = FontWeight.Bold)
        }
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
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Text("검색 결과가 없어요.")
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

    // 마커 아이콘 캐싱
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

                    val firstValidItem = results.firstOrNull {
                        it.y?.toDoubleOrNull() != null && it.x?.toDoubleOrNull() != null
                    }

                    firstValidItem?.let { item ->
                        val cameraUpdate = CameraUpdate.scrollTo(
                            LatLng(item.y!!.toDouble(), item.x!!.toDouble())
                        ).animate(CameraAnimation.Easing)
                        map.moveCamera(cameraUpdate)
                    }
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
        shape = RoundedCornerShape(24.dp), // 타원형
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Box(
            // 아이콘이 빠진 대신 텍스트 양옆 여백을 20dp로 살짝 넓혀주면 훨씬 안정감 있고 예쁩니다
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center // 완벽한 중앙 정렬
        ) {
            Text(
                text = "현위치에서 찾기",
                fontSize = 16.sp, // 텍스트 크기 16sp
                color = colorResource(id = R.color.main), // main 컬러
                fontWeight = FontWeight.Bold // Bold 처리
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

// 🌟 1. 기존 프리뷰: 검색 전 (최근 검색어 / 인기 탭 화면)
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

// 🌟 2. 추가된 프리뷰: 검색 완료 후 (지도 배경 + 위치 버튼들 + 하단 검색바)
@Preview(showBackground = true, name = "2. 검색 후 (지도 화면)")
@Composable
fun SearchScreenMapPreview() {
    FILLINTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                query = "홍대입구",
                isSearchCompleted = true, // 지도 표시 상태
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