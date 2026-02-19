package com.example.fillin.feature.report.realtime

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import com.example.fillin.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun RealtimeReportScreen(
    onDismiss: () -> Unit,
    onReportSubmit: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    RealtimeReportContent(
        capturedImageUri = capturedImageUri,
        previewView = previewView,
        lifecycleOwner = lifecycleOwner,
        imageCapture = imageCapture,
        onDismiss = onDismiss,
        onTakePhoto = {
            takePhoto(context, imageCapture, cameraExecutor) { uri ->
                capturedImageUri = uri
            }
        },
        onRetake = { capturedImageUri = null },
        onReportSubmit = { onReportSubmit(it) }
    )
}

@Composable
fun RealtimeReportContent(
    capturedImageUri: Uri?,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onRetake: () -> Unit,
    onReportSubmit: (Uri) -> Unit
) {
    val isPreviewMode = LocalInspectionMode.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (capturedImageUri == null) {
            // 1. 카메라 프리뷰 영역
            if (isPreviewMode) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("카메라 프리뷰 영역 (실제 기기에서 작동)", color = Color.Gray)
                }
            } else {
                CameraPreview(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    imageCapture = imageCapture
                )
            }

            // --- [수정된 상단 바] 흰색 배경 및 그림자 적용 ---
            CameraTopBar(title = "실시간 제보", onClose = onDismiss)

            // 중앙 가이드 텍스트
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 230.dp)
                    .background(Color(0x994090E0), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "제보할 사진을 가운데에 맞춰 촬영해주세요",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 하단 촬영 버튼
            IconButton(
                onClick = onTakePhoto,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
                    .size(80.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
                    .background(Color.Black, CircleShape)
                    .background(Color.White, CircleShape)
            ) { }

        } else {
            // 2. 촬영 후 확인 화면
            AsyncImage(
                model = capturedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            // 촬영 후 화면에서도 상단 바 유지
            CameraTopBar(title = "실시간 제보", onClose = onRetake)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f).height(53.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5EBEB)),
                    shape = RoundedCornerShape(40.dp)
                ) {
                    Text("다시 찍기", color = Color.Black)
                }
                Button(
                    onClick = { onReportSubmit(capturedImageUri) },
                    modifier = Modifier.weight(1f).height(53.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4090E0)),
                    shape = RoundedCornerShape(40.dp)
                ) {
                    Text("등록하기", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraTopBar(title: String, onClose: () -> Unit) {
    // 요청하신 대로 흰색 박스(Surface)와 그림자(Elevation)를 추가했습니다.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.btn_close),
                    contentDescription = "닫기",
                    tint = Color.Unspecified // 검정색 아이콘 본연의 색 유지
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Color.Black, // 배경이 흰색이므로 텍스트는 검정색
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

// --- [이전과 동일한 CameraPreview 및 takePhoto 함수] ---
@Composable
fun CameraPreview(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = CameraXPreview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (Uri) -> Unit
) {
    val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onImageCaptured(Uri.fromFile(photoFile))
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Photo capture failed", exception)
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RealtimeReportScreenPreview() {
    MaterialTheme {
        RealtimeReportContent(
            capturedImageUri = null,
            previewView = PreviewView(LocalContext.current),
            lifecycleOwner = LocalLifecycleOwner.current,
            imageCapture = ImageCapture.Builder().build(),
            onDismiss = {},
            onTakePhoto = {},
            onRetake = {},
            onReportSubmit = {}
        )
    }
}