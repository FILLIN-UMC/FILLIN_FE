package com.example.fillin2.map


import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap

@Composable
fun MapContent(
    modifier: Modifier = Modifier,
    onMapReady: (NaverMap) -> Unit = {} // 지도가 준비되었을 때 실행할 콜백
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 1. MapView 객체 생성 및 기억
    val mapView = remember {
        MapView(context).apply {
            id = View.generateViewId()
            // 지도가 준비되면 콜백 실행
            getMapAsync { naverMap ->
                onMapReady(naverMap)
            }
        }
    }
    // 2. 지도의 생명주기를 안드로이드 시스템에 연결
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // 3. Compose 화면에 네이버 지도 View 삽입
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}