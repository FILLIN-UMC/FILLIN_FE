package com.example.fillin2.map


import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap

class PresentLocation(private val context: Context) {
    // 구글 위치 서비스 클라이언트 초기화
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // 현재 위치를 찾아서 지도를 이동시키는 핵심 함수
    @SuppressLint("MissingPermission")
    fun moveMapToCurrentLocation(naverMap: NaverMap) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)

                // 1. 카메라 이동: 부드럽게 현재 위치로 슈웅~
                val cameraUpdate = CameraUpdate.scrollTo(latLng)
                    .animate(CameraAnimation.Easing)
                naverMap.moveCamera(cameraUpdate)

                // 2. 내 위치 블루닷 표시
                naverMap.locationOverlay.apply {
                    isVisible = true
                    position = latLng
                }
            }
        }
    }
}