package com.example.fillin.ui.map

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.fillin.R // 리소스 아이콘 사용을 위해 필요
import com.example.fillin.feature.report.ReportViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage

@Composable
fun MapContent(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel? = null,
    onMapReady: (NaverMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // NaverMap 인스턴스를 보관할 상태
    var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }
    // 지도에 표시 중인 마커 객체들을 관리할 리스트 (메모리 누수 방지 및 갱신용)
    val activeMarkers = remember { mutableListOf<Marker>() }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { naverMap ->
                naverMapInstance = naverMap

                // viewModel이 전달되었을 때만 카메라 이동 감지 로직 작동
                naverMap.addOnCameraIdleListener {
                    viewModel?.let { vm ->
                        val bounds = naverMap.contentBounds
                        vm.fetchMapMarkers(
                            minLat = bounds.southWest.latitude,
                            maxLat = bounds.northEast.latitude,
                            minLon = bounds.southWest.longitude,
                            maxLon = bounds.northEast.longitude
                        )
                    }
                }
                onMapReady(naverMap)
            }
        }
    }

    // [기능 2] ViewModel의 mapMarkers 상태가 변할 때마다 마커를 새로 그림
    viewModel?.let { vm ->
        LaunchedEffect(vm.mapMarkers) {
            val map = naverMapInstance ?: return@LaunchedEffect
            Log.d("MapContent", "지도 마커 갱신 시작. 데이터 개수: ${vm.mapMarkers.size}")
            activeMarkers.forEach { it.map = null }
            activeMarkers.clear()

            vm.mapMarkers.forEach { markerData ->
                val marker = Marker().apply {
                    position = LatLng(markerData.latitude, markerData.longitude)
                    icon = OverlayImage.fromResource(
                        when (markerData.category) {
                            "DANGER" -> R.drawable.ic_warning_selected
                            "INCONVENIENCE" -> R.drawable.ic_inconvenience_selected
                            else -> R.drawable.ic_discovery
                        }
                    )
                    this.map = map
                }
                activeMarkers.add(marker)
            }
            Log.d("MapContent", "지도 마커 렌더링 완료. 현재 마커 객체 수: ${activeMarkers.size}")
        }
    }

    // 생명주기 연결 코드 (기존과 동일)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    activeMarkers.forEach { it.map = null }
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}