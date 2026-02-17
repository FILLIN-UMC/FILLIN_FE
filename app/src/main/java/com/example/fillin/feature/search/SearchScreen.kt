package com.example.fillin.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.ui.theme.FILLINTheme
import com.example.fillin.R

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSelectPlace: (PlaceItem) -> Unit,
    onClickHotReport: (HotReportItem) -> Unit,
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
        onClickHotReport = onClickHotReport
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
    onClickHotReport: (HotReportItem) -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    keyboardController?.hide()
                    focusManager.clearFocus()

                    if (hasQuery && !uiState.isSearchCompleted) {
                        onSearch()
                    }
                })
            }
    ) {
        SearchTabs(
            tab = uiState.tab,
            onTabChange = onTabChange
        )

        val handleBackgroundTap = {
            keyboardController?.hide()
            focusManager.clearFocus()
            if (hasQuery && !uiState.isSearchCompleted) {
                onSearch()
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (!uiState.isSearching && !uiState.isSearchCompleted && uiState.searchError == null) {
                when (uiState.tab) {
                    SearchTab.RECENT -> {
                        RecentContent(
                            recent = uiState.recentQueries,
                            onClick = { q ->
                                onQueryChange(q)
                                onSearch()
                            },
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

            if (uiState.isSearching) {
                OverlayLoading()
            } else if (uiState.searchError != null) {
                OverlayError(message = uiState.searchError, onRetry = onSearch)
            } else if (uiState.isSearchCompleted) {
                if (uiState.places.isEmpty()) {
                    OverlayEmpty()
                } else {
                    MapOverlay(results = uiState.places, onClick = onSelectPlace)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(y = searchBarOffsetY)
        ) {
            BottomSearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onClear = onClear,
                onBack = onBack,
                isVisible = transitionState
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
    isVisible: MutableTransitionState<Boolean>? = null
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
                color = colorResource(id = R.color.grey1),
                border = BorderStroke(1.dp, colorResource(id = R.color.grey2)),
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
            color = colorResource(id = R.color.grey1),
            border = BorderStroke(1.dp, colorResource(id = R.color.grey2)),
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

@Composable private fun OverlayLoading() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
@Composable private fun OverlayEmpty() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("검색 결과가 없어요.") } }
@Composable private fun OverlayError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(message); Button(onRetry) { Text("재시도") }
    }
}

@Composable
private fun MapOverlay(results: List<PlaceItem>, onClick: (PlaceItem) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // TODO: 여기에 네이버/카카오/구글 지도 SDK 컴포저블을 넣으시면 됩니다!

        // 일단 화면 전환 테스트를 위한 임시 텍스트입니다.
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🗺️ 지도가 들어갈 자리입니다.", style = MaterialTheme.typography.titleMedium)
            Text("${results.size}개의 결과 마커 표시 예정", color = Color.Gray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    FILLINTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                recentQueries = listOf("위험 요소", "경사로", "주변 놀거리", "팝업", "붕어빵")
            ),
            onBack = {}, onQueryChange = {}, onSearch = {}, onClear = {}, onTabChange = {}, onRemoveRecent = {}, onSelectPlace = {}, onClickHotReport = {}
        )
    }
}