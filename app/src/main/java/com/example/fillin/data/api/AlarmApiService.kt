package com.example.fillin.data.api

import com.example.fillin.data.model.alarm.AlarmListResponse
import com.example.fillin.data.model.auth.ApiResponse
import retrofit2.http.PATCH
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 알림(알람) 관련 API
 * Base URL: https://api.fillin.site
 */
interface AlarmApiService {

    @GET("api/alarm/list")
    suspend fun getAlarmList(
        @Query("read") read: Boolean? = null
    ): AlarmListResponse

    @PATCH("api/alarm/{alarmId}/read")
    suspend fun markAlarmAsRead(@Path("alarmId") alarmId: Long): ApiResponse<Unit>
}
