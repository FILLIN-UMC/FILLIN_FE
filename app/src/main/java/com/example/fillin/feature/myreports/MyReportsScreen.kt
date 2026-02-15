package com.example.fillin.feature.myreports

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fillin.R
import com.example.fillin.data.ReportStatusManager
import com.example.fillin.data.SharedReportData
import com.example.fillin.data.location.LocationProvider
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.model.mypage.MyReportItem
import com.example.fillin.data.repository.MypageRepository
import com.example.fillin.domain.model.ReportStatus
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.fillin.domain.model.ReportType
import com.example.fillin.feature.home.ReportWithLocation
import com.example.fillin.ui.theme.FILLINTheme

private enum class MyReportsTab { REGISTERED, EXPIRED }

private fun mapApiItemToUi(item: MyReportItem, context: android.content.Context): MyReportUi {
    val reportId = item.reportId ?: 0L
    val (badgeText, badgeBg) = when (item.reportCategory) {
        "DANGER" -> "위험" to Color(0xFFFF6060)
        "INCONVENIENCE" -> "불편" to Color(0xFFF5C72F)
        else -> "발견" to Color(0xFF29C488)
    }
    val addressClean = (item.address ?: "").replace(
        Regex("^[가-힣]+(?:시|도)\\s+[가-힣]+(?:구|시)\\s*"), ""
    ).replace(Regex("\\s*[가-힣]*역\\s*\\d+번\\s*출구\\s*앞"), "").trim()
        .ifEmpty { item.title ?: "" }
    val userLatLng = LocationProvider(context).getLatLng()
    val distance = formatDistance(userLatLng, item.latitude, item.longitude)
    return MyReportUi(
        id = reportId,
        backendReportId = reportId,
        badgeText = badgeText,
        badgeBg = badgeBg,
        viewCount = item.viewCount,
        titleTop = addressClean,
        titleBottom = distance,
        placeName = item.title ?: "",
        imageResId = null,
        imageUrl = item.reportImageUrl
    )
}

private fun formatDistance(userLatLng: Pair<Double, Double>?, lat: Double?, lon: Double?): String {
    if (userLatLng == null || lat == null || lon == null) return ""
    val meters = calculateDistanceMeters(userLatLng.first, userLatLng.second, lat, lon)
    return "가는길 ${meters.toInt()}m"
}

private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

data class MyReportUi(
    val id: Long,
    /** API 삭제 시 사용할 백엔드 reportId. documentId가 숫자 문자열이면 해당 값, 없으면 null */
    val backendReportId: Long? = null,
    val badgeText: String, // "발견" / "불편" 등
    val badgeBg: Color,
    val viewCount: Int, // 좌측 상단 조회수
    val titleTop: String,  // "행복길 122-11"
    val titleBottom: String, // "가는길 255m"
    val placeName: String, // 카드 아래 "붕어빵 가게"
    val imageResId: Int? = null, // 제보 이미지 리소스 ID (로컬)
    val imageUrl: String? = null  // 제보 이미지 URL (Firestore/Storage)
)

@Composable
fun MyReportsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mypageRepository = remember(context) { MypageRepository(context) }
    val isLoggedIn = TokenManager.getBearerToken(context) != null
    var tab by remember { mutableStateOf(MyReportsTab.REGISTERED) }
    var editMode by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MyReportUi?>(null) }

    // 로그인 시 API에서 등록된/사라진 제보 목록 로드 (서버 reportId로 삭제 가능)
    var apiRegistered by remember { mutableStateOf<List<MyReportUi>>(emptyList()) }
    var apiExpired by remember { mutableStateOf<List<MyReportUi>>(emptyList()) }
    var isLoadingApi by remember { mutableStateOf(isLoggedIn) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var locallyPermanentlyDeletedIds by remember { mutableStateOf(setOf<Long>()) }
    // 화면에 들어올 때마다 API 목록 재조회 (새로 등록한 제보가 곧바로 보이도록)
    var compositionCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        compositionCount++
    }
    LaunchedEffect(isLoggedIn, refreshTrigger, compositionCount) {
        if (!isLoggedIn) {
            apiRegistered = emptyList()
            apiExpired = emptyList()
            isLoadingApi = false
            return@LaunchedEffect
        }
        isLoadingApi = true
        val reg = mypageRepository.getMyReports().getOrNull()?.data?.map { mapApiItemToUi(it, context) } ?: emptyList()
        val exp = mypageRepository.getMyReportsExpired().getOrNull()?.data?.map { mapApiItemToUi(it, context) } ?: emptyList()
        apiRegistered = reg
        apiExpired = exp
        isLoadingApi = false
    }

    // SharedReportData에서 제보 데이터 가져오기 (비로그인 또는 API 로드 전 fallback)
    val userReports = remember {
        val reports = SharedReportData.getUserReports()
        reports.map { reportWithLocation ->
            val updatedReport = ReportStatusManager.updateReportStatus(reportWithLocation.report)
            reportWithLocation.copy(report = updatedReport)
        }
    }

    val deletedFromRegistered = remember {
        SharedReportData.loadUserDeletedFromRegisteredIds(context)
    }
    val permanentlyDeleted = remember {
        SharedReportData.loadUserPermanentlyDeletedIds(context)
    }
    
    // ReportWithLocation을 MyReportUi로 변환하는 헬퍼 함수
    val userLatLng = remember(context) { LocationProvider(context).getLatLng() }
    fun convertToMyReportUi(reportWithLocation: ReportWithLocation): MyReportUi {
        val report = reportWithLocation.report
        
        // 타입에 따른 배지 텍스트와 색상
        val (badgeText, badgeBg) = when (report.type) {
            ReportType.DANGER -> "위험" to Color(0xFFFF6060)
            ReportType.INCONVENIENCE -> "불편" to Color(0xFFF5C72F)
            ReportType.DISCOVERY -> "발견" to Color(0xFF29C488)
        }
        
        // 주소에서 시/도/구 제거 및 위치 설명 제거 (실제 주소만 표시)
        val addressWithoutCityDistrict = report.title.replace(
            Regex("^[가-힣]+(?:시|도)\\s+[가-힣]+(?:구|시)\\s*"), 
            ""
        ).replace(Regex("\\s*[가-힣]*역\\s*\\d+번\\s*출구\\s*앞"), "").trim()
        
        val distance = formatDistance(userLatLng, reportWithLocation.latitude, reportWithLocation.longitude)
        val backendReportId = report.documentId?.toLongOrNull()
        return MyReportUi(
            id = report.id,
            backendReportId = backendReportId,
            badgeText = badgeText,
            badgeBg = badgeBg,
            viewCount = report.viewCount,
            titleTop = addressWithoutCityDistrict,
            titleBottom = distance,
            placeName = report.meta, // Report: meta가 제보 제목 (API 매핑 시 item.title → meta)
            imageResId = report.imageResId,
            imageUrl = report.imageUrl
        )
    }
    
    // 등록된 제보: (사라진 제보로 이동한 것 + 완전 삭제한 것) 제외
    val registered = remember(userReports, deletedFromRegistered, permanentlyDeleted) {
        mutableStateListOf(
            *userReports
                .filter { it.report.id !in deletedFromRegistered && it.report.id !in permanentlyDeleted }
                .map { convertToMyReportUi(it) }
                .toTypedArray()
        )
    }

    // 사라진 제보: EXPIRED 상태이거나 사용자가 등록된 제보에서 삭제한 것. 완전 삭제한 것 제외.
    val expired = remember(userReports, deletedFromRegistered, permanentlyDeleted) {
        mutableStateListOf(
            *userReports
                .filter { it.report.id !in permanentlyDeleted }
                .filter { it.report.status == ReportStatus.EXPIRED || it.report.id in deletedFromRegistered }
                .map { convertToMyReportUi(it) }
                .toTypedArray()
        )
    }

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
                .height(56.dp)
                .padding(horizontal = 16.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back_btn),
                contentDescription = "뒤로가기",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart)
                    .clickable { navController.popBackStack() }
            )

            Text(
                text = "나의 제보",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFF111827),
                modifier = Modifier.align(Alignment.Center)
            )

            Text(
                text = if (editMode) "완료" else "편집",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (editMode) Color(0xFFFF6A6A) else Color(0xFF252526),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { editMode = !editMode }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Tabs (등록된 제보 / 사라진 제보)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TabText(
                    text = "등록된 제보",
                    selected = tab == MyReportsTab.REGISTERED,
                    enabled = !editMode,
                    onClick = { if (!editMode) tab = MyReportsTab.REGISTERED }
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TabText(
                    text = "사라진 제보",
                    selected = tab == MyReportsTab.EXPIRED,
                    enabled = !editMode,
                    onClick = { if (!editMode) tab = MyReportsTab.EXPIRED }
                )
            }
        }

        // underline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFE7EBF2))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (tab == MyReportsTab.REGISTERED) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(Color(0xFF4595E5))
            )
        }

        Spacer(Modifier.height(16.dp))

        val displayRegistered = if (isLoggedIn && !isLoadingApi) apiRegistered else registered
        val displayExpired = if (isLoggedIn && !isLoadingApi) apiExpired.filter { it.id !in permanentlyDeleted && it.id !in locallyPermanentlyDeletedIds } else expired
        val list = if (tab == MyReportsTab.REGISTERED) displayRegistered else displayExpired

        if (isLoggedIn && isLoadingApi) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(list, key = { it.id }) { item ->
                MyReportGridItem(
                    item = item,
                    editMode = editMode,
                    onDeleteClick = { pendingDelete = item }
                )
            }
        }
        }

        // Delete confirm dialog
        if (pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = {
                    Text(
                        text = "등록한 제보를 삭제할까요?",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                },
                text = {
                    Text(
                        text = "지도 및 마이페이지에서\n모두 삭제되며 복구할 수 없습니다.",
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = pendingDelete
                            if (target != null) {
                                if (tab == MyReportsTab.REGISTERED) {
                                    if (isLoggedIn) {
                                        scope.launch {
                                            mypageRepository.deleteReport(target.id)
                                                .onSuccess { refreshTrigger++ }
                                                .onFailure {
                                                    Toast.makeText(context, "삭제에 실패했어요.", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } else {
                                        val removed = registered.removeAll { it.id == target.id }
                                        if (removed) {
                                            SharedReportData.addUserDeletedFromRegisteredId(context, target.id)
                                            expired.add(0, target)
                                        }
                                    }
                                } else {
                                    // 사라진 제보에서 완전 삭제
                                    if (isLoggedIn) {
                                        locallyPermanentlyDeletedIds = locallyPermanentlyDeletedIds + target.id
                                        SharedReportData.addUserPermanentlyDeletedId(context, target.id)
                                    } else {
                                        val removed = expired.removeAll { it.id == target.id }
                                        if (removed) {
                                            SharedReportData.addUserPermanentlyDeletedId(context, target.id)
                                            SharedReportData.removeReport(target.id)
                                        }
                                    }
                                }
                            }

                            pendingDelete = null

                            val currentListEmpty = if (tab == MyReportsTab.REGISTERED) displayRegistered.isEmpty() else displayExpired.isEmpty()
                            if (currentListEmpty) editMode = false
                        }
                    ) {
                        Text(
                            text = "삭제",
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(
                            text = "취소",
                            color = Color(0xFF007AFF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun TabText(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = text,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
        color = when {
            !enabled -> Color(0xFFD1D5DB)
            selected -> Color(0xFF4595E5)
            else -> Color(0xFFAAADB3)
        },
        fontSize = 18.sp,
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    )
}

@Composable
private fun MyReportGridItem(
    item: MyReportUi,
    editMode: Boolean,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 카드
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF111827),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (!item.imageUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = item.imageResId ?: R.drawable.ic_report_img),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (editMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF6A6A).copy(alpha = 0.5f))
                    )
                }

                // top dark bar (gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // view count (top-left)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_view),
                        contentDescription = "조회수",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.viewCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }

                // badge
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopEnd)
                        .size(width = 45.dp, height = 28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(item.badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.badgeText,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                // bottom dark bar (gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // bottom text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.titleTop,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        lineHeight = 14.sp
                    )
//                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.titleBottom,
                        color = Color(0xFFE5E7EB),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }

                if (editMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "삭제",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 제보 제목 (이미지 아래 표시)
        Text(
            text = item.placeName,
            color = Color(0xFF252526),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReportsScreenPreview() {
    FILLINTheme {
        MyReportsScreen(navController = rememberNavController())
    }
}
