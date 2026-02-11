package com.example.fillin.data.repository

import android.content.Context
import com.example.fillin.data.api.AlarmApiService
import com.example.fillin.data.api.RetrofitClient
import com.example.fillin.data.model.alarm.AlarmResponse

/**
 * 알림(알람) API Repository
 */
class AlarmRepository(private val context: Context) {

    private val api: AlarmApiService = RetrofitClient.getAlarmApi(context)

    suspend fun getAlarmList(read: Boolean? = null) = runCatching {
        api.getAlarmList(read = read)
    }

    suspend fun markAlarmAsRead(alarmId: Long) = runCatching {
        api.markAlarmAsRead(alarmId)
    }
}
