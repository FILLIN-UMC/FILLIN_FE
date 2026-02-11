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
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate

@Composable
fun PastReportLocationScreen(
    initialAddress: String,
    onBack: () -> Unit,
    onLocationSet: (address: String, latitude: Double, longitude: Double) -> Unit
) {
    // 실시간 주소 및 좌표 상태 관리
    var centerAddress by remember { mutableStateOf(initialAddress) }
    var centerLat by remember { mutableStateOf(37.5665) }
    var centerLon by remember { mutableStateOf(126.9780) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    // [추가] 현재 위치를 가져오기 위한 FusedLocationProviderClient
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 1. 지도 영역 (배경)
        MapContent(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { naverMap ->
                // [추가] 지도가 준비되면 즉시 현재 위치를 파악하여 카메라 이동
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)

                            // 1. 지도 카메라를 현재 위치로 이동
                            val cameraUpdate = CameraUpdate.scrollTo(currentLatLng)
                            naverMap.moveCamera(cameraUpdate)

                            // 2. 상태 변수도 현재 위치로 즉시 업데이트 (UI 반응성 향상)
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

        // 중앙 핀 (유저님이 만든 CenterPin 컴포넌트 사용)
        Box(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 35.dp)
        ) {
            CenterPin() // 이전에 만든 파란색 '제보' 핀
        }

        // 2. 상단 헤더 영역 [수정사항 반영: X 아이콘 및 타이틀 변경]
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding() // 상태바 영역까지 흰색 배경이 채워지도록 설정
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽 'X' 닫기 버튼
                IconButton(
                    onClick = onBack
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                }

                // 중앙 타이틀: 지난 상황 제보
                Text(
                    text = "지난 상황 제보", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(48.dp))
            }
        }

        // 중앙 안내 문구
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Text(
                "지도를 움직여 제보 위치를 설정해주세요.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 하단 주소 표시 및 설정 버튼 영역
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = centerAddress, // 실시간으로 바뀐 주소가 여기에 표시됩니다!
                    onValueChange = { centerAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4090E0)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFF5F5F5))
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onLocationSet(centerAddress, centerLat, centerLon) }, // 선택된 주소와 좌표 전달
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4090E0)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("해당 위치로 설정", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
