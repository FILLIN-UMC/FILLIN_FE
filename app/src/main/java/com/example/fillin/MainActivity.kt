package com.example.fillin

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fillin.navigation.NavGraph
import com.example.fillin.navigation.Screen
import com.example.fillin.ui.theme.FILLINTheme
import com.kakao.sdk.common.util.Utility

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("KAKAO_KEYHASH", Utility.getKeyHash(this))

        setContent {
            FILLINTheme {
                // ✅ 앱 시작은 무조건 스플래시(가드)에서 시작
                NavGraph(startDestination = Screen.AfterLoginSplash.route)
            }
        }
    }
}