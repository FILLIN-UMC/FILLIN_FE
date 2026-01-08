package com.example.fillin.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fillin.ui.theme.FILLINTheme

@Composable
fun HomeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home")
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FILLINTheme {
        HomeScreen()
    }
}