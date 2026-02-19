package com.example.fillin.ui.map

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapContent(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel? = null,
    onMapReady: (NaverMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. MapManager를 통해 지도를 가져오거나 생성 (캐싱)
    val mapView = remember { MapManager.getMapView(context) }

    // NaverMap 인스턴스와 마커 리스트 상태 관리
    var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }
    val activeMarkers = remember { mutableListOf<Marker>() }

    LaunchedEffect(Unit) {
        viewModel?.clearMarkers() // ViewModel에 이 함수를 만들어야 합니다 (아래 참고)
    }

    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // 2. [핵심] 지도가 준비되었을 때 실행될 로직을 LaunchedEffect로 분리
    // 이렇게 하면 viewModel 접근 시 오류가 나지 않습니다.
    LaunchedEffect(mapView) {
        mapView.getMapAsync { naverMap ->
            naverMapInstance = naverMap
            val presentLocation = PresentLocation(context)

            // 초기 위치 이동
            presentLocation.moveMapToCurrentLocation(naverMap)

            // 카메라 정지 시 리스너 설정
            naverMap.addOnCameraIdleListener {
                // 📍 [핵심] 이전 예약된 조회가 있다면 취소합니다.
                debounceJob?.cancel()

                // 📍 300ms(0.3초) 동안 카메라가 조용하면 그때 비로소 API를 호출합니다.
                debounceJob = scope.launch {
                    delay(1500)

                    val pos = naverMap.cameraPosition.target
                    val isCityHall = Math.abs(pos.latitude - 37.5666) < 0.001 &&
                            Math.abs(pos.longitude - 126.9784) < 0.001

                    if (!isCityHall) {
                        Log.d("MapContent", "카메라가 완전히 정착했습니다. 데이터 조회를 시작합니다.")
                        fetchMarkersInView(naverMap, viewModel)
                    }
                }
            }
            onMapReady(naverMap)
        }
    }

    // 3. 마커 갱신 로직 (viewModel의 데이터가 바뀔 때마다 실행)
    viewModel?.let { vm ->
        LaunchedEffect(vm.mapMarkers) {
            val map = naverMapInstance ?: return@LaunchedEffect

            // 기존 마커 제거
            activeMarkers.forEach { it.map = null }
            activeMarkers.clear()

            // 서버에서 받은 데이터로 새 마커 생성
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

    // 4. 생명주기 관리 (onDestroy 제외로 캐싱 유지)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)

            // 📍 [핵심] 화면을 나갈 때 지도의 상태를 '백지상태'로 만듭니다.
            naverMapInstance?.let { map ->
                // 1. 모든 마커와 정보창을 지도에서 즉시 제거
                activeMarkers.forEach { it.map = null }

                // 2. 만약 다른 리스너들이 남아있다면 여기서 제거 (선택 사항)
                // map.onCameraIdleListener = null
            }
            activeMarkers.clear()

            // 📍 [팁] 지도가 너무 뜬금없는 곳을 보고 있지 않게 하고 싶다면
            // 여기서 카메라를 아주 살짝 투명하게 하거나 가리는 처리를 할 수도 있습니다.
        }
    }

    AndroidView(
        factory = {
            // 부모와 이별할 때도 확실하게
            (mapView.parent as? ViewGroup)?.let { parent ->
                parent.removeView(mapView)
            }

            // 지도가 다시 붙을 때 레이아웃 파라미터를 명확히 설정
            mapView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            mapView
        },
        modifier = modifier.fillMaxSize(),
        update = { _ ->
            // 📍 깜빡임을 줄이려면 여기서 아무것도 하지 않는 것이 좋습니다.
        }
    )
}

private fun fetchMarkersInView(naverMap: NaverMap, viewModel: ReportViewModel?) {
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