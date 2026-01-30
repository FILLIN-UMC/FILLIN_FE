package com.example.fillin.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem

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
                        onClickHotReport = onClickHotReport
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
    TabRow(selectedTabIndex = selectedIndex) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onTabChange(SearchTab.RECENT) },
            text = { Text("최근") }
        )
        Tab(
            selected = selectedIndex == 1,
            onClick = { onTabChange(SearchTab.HOT) },
            text = { Text("인기 제보") }
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
        Text(
            text = "최근 검색",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.take(10).forEach { q ->
                RecentRow(
                    text = q,
                    onClick = { onClick(q) },
                    onRemove = { onRemove(q) }
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "remove")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            gridItems(hotReports, key = { it.id }) { item ->
                HotReportCard(
                    item = item,
                    onClick = { onClickHotReport(item) }
                )
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
                Text(
                    text = "♥ ${item.likeCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            val (tagText, tagColor) = when (item.tag) {
                "발견" -> "발견" to Color(0xFF2DBE7A)
                "불편" -> "불편" to Color(0xFFF2C94C)
                "위험" -> "위험" to Color(0xFFEB5757)
                else -> item.tag to Color(0xFF2D9CDB)
            }

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.daysAgo > 0) {
                Text(
                    text = "${item.daysAgo}일 전",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("어떤 장소를 찾고 계신가요?", style = MaterialTheme.typography.titleMedium)
        Text(
            "키워드를 입력하면 장소를 찾아드려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        Text(
            text = "검색 결과가 없어요",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "다른 키워드로 다시 검색해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
