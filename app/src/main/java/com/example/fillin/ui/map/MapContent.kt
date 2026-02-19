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
import com.example.fillin.R
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

    var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }
    val activeMarkers = remember { mutableListOf<Marker>() }

    val mapView = remember {
        MapView(context).apply {
            id = View.generateViewId()
            getMapAsync { naverMap ->
                naverMapInstance = naverMap
                val presentLocation = PresentLocation(context)

                presentLocation.moveMapToCurrentLocation(naverMap)

                naverMap.addOnCameraIdleListener {
                    val pos = naverMap.cameraPosition.target

                    val isCityHall = Math.abs(pos.latitude - 37.5666) < 0.001 &&
                            Math.abs(pos.longitude - 126.9784) < 0.001

                    if (isCityHall) {
                        Log.d("MapContent", "기본 위치(시청) 조회 건너뜁니다.")
                    } else {
                        Log.d("MapContent", "카메라 정지: 현재 영역 데이터 조회")
                        fetchMarkersInView(naverMap, viewModel)
                    }
                }

                onMapReady(naverMap)
            }
        }
    }

    viewModel?.let { vm ->
        LaunchedEffect(vm.mapMarkers) {
            val map = naverMapInstance ?: return@LaunchedEffect
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
        }
    }

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

private fun fetchMarkersInView(naverMap: NaverMap, viewModel: ReportViewModel?) {
    viewModel?.let { vm ->
        val bounds = naverMap.contentBounds //
        vm.fetchMapMarkers(
            minLat = bounds.southWest.latitude,
            maxLat = bounds.northEast.latitude,
            minLon = bounds.southWest.longitude,
            maxLon = bounds.northEast.longitude
        )
    }
}