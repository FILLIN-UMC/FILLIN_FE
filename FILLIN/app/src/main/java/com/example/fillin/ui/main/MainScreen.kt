package com.example.fillin.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text (
            text = " ",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("로그인 · 약관 · 권한이 모두 완료되었습니다.")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    // 저장된 상태 전부 초기화
                    appPreferences.clearAll()

                    // 로그인 화면으로 이동 (뒤로가기 방지)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            }
        ) {
            Text("로그아웃")
        }
    }
}
