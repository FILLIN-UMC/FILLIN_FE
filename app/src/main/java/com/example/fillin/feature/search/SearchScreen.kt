package com.example.fillin.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding() // 상태바 영역 확보
    ) {
        // 1. 상단 탭 영역
        SearchTabs(
            tab = uiState.tab,
            onTabChange = onTabChange
        )

        // 2. 중앙 컨텐츠 (스크롤 영역)
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.tab) {
                SearchTab.RECENT -> {
                    RecentContent(
                        recent = uiState.recentQueries,
                        onClick = { q ->
                            onQueryChange(q)
                            onSearch()
                        },
                        onRemove = onRemoveRecent
                    )
                }
                SearchTab.HOT -> {
                    HotReportGridContent(
                        hotReports = uiState.hotReports,
                        hotError = uiState.hotError,
                        isLoading = uiState.isHotLoading,
                        onClickHotReport = onClickHotReport
                    )
                }
            }

            // 검색 결과 오버레이 (검색 시 화면을 덮음)
            if (isSearchTab && hasQuery) {
                if (uiState.isSearching) OverlayLoading()

                uiState.searchError?.let { msg ->
                    OverlayError(message = msg, onRetry = onSearch)
                }

                if (!uiState.isSearching && uiState.searchError == null) {
                    if (uiState.places.isEmpty()) {
                        OverlayEmpty()
                    } else {
                        OverlayResultList(results = uiState.places, onClick = onSelectPlace)
                    }
                }
            }
        }

        // 3. 하단 검색바 (키보드 대응)
        BottomSearchBar(
            query = uiState.query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onClear = onClear,
            onBack = onBack
        )
    }
}

/* --- 세부 UI 컴포넌트 --- */

@Composable
private fun SearchTabs(tab: SearchTab, onTabChange: (SearchTab) -> Unit) {
    val selectedIndex = if (tab == SearchTab.RECENT) 0 else 1

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.White,
        edgePadding = 0.dp, // 첫 번째 탭의 내장 패딩(16dp) 덕분에 '최근'이 왼쪽에서 16dp 위치에 고정됩니다.
        divider = {},
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                // 선택된 탭의 위치 정보를 가져옵니다.
                val currentTab = tabPositions[selectedIndex]

                // 두 번째 탭이 선택되었을 때, 인디케이터도 왼쪽으로 12dp 이동시킵니다.
                val indicatorOffset = if (selectedIndex == 1) (-12).dp else 0.dp

                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(currentTab)
                        .offset(x = indicatorOffset) // 인디케이터 위치 보정
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
            // 핵심: 음수 오프셋을 주어 내장 패딩을 뚫고 왼쪽으로 12dp 당깁니다. (32dp - 12dp = 20dp)
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
private fun RecentContent(recent: List<String>, onClick: (String) -> Unit, onRemove: (String) -> Unit) {
    if (recent.isEmpty()) {
        GuideBlock()
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                // 왼쪽은 16dp를 유지하고, 오른쪽은 IconButton의 기본 여백을 고려해 4dp로 설정합니다.
                // 이렇게 하면 시각적으로 'X' 아이콘이 오른쪽 끝에서 16dp 떨어진 것처럼 보입니다.
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 아이콘 영역 (위험 요소일 때 겹치기 로직 추가)
            Box(modifier = Modifier.size(44.dp)) { // 겹치는 아이콘을 위해 크기 확보
                if (text == "위험 요소") {
                    // 뒤에 있는 노란색 '경사로' 아이콘
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd) // 오른쪽 정렬
                            .clip(CircleShape)
                            .background(colorResource(R.color.grey2)), // 또는 노란색
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➖", fontSize = 14.sp)
                    }
                    // 앞에 있는 빨간색 '위험' 아이콘
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterStart) // 왼쪽 정렬
                            .clip(CircleShape)
                            .background(Color(0xFFFF6B6B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                    }
                } else {
                    // 일반 단일 아이콘 (경사로, 주변 놀거리 등)
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

            // 2. 텍스트 영역
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 3. X 버튼 영역 (커스텀 이미지 적용 및 간격 조정)
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp) // 터치 영역은 확보하고 크기는 조절
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "삭제",
                    tint = colorResource(id = R.color.grey4),
                    modifier = Modifier.size(20.dp) // 시각적인 아이콘 크기
                )
            }
        }

        // 4. 구분선 (색상: grey2)
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
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 뒤로가기 버튼
        Surface(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = colorResource(id = R.color.grey1), // 내부 색상 grey1
            border = BorderStroke(1.dp, colorResource(id = R.color.grey2)), // 테두리 grey2, 두께 1
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "뒤로가기",
                    tint = colorResource(id = R.color.grey3), // 아이콘도 grey3로 통일
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 2. 검색창
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = colorResource(id = R.color.grey1), // 내부 색상 grey1
            border = BorderStroke(1.dp, colorResource(id = R.color.grey2)), // 테두리 grey2, 두께 1
            shadowElevation = 2.dp
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                singleLine = true,
                // 입력 텍스트 스타일: Bold, 16sp, grey3
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.grey3)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch()
                    keyboardController?.hide()
                }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                // 힌트 텍스트 스타일: Bold, 16sp, grey3
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
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "지우기",
                                    tint = colorResource(id = R.color.grey3),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

/* --- 기타 컴포넌트 (기존 로직 유지) --- */

@Composable
private fun HotReportGridContent(hotReports: List<HotReportItem>, hotError: String?, isLoading: Boolean, onClickHotReport: (HotReportItem) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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

/* --- Preview --- */

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