package com.example.fillin.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : MainTab("home", "home", Icons.Filled.Home)
    data object Report : MainTab("report", "제보", Icons.Filled.Add)
    data object My : MainTab("my", "my", Icons.Filled.Person)
}
