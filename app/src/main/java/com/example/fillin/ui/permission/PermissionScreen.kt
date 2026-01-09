package com.example.fillin.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun PermissionScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 요청할 권한 목록
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CAMERA)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                @Suppress("DEPRECATION")
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    fun isAllGranted(): Boolean =
        permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }

        scope.launch {
            appPreferences.setPermissionGranted(allGranted)

            if (allGranted) {
                navController.navigate(Screen.AfterLoginSplash.route) {
                    popUpTo(Screen.Permission.route) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                // ✅ 거부 시에는 스플래시로 보내지 말고 여기서 머무르기
                Toast.makeText(context, "권한을 허용해야 서비스를 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 색상 정의 (시안 기준)
    val primaryBlue = Color(0xFF4A90E2)
    val logoBlue = Color(0xFF7DB7F0)
    val cardGray = Color(0xFFF0F3F6)
    val iconBlue = Color(0xFF3F8FEA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(72.dp))

        // 상단 로고
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(logoBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fillin_logo),
                    contentDescription = "FILLIN Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "편리한 서비스 제공을 위해\n다음 접근 권한 허용이 필요해요.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(34.dp))

        PermissionCard(
            icon = { Icon(Icons.Filled.LocationOn, null, tint = Color.White) },
            iconCircleColor = iconBlue,
            title = "위치 정보",
            desc = "지금 있는 위치를 기준으로 정확한 제보를 보여드려요",
            cardColor = cardGray
        )

        Spacer(Modifier.height(14.dp))

        PermissionCard(
            icon = { Icon(Icons.Filled.Camera, null, tint = Color.White) },
            iconCircleColor = iconBlue,
            title = "카메라",
            desc = "현장 사진을 빠르게 제보할 수 있어요",
            cardColor = cardGray
        )

        Spacer(Modifier.height(14.dp))

        PermissionCard(
            icon = { Icon(Icons.Filled.Image, null, tint = Color.White) },
            iconCircleColor = iconBlue,
            title = "사진",
            desc = "이미 찍어둔 사진으로도 제보할 수 있어요",
            cardColor = cardGray
        )

        Spacer(Modifier.height(14.dp))

        PermissionCard(
            icon = { Icon(Icons.Filled.Notifications, null, tint = Color.White) },
            iconCircleColor = iconBlue,
            title = "알림",
            desc = "내 주변 위험 상황을 놓치지 않도록 알려드려요",
            cardColor = cardGray
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (isAllGranted()) {
                    scope.launch {
                        appPreferences.setPermissionGranted(true)
                        navController.navigate(Screen.AfterLoginSplash.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else {
                    launcher.launch(permissionsToRequest)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text(
                text = "확인",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: @Composable () -> Unit,
    iconCircleColor: Color,
    title: String,
    desc: String,
    cardColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconCircleColor),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}
