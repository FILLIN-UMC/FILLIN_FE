package com.example.fillin.data.db

import com.google.firebase.Timestamp

// 사용자가 제보를 할 때마다 새로운 객체를 만들어야 하므로 data class를 사용
// val을 사용하여 한 번 생성된 제보 데이터가 전송 중에 변하지 않도록 보장하여 버그를 방지합니다.
data class ReportData(
    val category: String = "",
    val title: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val userId: String = "guest_user" // 추후 로그인 기능 연동 시 변경
)
