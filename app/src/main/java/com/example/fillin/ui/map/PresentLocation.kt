package com.example.fillin.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.OverlayImage

class PresentLocation(private val context: Context) {
    // 구글 위치 서비스 클라이언트 초기화
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    // 위치 업데이트 콜백
    private var locationCallback: LocationCallback? = null

    // 현재 위치를 찾아서 지도를 이동시키는 핵심 함수
    @SuppressLint("MissingPermission")
    fun moveMapToCurrentLocation(naverMap: NaverMap) {
        // 먼저 캐시된 위치를 확인
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                // 위치가 유효한지 확인 (위도/경도가 0이 아니고, 정상 범위 내인지)
                if (isValidLocation(it.latitude, it.longitude)) {
                    val latLng = LatLng(it.latitude, it.longitude)
                    moveCameraToLocation(naverMap, latLng)
                } else {
                    // 캐시된 위치가 유효하지 않으면 현재 위치 요청
                    requestCurrentLocation(naverMap)
                }
            } ?: run {
                // 캐시된 위치가 없으면 현재 위치 요청
                requestCurrentLocation(naverMap)
            }
        }.addOnFailureListener {
            // lastLocation 실패 시 현재 위치 요청
            requestCurrentLocation(naverMap)
        }
    }
    
    private fun requestCurrentLocation(naverMap: NaverMap) {
        val cancellationTokenSource = CancellationTokenSource()
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(5000) // 최대 5초 대기
            .build()
        
        fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token)
            .addOnSuccessListener { currentLocation: android.location.Location? ->
                currentLocation?.let {
                    if (isValidLocation(it.latitude, it.longitude)) {
                        val latLng = LatLng(it.latitude, it.longitude)
                        moveCameraToLocation(naverMap, latLng)
                    }
                }
            }
            .addOnFailureListener {
                // 위치를 가져오지 못한 경우 처리 (에러 로그 등)
            }
    }
    
    // 위치가 유효한지 확인 (한국 지역 범위: 위도 33-43, 경도 124-132)
    private fun isValidLocation(latitude: Double, longitude: Double): Boolean {
        return latitude != 0.0 && longitude != 0.0 &&
               latitude >= 33.0 && latitude <= 43.0 &&
               longitude >= 124.0 && longitude <= 132.0
    }
    
    private fun moveCameraToLocation(naverMap: NaverMap, latLng: LatLng, bearing: Float? = null) {
        // 1. 카메라 이동: 현재 위치를 지도 중앙에 정렬하고 적절한 확대 레벨로 조정
        val cameraUpdate = CameraUpdate.scrollAndZoomTo(latLng, 16.0)
            .animate(CameraAnimation.Easing)
        naverMap.moveCamera(cameraUpdate)

        // 2. 내 위치 커스텀 핀 표시
        naverMap.locationOverlay.apply {
            isVisible = true
            position = latLng
            bearing?.let {
                this.bearing = it
            }
        }
    }
    
    // 실시간 위치 업데이트 시작
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(naverMap: NaverMap, customIcon: OverlayImage? = null) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(2000)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (isValidLocation(location.latitude, location.longitude)) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        val bearing = if (location.hasBearing()) location.bearing else null
                        
                        naverMap.locationOverlay.apply {
                            isVisible = true
                            position = latLng
                            bearing?.let { this.bearing = it }
                            customIcon?.let { icon = it }
                        }
                    }
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }
    
    // 위치 업데이트 중지
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
    
    // 초기 위치 설정 및 커스텀 아이콘 적용
    fun setupLocationOverlay(naverMap: NaverMap, customIcon: OverlayImage? = null) {
        naverMap.locationOverlay.apply {
            customIcon?.let { icon = it }
        }
    }
}
