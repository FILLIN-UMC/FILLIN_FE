package com.example.fillin.feature.report.locationselect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import com.example.fillin.data.kakao.RetrofitClient
import com.example.fillin.feature.report.locationselect.CenterPin
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.fillin.ui.map.MapContent

// 지도에서 장소 선택 화면
@Composable
fun LocationSelectionScreen(
    initialAddress: String,
    onBack: () -> Unit,
    onLocationSet: (String) -> Unit
) {
    // 현재 지도의 중앙 지점 주소를 관리하는 상태
    var centerAddress by remember { mutableStateOf(initialAddress) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // [지도 영역] - 실제 프로젝트의 NaverMap 구현체를 넣으세요.
        MapContent(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { naverMap ->
                // 지도가 움직일 때마다 중앙 좌표의 주소를 역지오코딩하여 centerAddress 업데이트 로직 추가 가능
                // ★ 핵심: 지도가 멈췄을 때 실행되는 리스너
                naverMap.addOnCameraIdleListener {
                    // 현재 지도의 정중앙 좌표 추출
                    val cameraCenter = naverMap.cameraPosition.target
                    // 비동기로 카카오 API 호출
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.kakaoApi.getAddressFromCoord(
                                token = "KakaoAK bace9c32155b5a56bcbb4a74fdd04e9a", // TODO: strings.xml에서 가져오기
                                longitude = cameraCenter.longitude, // x
                                latitude = cameraCenter.latitude    // y
                            )

                            // DTO 그릇에서 데이터 꺼내기
                            val addressDoc = response.documents.firstOrNull()
                            if (addressDoc != null) {
                                // 도로명 주소가 있으면 우선 사용, 없으면 지번 주소 사용
                                centerAddress = addressDoc.road_address?.address_name
                                    ?: addressDoc.address?.address_name
                                            ?: "주소를 찾을 수 없는 지역입니다"
                            }
                        } catch (e: Exception) {
                            centerAddress = "주소 정보를 가져오지 못했습니다"
                        }
                    }
                }
            }
        )

        // [작성하신 CenterPin 레이어]
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 35.dp) // 핀의 끝점이 정중앙에 오도록 미세 조정
        ) {
            CenterPin()
        }

        // 상단 바
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
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "뒤로가기")
                }
                Text("실시간 제보", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
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
                    onClick = { onLocationSet(centerAddress) }, // 최종 선택된 주소를 부모로 전달
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
