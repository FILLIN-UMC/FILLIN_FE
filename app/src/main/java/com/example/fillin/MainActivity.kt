package com.example.fillin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.fillin.ui.main.MainScreen
import com.example.fillin.ui.theme.FILLINTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FILLINTheme {
                MainScreen()
            }
        }
    }
}