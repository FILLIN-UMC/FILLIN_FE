package com.example.fillin.data.location

import android.content.Context
import android.location.LocationManager

class LocationProvider(private val context: Context) {

    fun getLatLng(): Pair<Double, Double>? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        return location?.let { it.latitude to it.longitude }
    }
}
