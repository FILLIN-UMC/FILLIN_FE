package com.example.fillin.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.LocationManager
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Looper
import android.provider.Settings
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

    // 위치 서비스가 활성화되어 있는지 확인
    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    // 현재 위치를 찾아서 지도를 이동시키는 핵심 함수
    @SuppressLint("MissingPermission")
    fun moveMapToCurrentLocation(naverMap: NaverMap) {
        // 위치 서비스가 활성화되어 있지 않으면 아무 동작도 하지 않음
        if (!isLocationEnabled()) {
            return
        }
        
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
    // ★ 여기에 @SuppressLint 추가 (오류 해결 1)
    @SuppressLint("MissingPermission")
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
        // 캐시된 위치가 있으면 즉시 마커 표시 (첫 위치 수신 전에도 보이도록)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                if (it.latitude != 0.0 || it.longitude != 0.0) {
                    naverMap.locationOverlay.apply {
                        isVisible = true
                        position = LatLng(it.latitude, it.longitude)
                        customIcon?.let { icon = it }
                    }
                }
            }
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(2000)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // (0,0)이 아닌 모든 좌표에서 마커 표시 (한국 밖/에뮬레이터에서도 보이도록)
                    if (location.latitude != 0.0 || location.longitude != 0.0) {
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

    /**
     * 모든 에러를 방지하는 가장 안정적인 주소 변환 함수.
     * 도로명 주소(시/도 + 구/군 + 도로명 + 건물번호)를 우선 반환하고,
     * 도로명 정보가 없으면 getAddressLine(0) 결과를 반환한다.
     */
    fun getAddressFromCoords(lat: Double, lng: Double, callback: (String) -> Unit) {
        val geocoder = android.location.Geocoder(context, java.util.Locale.KOREA)

        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val roadAddress = buildRoadAddress(addr)
                val result = if (roadAddress.isNotBlank()) roadAddress else addr.getAddressLine(0).orEmpty()
                callback(cleanAddress(result))
            } else {
                callback("주소를 찾을 수 없는 지역입니다")
            }
        } catch (e: Exception) {
            callback("주소 로딩 실패 (네트워크 확인)")
        }
    }

    /**
     * Address에서 도로명 주소 조합: 시/도 + 구/군 + 도로명 + 건물번호
     * 도로명(thoroughfare)이 없으면 빈 문자열 반환 → getAddressLine(0) 폴백
     */
    private fun buildRoadAddress(addr: Address): String {
        val thoroughfare = addr.thoroughfare?.takeIf { it.isNotBlank() } ?: return ""
        val adminArea = addr.adminArea?.takeIf { it.isNotBlank() }
        val subLocality = addr.subLocality?.takeIf { it.isNotBlank() }
        val subThoroughfare = addr.subThoroughfare?.takeIf { it.isNotBlank() }
        return listOfNotNull(adminArea, subLocality, thoroughfare, subThoroughfare).joinToString(" ")
    }

    /**
     * "대한민국 서울특별시..." 에서 "대한민국" 등을 떼어내는 가공 함수
     */
    private fun cleanAddress(address: String): String {
        return address.replace("대한민국 ", "").replace("한국 ", "")
    }
}
