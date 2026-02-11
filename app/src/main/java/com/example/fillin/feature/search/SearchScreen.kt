package com.example.fillin.feature.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fillin.R
import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.domain.model.VoteType

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSelectPlace: (PlaceItem) -> Unit,
    onClickHotReport: (HotReportItem) -> Unit = {},
    vm: SearchViewModel = run {
        val ctx = LocalContext.current
        viewModel(factory = SearchViewModelFactory(ctx))
    }
) {
    val ui by vm.uiState.collectAsState()

    val query = ui.query

    // 탭별 로딩/에러
    val isSearching = ui.isSearching
    val isHotLoading = ui.isHotLoading
    val searchErrorMessage = ui.searchError
    val hotErrorMessage = ui.hotError

    // 검색 오버레이는 RECENT 탭에서만
    val isSearchTab = ui.tab == SearchTab.RECENT
    val hasQuery = query.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 상단 헤더
        Header(onBack = onBack)

        Spacer(Modifier.height(8.dp))

        SearchBar(
            query = query,
            onQueryChange = { vm.setQuery(it) },
            onSearch = { vm.search() },
            onClear = { vm.clearQuery() }
        )

        Spacer(Modifier.height(10.dp))

        SearchTabs(
            tab = ui.tab,
            onTabChange = { vm.switchTab(it) }
        )

        Spacer(Modifier.height(10.dp))

        // 탭 아래 콘텐츠 영역
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // (1) 탭 본문
            when (ui.tab) {
                SearchTab.RECENT -> {
                    RecentContent(
                        recent = ui.recentQueries,
                        onClick = { q ->
                            vm.setQuery(q)
                            vm.search()
                        },
                        onRemove = { q -> vm.removeRecent(q) }
                    )
                }

                SearchTab.HOT -> {
                    HotReportGridContent(
                        hotReports = ui.hotReports,
                        hotError = hotErrorMessage,
                        isLoading = isHotLoading,
                        onClickHotReport = { item -> vm.setSelectedHotReport(item.id) }
                    )
                }
            }

            // (2) 검색 오버레이(최근 탭 전용)
            if (isSearchTab) {
                if (isSearching) {
                    OverlayLoading()
                }

                if (searchErrorMessage != null) {
                    OverlayError(
                        message = searchErrorMessage,
                        onRetry = { vm.search() }
                    )
                }

                // 검색 결과 없음
                if (hasQuery && !isSearching && searchErrorMessage == null && ui.places.isEmpty()) {
                    OverlayEmpty()
                }

                // 검색 결과 리스트
                if (hasQuery && !isSearching && searchErrorMessage == null && ui.places.isNotEmpty()) {
                    OverlayResultList(
                        results = ui.places,
                        onClick = onSelectPlace
                    )
                }
            }

            // (3) 인기 제보 상세 바텀 시트
            ui.selectedHotReportId?.let { reportId ->
                val report = ui.hotReports.find { it.id == reportId }
                if (report != null) {
                    HotReportDetailSheet(
                        report = report,
                        userVote = ui.hotUserVotes[reportId],
                        onDismiss = { vm.setSelectedHotReport(null) },
                        onVote = { type -> vm.vote(reportId, type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "back")
        }
        Text(
            text = "검색",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("장소를 검색해보세요") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = "검색",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSearch() }
                .padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SearchTabs(
    tab: SearchTab,
    onTabChange: (SearchTab) -> Unit
) {
    val selectedIndex = if (tab == SearchTab.RECENT) 0 else 1
    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.White,
        contentColor = Color(0xFF2563EB),
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = Color(0xFF2563EB)
                )
            }
        }
    ) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onTabChange(SearchTab.RECENT) },
            text = {
                Text(
                    "최근",
                    color = if (selectedIndex == 0) Color(0xFF2563EB) else Color(0xFF9CA3AF)
                )
            }
        )
        Tab(
            selected = selectedIndex == 1,
            onClick = { onTabChange(SearchTab.HOT) },
            text = {
                Text(
                    "인기 제보",
                    color = if (selectedIndex == 1) Color(0xFF2563EB) else Color(0xFF9CA3AF)
                )
            }
        )
    }
}

@Composable
private fun RecentContent(
    recent: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    // ✅ 검색 기록이 없을 때: "검색 결과 없을때.png" 스타일 안내 문구
    if (recent.isEmpty()) {
        GuideBlock()
        return
    }

    // ✅ 검색 기록이 있을 때: 리스트 형태
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            recent.take(10).forEachIndexed { index, q ->
                RecentRow(
                    text = q,
                    onClick = { onClick(q) },
                    onRemove = { onRemove(q) },
                    showDivider = index < recent.take(10).size - 1
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 색상 있는 원형 아이콘
            RecentIcon(text = text)
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF252526),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "remove",
                    tint = Color(0xFF878B94),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                color = Color(0xFFE7EBF2),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun RecentIcon(text: String) {
    val (iconColor, iconContent) = when {
        text.contains("위험") || text.contains("위험 요소") -> Color(0xFFEB5757) to "⚠"
        text.contains("경사로") -> Color(0xFFF2C94C) to "−"
        else -> Color(0xFF2DBE7A) to "👀"
    }
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(iconColor),
        contentAlignment = Alignment.Center
    ) {
        if (iconContent == "👀") {
            Text(
                text = iconContent,
                fontSize = 16.sp
            )
        } else {
            Text(
                text = iconContent,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * HOT 탭 콘텐츠 (검색 인기 제보.png 형태의 카드 그리드)
 */
@Composable
private fun HotReportGridContent(
    hotReports: List<HotReportItem>,
    hotError: String?,
    isLoading: Boolean,
    onClickHotReport: (HotReportItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "내 주변 인기 장소",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Text(
                    text = "불러오는 중...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return
            }

            hotError != null -> {
                Text(
                    text = hotError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return
            }

            hotReports.isEmpty() -> {
                Text(
                    text = "주변 인기 제보가 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            gridItems(hotReports, key = { it.id }) { item ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HotReportCard(
                        item = item,
                        onClick = { onClickHotReport(item) }
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF252526),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HotReportCard(
    item: HotReportItem,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "report_image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_view),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "5",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            val tagPair = when (item.tag) {
                "발견" -> "발견" to Color(0xFF2DBE7A)
                "불편" -> "불편" to Color(0xFFF2C94C)
                "위험" -> "위험" to Color(0xFFEB5757)
                else -> item.tag to Color(0xFF2D9CDB)
            }
            val tagText = tagPair.first
            val tagColor = tagPair.second

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tagColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "가는길 ${formatDistance(item.distanceMeters)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideBlock() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "어떤 장소를 찾고 계신가요?",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF252526)
            )
            Text(
                "키워드를 입력하면 장소를 찾아드려요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF878B94)
            )
        }
    }
}

/* =========================
   오버레이 (검색 관련만)
   ========================= */

@Composable
private fun OverlayBaseContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun OverlayLoading() {
    OverlayBaseContainer {
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(10.dp))
        Text(
            text = "불러오는 중...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OverlayError(
    message: String,
    onRetry: () -> Unit
) {
    OverlayBaseContainer {
        Spacer(Modifier.height(24.dp))
        Text("검색 실패", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "다시 시도",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onRetry() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun OverlayEmpty() {
    OverlayBaseContainer {
        Spacer(Modifier.height(40.dp))
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF9CA3AF)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "검색 결과가 없어요",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF252526)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "다른 키워드로 다시 검색해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF878B94)
        )
    }
}

@Composable
private fun OverlayResultList(
    results: List<PlaceItem>,
    onClick: (PlaceItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            lazyItems(results, key = { it.id }) { item ->
                PlaceCard(item = item, onClick = { onClick(item) })
            }
        }
    }
}

@Composable
private fun PlaceCard(
    item: PlaceItem,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.category.isNotBlank()) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDistance(meters: Int): String {
    if (meters < 0) return "-"
    return if (meters < 1000) "${meters}m"
    else String.format("%.1fkm", meters / 1000.0)
}

/**
 * 인기 제보 상세 바텀 시트 (인기제보 결과탭.png)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotReportDetailSheet(
    report: HotReportItem,
    userVote: VoteType?,
    onDismiss: () -> Unit,
    onVote: (VoteType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD1D5DB))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "오래된 제보일 수 있어요" 경고
            Text(
                text = "오래된 제보일 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF878B94),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // 이미지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (!report.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = report.imageUrl,
                        contentDescription = "report_image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF3F4F6))
                    )
                }

                // 이미지 오버레이 - 조회수 (좌측 상단)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_view),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                // 이미지 오버레이 - 태그 (우측 상단)
                val tagPair = when (report.tag) {
                    "발견" -> "발견" to Color(0xFF2DBE7A)
                    "불편" -> "불편" to Color(0xFFF2C94C)
                    "위험" -> "위험" to Color(0xFFEB5757)
                    else -> report.tag to Color(0xFF2D9CDB)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(tagPair.second)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tagPair.first,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // 이미지 오버레이 - 사용자 정보 (좌측 하단)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9CA3AF))
                        )
                        Text(
                            text = "조치원 고라니",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF60A5FA))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "루키",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 제목
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF252526)
            )

            // 시간
            Text(
                text = "${report.daysAgo}일 전",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF878B94)
            )

            // 위치 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF252526)
                    )
                    Text(
                        text = "${report.address} 가는 길 ${formatDistance(report.distanceMeters)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF878B94)
                    )
                }
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF878B94)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 질문
            Text(
                text = "지금도 조심해야 할 상황인가요?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF878B94),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // 투표 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VoteButton(
                    text = "이제 괜찮아요",
                    count = report.nowSafeCount,
                    isSelected = userVote == VoteType.NOW_SAFE,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = { onVote(VoteType.NOW_SAFE) }
                )
                VoteButton(
                    text = "아직 위험해요",
                    count = report.stillDangerCount,
                    isSelected = userVote == VoteType.STILL_DANGER,
                    color = Color(0xFFEB5757),
                    modifier = Modifier.weight(1f),
                    onClick = { onVote(VoteType.STILL_DANGER) }
                )
            }
        }
    }
}

@Composable
private fun VoteButton(
    text: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else Color(0xFFE5E7EB)
        ),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White
        )
    ) {
        Text(
            text = "$text $count",
            color = if (isSelected) color else Color(0xFF6B7280),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
