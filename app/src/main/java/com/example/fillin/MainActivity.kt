package com.example.fillin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fillin.ui.main.MainScreen
import com.example.fillin.ui.theme.FILLINTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FILLINTheme {
                MainScreen()
            }
        }
    }
}