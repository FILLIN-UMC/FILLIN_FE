package com.example.fillin2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.fillin2.report.ReportScreen
import com.example.fillin2.ui.theme.Fillin2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 상태바와 네비게이션바 영역까지 화면을 사용하는 설정
        enableEdgeToEdge()

        setContent {
            Fillin2Theme {
                // 2. 배경색을 지정한 Surface 위에 메인 화면을 올립니다.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. 이제 ReportScreen이 검색창, 지도, 제보 메뉴를 모두 관리하는 허브입니다.
                    //
                    ReportScreen()
                }
            }
        }
    }
}