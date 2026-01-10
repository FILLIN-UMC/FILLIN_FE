package com.example.fillin2.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.fillin2.search.SearchViewModel
import com.example.fillin2.kakao.Place

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    // onPlaceClick: (Place) -> Unit
    // 기존의 onPlaceClick 대신 출발/도착 클릭 이벤트를 파라미터로 받습니다.
    onStartClick: (Place) -> Unit,
    onEndClick: (Place) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResult
    val focusRequester = remember { FocusRequester() } // 1. 포커스 요청 객체 생성

    // 2. 화면이 처음 보일 때 포커스를 요청
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        // [1. 하단 검색바 영역] - Scaffold의 bottomBar를 사용합니다.
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // 시스템 네비게이션 바 영역 확보
                        .imePadding()            // ★ 키보드가 올라오면 그 높이만큼 위로 밀림
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    TextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            if (it.isNotEmpty()) viewModel.searchPlaces(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester) // 3. TextField에 연결
                            .heightIn(min = 52.dp), // 높이를 살짝 키워 텍스트 잘림 방지
                        placeholder = {
                            Text(
                                text = "어디로 갈까요?",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = TextUnit.Unspecified // 행간 제한 해제하여 잘림 방지
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F4F7),
                            unfocusedContainerColor = Color(0xFFF2F4F7),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(26.dp), // 조금 더 둥근 디자인
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "지우기")
                                }
                            }
                        },
                        singleLine = true,
                        // 텍스트 수직 정렬 문제를 해결하기 위한 스타일 적용
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = TextUnit.Unspecified
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // [2. 검색 결과 리스트]
        // innerPadding을 적용해야 리스트 마지막 아이템이 검색바에 가려지지 않습니다.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(searchResults) { place ->
                SearchPlaceItem(
                    place = place,
                    onStartClick = { onStartClick(place) }, // 출발 클릭 시 동작 연결
                    onEndClick = { onEndClick(place) } // 도착 클릭 시 동작 연결

                )
            }
        }
    }
}

@Composable
fun SearchPlaceItem(place: Place,
                    onStartClick: (Place) -> Unit, // 추가
                    onEndClick: (Place) -> Unit   // 추가
     ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = place.place_name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        // 이미지의 '출발', '도착' 버튼 디자인
        Row {
            SearchActionButton(text = "출발", isPrimary = false, onClick = { onStartClick(place) } )// 전달받은 함수 호출
            Spacer(Modifier.width(8.dp))
            SearchActionButton(text = "도착", isPrimary = true, onClick = { onEndClick(place) })
        }
    }
}

@Composable
fun SearchActionButton(text: String, isPrimary: Boolean, onClick: () -> Unit) {
    val bgColor = if (isPrimary) Color(0xFF8EBAF3) else Color(0xFFDCEBFF)
    val textColor = if (isPrimary) Color.White else Color(0xFF4090E0)

    Surface(
        onClick = onClick, // Surface에 클릭 추가
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}