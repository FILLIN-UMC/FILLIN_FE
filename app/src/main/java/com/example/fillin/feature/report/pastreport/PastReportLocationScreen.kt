package com.example.fillin.feature.report.pastreport

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.fillin.BuildConfig
import com.example.fillin.data.kakao.RetrofitClient
import com.example.fillin.feature.report.locationselect.CenterPin
import com.example.fillin.ui.map.MapContent
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode // [추가] 프리뷰 체크용
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.fillin.R
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate

@Composable
fun PastReportLocationScreen(
    initialAddress: String,
    onBack: () -> Unit,
    onLocationSet: (address: String, latitude: Double, longitude: Double) -> Unit
) {
    // 프리뷰 모드인지 확인하여 Naver Map SDK 로드 시 발생하는 VerifyError 방지
    val isPreview = LocalInspectionMode.current

    // 실시간 주소 및 좌표 상태 관리
    var centerAddress by remember { mutableStateOf(initialAddress) }
    var centerLat by remember { mutableStateOf(37.5665) }
    var centerLon by remember { mutableStateOf(126.9780) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 현재 위치를 가져오기 위한 FusedLocationProviderClient
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 1. 지도 영역 (배경)
        if (isPreview) {
            // 프리뷰 환경에서는 실제 지도를 로드하지 않고 더미 배경을 표시
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text("지도 영역 (실제 기기에서 작동)", color = Color.Gray)
            }
        } else {
            // 실제 앱 실행 시에만 MapContent 로드
            MapContent(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { naverMap ->
                    // 지도가 준비되면 즉시 현재 위치를 파악하여 카메라 이동
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            location?.let {
                                val currentLatLng = LatLng(it.latitude, it.longitude)
                                val cameraUpdate = CameraUpdate.scrollTo(currentLatLng)
                                naverMap.moveCamera(cameraUpdate)

                                centerLat = it.latitude
                                centerLon = it.longitude
                            }
                        }
                    }
                    // 지도가 멈추면 중앙 좌표의 주소를 가져옴 (역지오코딩)
                    naverMap.addOnCameraIdleListener {
                        val cameraCenter = naverMap.cameraPosition.target
                        centerLat = cameraCenter.latitude
                        centerLon = cameraCenter.longitude
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.kakaoApi.getAddressFromCoord(
                                    token = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
                                    longitude = cameraCenter.longitude,
                                    latitude = cameraCenter.latitude
                                )
                                val addressDoc = response.documents.firstOrNull()
                                centerAddress = addressDoc?.road_address?.address_name
                                    ?: addressDoc?.address?.address_name
                                            ?: "주소를 찾을 수 없는 지역입니다"
                            } catch (e: Exception) {
                                centerAddress = "주소 로드 실패"
                            }
                        }
                    }
                }
            )
        }

        // 중앙 핀 (MapContent 외부에 위치하므로 프리뷰에서도 보임)
        Box(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 35.dp)
        ) {
            CenterPin()
        }

        // 2. 상단 헤더 영역 (수정본)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    // [수정된 부분] 기존 Icons.Default.Close 대신 이미지 리소스 사용
                    Icon(
                        painter = painterResource(id = R.drawable.btn_close), // TODO: 실제 이미지 리소스 ID로 변경 (예: R.drawable.img_close_btn)
                        contentDescription = "닫기",
                        tint = Color.Unspecified // 이미지 본연의 색상을 유지하기 위해 tint를 끕니다.
                    )
                }
                Text(
                    text = "지난 상황 제보",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium // 텍스트 스타일 통일감 부여
                )
                // 아이콘 크기(24dp) + 패딩 등을 고려하여 균형을 맞추기 위한 Spacer
                Spacer(Modifier.size(48.dp))
            }
        }

        // 3. 하단 통합 영역 (안내 문구 + 주소 카드)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // 전체 묶음을 화면 하단에 정렬
                .padding(bottom = 32.dp),      // 화면 최하단과의 여백
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // [A] 안내 문구
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Text(
                    "지도를 움직여 제보 위치를 설정해주세요.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium, // Medium 두께
                        fontSize = 16.sp,               // 16sp 크기
                        color = colorResource(R.color.grey5)      // Grey5 색상 (프로젝트 변수가 있다면 대체 가능)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) // 안내 문구와 카드 사이의 간격

            // [B] 하단 주소 카드 영역
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp), // 카드 좌우 여백
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 주소 표시바
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        color = Color(0xFFF8FAFF),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF4090E0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = centerAddress,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Start
                                ),
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 설정 버튼
                    Button(
                        onClick = { onLocationSet(centerAddress, centerLat, centerLon) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(53.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4090E0)),
                        shape = RoundedCornerShape(30.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            "해당 위치로 설정",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PastReportLocationScreenPreview() {
    MaterialTheme {
        PastReportLocationScreen(
            initialAddress = "서울특별시 중구 세종대로 110",
            onBack = {},
            onLocationSet = { _, _, _ -> }
        )
    }
}