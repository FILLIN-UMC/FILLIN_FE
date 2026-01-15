package com.example.fillin2.report

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ReportRegistrationScreen(
    topBarTitle: String, // ★ 타이틀을 외부에서 받도록 추가
    imageUri: Uri?,
    initialTitle: String,
    initialLocation: String,
    onLocationFieldClick: () -> Unit, // [추가] 장소 필드 클릭 시 실행할 함수
    onDismiss: () -> Unit,
    onRegister: (String, String, String) -> Unit // 카테고리, 제목, 장소 전달
) {
    var selectedCategory by remember { mutableStateOf("위험") }
    var title by remember { mutableStateOf(initialTitle) }
    var location by remember { mutableStateOf(initialLocation) }
    // 공백을 제외한 글자 수를 계산하는 변수를 미리 선언합니다.
    val pureCharCount = title.count { !it.isWhitespace() }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // 상단 바
            Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
                Text(
                    text = topBarTitle, // ★ 하드코딩된 "실시간 제보"를 변수로 변경!
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 제보 사진 미리보기
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                      //  .fillMaxWidth()
                        .size(260.dp) // 시안의 비율에 맞춰 정사각형에 가깝게 조정
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))

            // 1. 카테고리 선택
            Text("카테고리", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelectChip("위험", Icons.Default.Warning, Color(0xFFE57373), selectedCategory == "위험") { selectedCategory = "위험" }
                CategorySelectChip("불편", Icons.Default.RemoveCircleOutline, Color(0xFFFFB74D), selectedCategory == "불편") { selectedCategory = "불편" }
                CategorySelectChip("발견", Icons.Default.Visibility, Color(0xFF4DB6AC), selectedCategory == "발견") { selectedCategory = "발견" }
            }

            Spacer(Modifier.height(32.dp))

            // 2. 제목 (AI 자동 작성)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text("제목", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp)) // '제목'과 숫자 사이의 간격
                Text("$pureCharCount/15자", style = MaterialTheme.typography.labelMedium, color = Color.Gray) // 글자 수
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                // [반영] 텍스트를 지웠을 때 나타날 힌트 문구
                placeholder = {
                    Text("제목을 작성해 주세요.", color = Color.Gray)},
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(onClick = { title = "" }) { // [반영] 클릭 시 지우기
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF4090E0)
                )
            )

            Spacer(Modifier.height(16.dp))

            // 3. 장소 (현재 위치 자동 입력)
            Text("장소", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onLocationFieldClick() } // 이제 클릭이 확실히 작동합니다!
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { }, // 읽기 전용
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // ★ 중요: 입력을 꺼야 Box의 클릭이 우선권을 가집니다.
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4090E0)) },
                    placeholder = { Text("장소를 선택해 주세요.") },
                    trailingIcon = {
                        if (location.isNotEmpty()) {
                            IconButton(onClick = { location = "" }) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    // 비활성화 상태에서도 글자가 잘 보이도록 색상 고정
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledPlaceholderColor = Color.Gray,
                        disabledContainerColor = Color(0xFFF5F5F5),
                        disabledBorderColor = Color.Transparent,
                        disabledLeadingIconColor = Color(0xFF4090E0)
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            // 안내 문구 및 등록 버튼
            Text(
                text = "ⓘ AI가 분석한 내용은 사실과 다를 수 있어요",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Button(
                onClick = { onRegister(selectedCategory, title, location) },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4090E0)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("등록하기", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(35.dp))
        }
    }
}

@Composable
fun CategorySelectChip(text: String, icon: ImageVector, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else Color(0xFFF5F5F5),
        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSelected) Color.White else color)
            Spacer(Modifier.width(4.dp))
            Text(text, color = if (isSelected) Color.White else Color.Black, style = MaterialTheme.typography.labelLarge)
        }
    }
}