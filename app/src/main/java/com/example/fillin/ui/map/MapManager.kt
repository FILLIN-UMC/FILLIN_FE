package com.example.fillin.ui.map

import android.content.Context
import android.view.View
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMapOptions

object MapManager {
    private var mapView: MapView? = null

    fun getMapView(context: Context): MapView {
        if (mapView == null) {
            // 📍 [핵심] TextureView 사용 옵션 적용
            val options = NaverMapOptions().useTextureView(true)

            mapView = MapView(context.applicationContext, options).apply {
                id = View.generateViewId()
            }
        }
        return mapView!!
    }

    // 앱이 완전히 종료되거나 메모리 정리가 필요할 때 호출
    fun clear() {
        mapView?.onDestroy()
        mapView = null
    }
}