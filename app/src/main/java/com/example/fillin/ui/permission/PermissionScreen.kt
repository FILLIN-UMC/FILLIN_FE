package com.example.fillin.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.sp

@Composable
fun PermissionScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val scope = rememberCoroutineScope()

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                @Suppress("DEPRECATION")
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    fun isAllGranted(): Boolean {
        val basicPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return basicPermissionsGranted && notificationGranted
    }

    fun checkAllPermissionsAndNavigate() {
        scope.launch {
            val allGranted = isAllGranted()
            appPreferences.setPermissionGranted(allGranted)
            if (allGranted) {
                navController.navigate(Screen.AfterLoginSplash.route) {
                    popUpTo(Screen.Permission.route) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                Toast.makeText(context, "권한을 허용해야 서비스를 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkAllPermissionsAndNavigate()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checkAllPermissionsAndNavigate()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val allGranted = result.values.all { it }
        if (!allGranted && permissionsToRequest.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED && !ActivityCompat.shouldShowRequestPermissionRationale(activity!!, it) }) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            settingsLauncher.launch(intent)
            return@rememberLauncherForActivityResult
        }

        if (allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            checkAllPermissionsAndNavigate()
        }
    }

    // ✅ UI 부분만 담당하는 Content 호출
    PermissionContent(
        onConfirmClick = { launcher.launch(permissionsToRequest) }
    )
}

@Composable
fun PermissionContent(
    onConfirmClick: () -> Unit
) {
    val primaryBlue = Color(0xFF4A90E2)
    val logoBlue = Color(0xFF7DB7F0)
    val cardGray = Color(0xFFF0F3F6)
    val iconBlue = Color(0xFF3F8FEA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(100.dp))

        Image(
            painter = painterResource(R.drawable.img_logo_round), // 온보딩에서 쓴 로고 리소스
            contentDescription = "FILLIN Logo",
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "편리한 서비스 제공을 위해\n다음 접근 권한 허용이 필요해요.",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(58.dp))

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

        Spacer(Modifier.height(72.dp))

        Button(
            onClick = onConfirmClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp), // 온보딩 버튼과 통일감 있게 수정 (원래 64dp)
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text(
                text = "확인",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = Color.White
            )
        }

        Spacer(Modifier.height(18.dp))
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
            ) { icon() }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF666666))
            }
        }
    }
}

// --- 프리뷰 추가 ---
@Preview(showBackground = true, name = "권한 안내 화면", device = "spec:width=411dp,height=891dp")
@Composable
fun PermissionScreenPreview() {
    PermissionContent(onConfirmClick = {})
}