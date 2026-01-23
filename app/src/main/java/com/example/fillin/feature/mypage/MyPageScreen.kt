package com.example.fillin.feature.mypage

import android.R.attr.contentDescription
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fillin.ui.theme.FILLINTheme
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.fillin.R
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.fillin.data.AppPreferences

data class ExpiringReportNotice(
    val daysLeft: Int,
    val summaryText: String, // e.g., "위험 1, 발견 2"
)

const val ROUTE_PROFILE_EDIT = "profile_edit"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_MY_REPORTS = "my_reports"
const val ROUTE_EXPIRING_REPORT_DETAIL = "expiring_report_detail"


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
fun MyPageScreen(
    navController: NavController,
    appPreferences: AppPreferences,
    onHideBottomBar: () -> Unit,
    onShowBottomBar: () -> Unit,
    vm: MyPageViewModel = viewModel(
        factory = MyPageViewModelFactory(appPreferences)
    )
) {
    // 보이는 상태바(시간/배터리 등)를 위해 밝은 배경 + 어두운 아이콘으로 고정
    SetStatusBarColor(color = Color.White, darkIcons = true)

    val state by vm.uiState.collectAsState()
    MyPageContent(
        uiState = state,
        onNavigateProfileEdit = { navController.navigate(ROUTE_PROFILE_EDIT) },
        onNavigateSettings = { navController.navigate(ROUTE_SETTINGS) },
        onNavigateNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
        onNavigateMyReports = { navController.navigate(ROUTE_MY_REPORTS) },
        onNavigateExpiringDetail = { navController.navigate(ROUTE_EXPIRING_REPORT_DETAIL) },
        onHideBottomBar = onHideBottomBar,
        onShowBottomBar = onShowBottomBar
    )
}

@Composable
private fun MyPageContent(
    uiState: MyPageUiState,
    onNavigateProfileEdit: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateMyReports: () -> Unit,
    onNavigateExpiringDetail: () -> Unit,
    onHideBottomBar: () -> Unit,
    onShowBottomBar: () -> Unit
) {
    when (uiState) {
        is MyPageUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is MyPageUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("에러: ${uiState.message}")
            }
        }
        is MyPageUiState.Success -> {
            MyPageSuccess(
                nickname = uiState.summary.nickname,
                totalReports = uiState.summary.totalReports,
                totalViews = uiState.summary.totalViews,
                dangerCount = uiState.summary.danger.first,
                dangerGoal = uiState.summary.danger.second,
                inconvenienceCount = uiState.summary.inconvenience.first,
                inconvenienceGoal = uiState.summary.inconvenience.second,
                discoveryCount = uiState.summary.discoveryCount,
                reports = uiState.reports,
                onNotificationsClick = onNavigateNotifications,
                onNavigateProfileEdit = onNavigateProfileEdit,
                onNavigateSettings = onNavigateSettings,
                onNavigateMyReports = onNavigateMyReports,
                onNavigateExpiringDetail = onNavigateExpiringDetail,
                onHideBottomBar = onHideBottomBar,
                onShowBottomBar = onShowBottomBar,
                expiringNotice = ExpiringReportNotice(daysLeft = 5, summaryText = "위험 1, 발견 2")
            )
        }
    }
}

@Composable
private fun MyPageSuccess(
    nickname: String,
    totalReports: Int,
    totalViews: Int,
    dangerCount: Int,
    dangerGoal: Int,
    inconvenienceCount: Int,
    inconvenienceGoal: Int,
    discoveryCount: Int,
    reports: List<MyReportCard>,
    onNotificationsClick: () -> Unit,
    onNavigateProfileEdit: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateMyReports: () -> Unit,
    onNavigateExpiringDetail: () -> Unit = { },
    onHideBottomBar: () -> Unit,
    onShowBottomBar: () -> Unit,
    expiringNotice: ExpiringReportNotice? = null
) {
    val scrollState = rememberScrollState()
    var lastScrollValue by remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(scrollState.value) {
        if (scrollState.value > lastScrollValue) {
            onHideBottomBar()
        } else if (scrollState.value < lastScrollValue) {
            onShowBottomBar()
        }
        lastScrollValue = scrollState.value
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showExpiringNotice by rememberSaveable(expiringNotice) {
        mutableStateOf(expiringNotice != null)
    }
    var showBadgeTooltip by remember { mutableStateOf(false) }
    var badgeInfoIconCenterXInWindow by remember { mutableStateOf<Float?>(null) }

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.merge(
            TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Top-right actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_fillin_logo),
                    contentDescription = "FILLIN logo",
                    modifier = Modifier.size(width = 58.dp, height = 25.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp)) {
                        CircleIconButton(
                            icon = Icons.Filled.Notifications,
                            onClick = onNotificationsClick
                        )
                    }
                    Spacer(Modifier.width(10.dp))

                    Box {
                        CircleIconButton(
                            icon = Icons.Outlined.Menu,
                            onClick = { menuExpanded = true })
                        val menuShape = RoundedCornerShape(12.dp)

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .widthIn(min = 160.dp)
                                .shadow(elevation = 18.dp, shape = menuShape, clip = false)
                                .clip(menuShape)
                                .background(Color(0xFFE7EBF2))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "프로필 편집",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateProfileEdit()
                                },
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                            )

                            HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "설정",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateSettings()
                                },
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(Modifier.height(16.dp))
                if (expiringNotice != null && showExpiringNotice) {
                    ExpiringReportBanner(
                        daysLeft = expiringNotice.daysLeft,
                        summaryText = expiringNotice.summaryText,
                        onClick = onNavigateExpiringDetail,
                        onDismiss = { showExpiringNotice = false }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Profile row with chips + chevron
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: profile detail */ }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_user_img),
                                contentDescription = "프로필 이미지",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nickname,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF252526),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(6.dp))

                            val badgeText = when {
                                totalReports >= 30 -> "마스터"
                                totalReports >= 10 -> "베테랑"
                                else -> "루키"
                            }

                            TagChip(
                                text = badgeText,
                                border = Color(0xFF4595E5),
                                textColor = Color(0xFF4595E5)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Stats pill
                StatsPill(
                    totalReports = totalReports,
                    totalViews = totalViews,
                    onClick = onNavigateMyReports
                )

                Spacer(Modifier.height(26.dp))

                Spacer(Modifier.height(22.dp))

                // Mission section title with info icon + badge tooltip
                val currentBadge = when {
                    totalReports >= 30 -> "마스터"
                    totalReports >= 10 -> "베테랑"
                    else -> "루키"
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "내가 한 제보",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF252526)
                        )
                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "뱃지 기준 안내",
                            tint = Color(0xFF86878C),
                            modifier = Modifier
                                .size(18.dp)
                                .onGloballyPositioned { coordinates ->
                                    val b = coordinates.boundsInWindow()
                                    badgeInfoIconCenterXInWindow = b.left + (b.width / 2f)
                                }
                                .clickable { showBadgeTooltip = true }
                        )
                    }

                    if (showBadgeTooltip) {
                        androidx.compose.ui.window.Popup(
                            alignment = Alignment.TopStart,
                            onDismissRequest = { showBadgeTooltip = false },
                            properties = androidx.compose.ui.window.PopupProperties(
                                focusable = true
                            )
                        ) {
                            // Tooltip bubble (fixed width like the design)
                            Box(
                                modifier = Modifier
                                    .padding(top = 28.dp) // below the title row
                                    .padding(start = 4.dp)
                            ) {
                                BadgeLevelTooltip(
                                    currentBadge = currentBadge,
                                    iconCenterXInWindow = badgeInfoIconCenterXInWindow,
                                    modifier = Modifier
                                        .width(332.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MissionCardSmall(
                        modifier = Modifier.weight(1f),
                        title = "위험",
                        iconRes = R.drawable.ic_warning,
                        emoji = null,
                        count = dangerCount,
                        leftColor = Color(0xFFFF6060)
                    )
                    MissionCardSmall(
                        modifier = Modifier.weight(1f),
                        title = "불편",
                        iconRes = R.drawable.ic_inconvenience,
                        emoji = null,
                        count = inconvenienceCount,
                        leftColor = Color(0xFF252526)
                    )
                    DiscoveryMissionCard(
                        modifier = Modifier.weight(1f),
                        count = discoveryCount
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "저장한 제보",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF252526)
                )
                Spacer(Modifier.height(12.dp))

                // 2-column grid using rows (pairs)
                val savedScrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(savedScrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    reports.forEach { r ->
                        SavedReportCard(
                            modifier = Modifier.width(170.dp),
                            title = r.title,
                            meta = r.meta,
                            imageResId = r.imageResId,
                            badgeCount = 5
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                // Bottom nav bar is drawn as an overlay on MyPage; add bottom space so the last content isn't hidden behind it.
                Spacer(Modifier.height(320.dp))
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    border: Color = Color.Transparent,
    textColor: Color = Color.Unspecified
) {
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF002BFF),
            Color(0xFF28EDFF),
            Color(0xFF002BFF)
        )
    )

    Surface(
        modifier = Modifier.size(width = 44.dp, height = 24.dp),
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        border = BorderStroke(2.dp, gradient)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                style = TextStyle(brush = gradient)
            )
        }
    }
}

@Composable
private fun DiscoveryMissionCard(
    modifier: Modifier = Modifier,
    count: Int
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier.height(124.dp),
        color = Color(0xFFF7FBFF),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("발견", fontWeight = FontWeight.ExtraBold, color = Color(0xFF252526))
            Spacer(Modifier.height(10.dp))
            Text("👀", fontSize = 22.sp)
            Spacer(Modifier.weight(1f))

            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF555659),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun SavedReportCard(
    modifier: Modifier = Modifier,
    title: String,
    meta: String,
    imageResId: Int?,
    badgeCount: Int
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .aspectRatio(1f),
        shape = shape,
        color = Color(0xFF111827)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 제보 이미지가 있으면 해당 이미지를 사용하고, 없으면 기본 이미지 사용
            Image(
                painter = painterResource(id = imageResId ?: R.drawable.ic_report_img),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // top-left badge
            Surface(
                modifier = Modifier
//                    .padding(10.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(999.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_view),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta,
                    color = Color(0xFFE5E7EB),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Color(0xFFE7EBF2), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFAAADB3),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun StatsPill(
    totalReports: Int,
    totalViews: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFFF7FBFF),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("총 제보", color = Color(0xFF252526), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${totalReports}",
                    color = Color(0xFF4595E5),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(Color(0xFFE7EBF2))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("전체 조회수", color = Color(0xFF252526), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$totalViews",
                    color = Color(0xFF4595E5),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun MissionCardSmall(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int? = null,
    emoji: String? = null,
    count: Int,
    leftColor: Color
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .height(124.dp),
        color = Color(0xFFF7FBFF),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF252526))
            Spacer(Modifier.height(10.dp))
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "$title 아이콘",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(emoji ?: "", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Text("$count", fontWeight = FontWeight.ExtraBold, color = Color(0xFF555659))
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
    modifier: Modifier = Modifier,
    completed: Boolean
) {
    val shape = RoundedCornerShape(20.dp)

    // Soft green gradient with a light yellow highlight like the design
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFDDF7A2), // light yellow-green
            Color(0xFF76D38E), // soft green
            Color(0xFF22B573)  // deeper green
        )
    )

    Box(
        modifier = modifier
            .height(124.dp)
            .clip(shape)
            .background(gradient)
            .padding(10.dp)
    ) {
        // Title
        Text(
            text = "발견",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        )

        // Eyes (emoji approximation)
        Text(
            text = "👀",
            fontSize = 26.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        if (completed) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(30.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(999.dp), clip = false),
                color = Color.White,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "미션완료",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555659),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, meta: String) {
    Surface(
        modifier = Modifier
            .width(170.dp)
            .height(150.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111827)
    ) {
        // Placeholder until images are available
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF111827), Color(0xFF6B7280))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(meta, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyPageScreenPreview() {
    val fakeState = MyPageUiState.Success(
        summary = MyPageSummary(
            nickname = "방태림",
            totalReports = 5,
            totalViews = 50,
            danger = 1 to 5,
            inconvenience = 0 to 5,
            discoveryCount = 1
        ),
        reports = listOf(
            MyReportCard(1, "행복길 2129-11", "가는길 255m"),
            MyReportCard(2, "행복길 2129-11", "가는길 255m"),
            MyReportCard(3, "행복길 2129-11", "가는길 255m")        )
    )

    FILLINTheme {
        MyPageContent(
            uiState = fakeState,
            onNavigateProfileEdit = {},
            onNavigateSettings = {},
            onNavigateNotifications = {},
            onNavigateMyReports = {},
            onNavigateExpiringDetail = {},
            onHideBottomBar = {},
            onShowBottomBar = {}
        )
    }
}

@Preview(showBackground = true, name = "ProfileEdit")
@Composable
private fun ProfileEditScreenPreview() {
    FILLINTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        ProfileEditScreen(
            navController = rememberNavController(),
            appPreferences = AppPreferences(context)
        )
    }
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsScreenPreview() {
    FILLINTheme {
        SettingsScreen(navController = rememberNavController())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    var nickname by rememberSaveable(stateSaver = TextFieldValue.Saver) { 
        mutableStateOf(TextFieldValue(appPreferences.getNickname())) 
    }
    var isNicknameChecked by rememberSaveable { mutableStateOf(false) }
    var isNicknameAvailable by rememberSaveable { mutableStateOf(false) }

    // 프로필 이미지 선택(앨범 시스템 모달)
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val pickProfileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // TODO: 실제로는 ViewModel/서버 업로드 등과 연결
        profileImageUri = uri
    }

    val maxLen = 15
    val count = nickname.text.length.coerceAtMost(maxLen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top bar: left circular back button + centered title
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFFE7EBF2), CircleShape)
                    .clickable { navController.popBackStack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0xFFAAADB3),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "프로필 편집",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                fontSize = 20.sp,
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(22.dp))

        // Profile image + edit icon (Frame 957 스타일)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(100.dp) // Box size == profile image size
                .clickable {
                    pickProfileImageLauncher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            // 사용자 프로필 이미지
            Image(
                painter = painterResource(id = R.drawable.ic_profile_img),
                contentDescription = "사용자 프로필 이미지",
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // 우측 하단 프로필 이미지 편집 아이콘
            Image(
                painter = painterResource(id = R.drawable.ic_profile_img_edit),
                contentDescription = "프로필 이미지 편집",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // 아이콘을 살짝 바깥쪽으로 밀어내서 아바타와 겹치도록
                    .offset(x = -15.dp, y = -15.dp)
                    .size(24.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(34.dp))

        // Nickname label + counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "닉네임",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF555659),
                fontSize = 16.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$count/${maxLen}자",
                color = Color(0xFF555659),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Input + duplicate check button
        val canCheck = nickname.text.isNotBlank()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (nickname.text.isBlank()) {
                        Text(
                            text = "활동할 닉네임을 입력하세요.",
                            color = Color(0xFFAAADB3),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    }

                    BasicTextField(
                        value = nickname,
                        onValueChange = {
                            val trimmed = if (it.text.length > maxLen) it.text.take(maxLen) else it.text
                            nickname = it.copy(text = trimmed)
                            isNicknameChecked = false
                            isNicknameAvailable = false
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            val checkBg = if (canCheck) Color(0xFF4595E5) else Color(0xFFE7EBF2)
            val checkTextColor = if (canCheck) Color.White else Color(0xFFAAADB3)

            Surface(
                modifier = Modifier
                    .height(48.dp)
                    .width(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = canCheck) {
                        // TODO: 실제 API 연결 시 결과에 따라 isNicknameAvailable 값 설정
                        // 임시 로직: 특정 닉네임은 이미 존재한다고 가정
                        val takenNicknames = setOf("가나다")
                        isNicknameChecked = true
                        isNicknameAvailable = nickname.text.trim() !in takenNicknames
                    },
                shape = RoundedCornerShape(14.dp),
                color = checkBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "중복확인",
                        color = checkTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isNicknameChecked) {
            val msg = if (isNicknameAvailable) "사용 가능한 닉네임이에요!" else "이미 존재하는 닉네임이에요."
            val msgColor = if (isNicknameAvailable) Color(0xFF4595E5) else Color(0xFFE54545)

            Text(
                text = msg,
                color = msgColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // Bottom CTA
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(bottom = 18.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(enabled = isNicknameChecked && isNicknameAvailable) {
                    // 닉네임 저장
                    appPreferences.setNickname(nickname.text.trim())
                    navController.popBackStack()
                },
            shape = RoundedCornerShape(999.dp),
            color = if (isNicknameChecked && isNicknameAvailable) Color(0xFF4595E5) else Color(0xFFBFDBFE)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "완료",
                    color = if (isNicknameChecked && isNicknameAvailable) Color.White else Color(0xFFFFFFFF).copy(alpha = 0.7f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    var reportNoti by rememberSaveable { mutableStateOf(true) }
    var feedbackNoti by rememberSaveable { mutableStateOf(true) }
    var serviceNoti by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { navController.popBackStack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back_btn),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "설정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                fontSize = 20.sp,
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Section: 알림 설정
            Text(
                text = "알림 설정",
                    color = Color(0xFFAAADB3),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(16.dp))

            SettingToggleRow(
                title = "제보 알림",
                subtitle = "가까운 위치의 새로운 제보에 대한 정보 알림",
                checked = reportNoti,
                onCheckedChange = { reportNoti = it }
            )
            Spacer(Modifier.height(18.dp))

            SettingToggleRow(
                title = "피드백 알림",
                subtitle = "다른 사용자가 나의 제보에 대한 피드백 반응 시 알림",
                checked = feedbackNoti,
                onCheckedChange = { feedbackNoti = it }
            )

            Spacer(Modifier.height(18.dp))

            SettingToggleRow(
                title = "서비스 알림",
                subtitle = "공지사항이나 이벤트, 업데이트 등 알림",
                checked = serviceNoti,
                onCheckedChange = { serviceNoti = it }
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFFE7EBF2), thickness = 1.dp)
            Spacer(Modifier.height(32.dp))

            // Section: 이용 정보
            Text(
                text = "이용 정보",
                color = Color(0xFFAAADB3),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(16.dp))

            SettingLinkRow(
                title = "필인 지도 서비스 이용약관",
                onClick = { /* TODO */ }
            )
            Spacer(Modifier.height(20.dp))

            SettingLinkRow(
                title = "필인 개인정보처리방침",
                onClick = { /* TODO */ }
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFFE7EBF2), thickness = 1.dp)
            Spacer(Modifier.height(32.dp))

            Text(
                text = "로그아웃",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF252526),
                fontSize = 18.sp,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "탈퇴하기",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF252526),
                fontSize = 18.sp,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF252526),
                fontSize = 18.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFAAADB3),
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4595E5),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE7EBF2)
            )
        )
    }
}

@Composable
private fun SettingLinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
//            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF252526),
            fontSize = 18.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExpiringReportBanner(
    daysLeft: Int,
    summaryText: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
        .clickable(onClick = onClick),
        color = Color(0xFFF7FBFF),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "많은 사람들에게 도움이 된",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )

                Text(
                    text = buildAnnotatedString {
                        append("내 제보가 ")
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF4595E5),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("${daysLeft}일")
                        }
                        append(" 뒤 사라져요")
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF252526),
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = summaryText,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .align(Alignment.CenterVertically)
                    .size(width = 72.dp, height = 72.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_report_img),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 0.dp, y = 0.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_report_img),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 11.dp, y = 27.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_report_img),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 29.dp, y = 10.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "닫기",
                    tint = Color(0xFF555659),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BadgeLevelTooltip(
    currentBadge: String,
    iconCenterXInWindow: Float?,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(14.dp)
    var bubbleLeftXInWindow by remember { mutableStateOf<Float?>(null) }

    Box(modifier = modifier) {
        // Pointer triangle that aims at the info icon (comic balloon style)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .align(Alignment.TopStart)
        ) {
            val triW = 22.dp.toPx()
            val triH = 12.dp.toPx()

            val bubbleLeft = bubbleLeftXInWindow
            val iconX = iconCenterXInWindow

            // Default to center until we have coordinates
            val targetCenterX = if (bubbleLeft != null && iconX != null) {
                (iconX - bubbleLeft).coerceIn(0f, size.width)
            } else {
                size.width / 2f
            }

            val startX = (targetCenterX - triW / 2f).coerceIn(0f, size.width - triW)

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(startX, triH)
                lineTo(startX + triW / 2f, 0f)
                lineTo(startX + triW, triH)
                close()
            }

            // subtle shadow under the pointer
            drawPath(path, color = Color(0x1A000000))
            drawPath(path, color = Color.White)
        }

        // Bubble
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .onGloballyPositioned { coordinates ->
                    bubbleLeftXInWindow = coordinates.boundsInWindow().left
                }
                .shadow(elevation = 20.dp, shape = bubbleShape, clip = false)
                .clip(bubbleShape)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "현재 뱃지 레벨",
                        color = Color(0xFF252526),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                    Spacer(Modifier.width(10.dp))

                    TagChip(
                        text = currentBadge,
                        border = Color(0xFF4595E5),
                        textColor = Color(0xFF4595E5)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "총 제보 개수에 따라 루키(0~9개), 베테랑(10~29개),\n마스터(30개~) 뱃지가 제공돼요.",
                    color = Color(0xFF252526),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

