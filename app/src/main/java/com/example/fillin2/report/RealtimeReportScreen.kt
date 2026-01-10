package com.example.fillin2.report

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
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

    // 촬영된 이미지의 URI를 저장 (null이면 촬영 전, 아니면 촬영 후)
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // CameraX 관련 객체
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (capturedImageUri == null) {
            // 1. 카메라 프리뷰 화면
            CameraPreview(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                imageCapture = imageCapture
            )

            // 상단 바 (X 버튼 및 타이틀)
            CameraTopBar(title = "실시간 제보", onClose = onDismiss)

            // 중앙 가이드 텍스트
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 100.dp)
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
                onClick = {
                    takePhoto(context, imageCapture, cameraExecutor) { uri ->
                        capturedImageUri = uri
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
                    .size(80.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
                    .background(Color.Black, CircleShape) // 이중 원 디자인
                    .background(Color.White, CircleShape)
            ) { }

        } else {
            // 2. 촬영 후 확인 화면
            AsyncImage(
                model = capturedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            CameraTopBar(title = "실시간 제보", onClose = { capturedImageUri = null })

            // 하단 버튼 (다시 찍기 / 등록하기)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { capturedImageUri = null },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5EBEB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("다시 찍기", color = Color.Black)
                }
                Button(
                    onClick = { onReportSubmit(capturedImageUri!!) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4090E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("등록하기", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraTopBar(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(48.dp)) // 균형을 위한 빈 공간
    }
}

@Composable
fun CameraPreview(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
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
            val preview = Preview.Builder().build().also {
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
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
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