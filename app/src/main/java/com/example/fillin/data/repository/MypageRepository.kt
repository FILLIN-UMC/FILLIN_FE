package com.example.fillin.data.repository

import android.content.Context
import android.net.Uri
import com.example.fillin.data.api.MypageApiService
import com.example.fillin.data.api.RetrofitClient
import com.example.fillin.data.model.mypage.ProfileRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * 마이페이지 API Repository
 */
class MypageRepository(private val context: Context) {

    private val api: MypageApiService = RetrofitClient.getMypageApi(context)
    private val gson = Gson()

    suspend fun getProfile() = runCatching {
        api.getProfile()
    }

    suspend fun updateProfile(nickname: String, profileImageUrl: String? = null, imageUri: Uri? = null) = runCatching {
        val request = ProfileRequest(nickname = nickname, profileImageUrl = profileImageUrl)
        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = imageUri?.let { uriToPart(it) }
        api.updateProfile(request = requestBody, image = imagePart)
    }

    private suspend fun uriToPart(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("image", file.name, requestBody)
    }

    suspend fun checkNickname(nickname: String) = runCatching {
        api.checkNickname(nickname)
    }

    suspend fun getRanks() = runCatching {
        api.getRanks()
    }

    suspend fun getNotificationSettings() = runCatching {
        api.getNotificationSettings()
    }

    suspend fun updateNotificationSettings(
        reportAlarm: Boolean,
        feedbackAlarm: Boolean,
        serviceAlarm: Boolean
    ) = runCatching {
        api.updateNotificationSettings(
            com.example.fillin.data.model.mypage.NotificationRequest(
                reportAlarm = reportAlarm,
                feedbackAlarm = feedbackAlarm,
                serviceAlarm = serviceAlarm
            )
        )
    }

    suspend fun getReportCount() = runCatching {
        api.getReportCount()
    }

    suspend fun getReportCategory() = runCatching {
        api.getReportCategory()
    }

    suspend fun getReportExpireSoon() = runCatching {
        api.getReportExpireSoon()
    }

    suspend fun getReportExpireSoonDetail() = runCatching {
        api.getReportExpireSoonDetail()
    }

    suspend fun getMyReports() = runCatching {
        api.getMyReports()
    }

    suspend fun getMyReportsExpired() = runCatching {
        api.getMyReportsExpired()
    }

    suspend fun deleteReport(reportId: Long) = runCatching {
        api.deleteReport(reportId)
    }

    /**
     * 현재 사용자가 등록한 모든 제보를 사라진 제보로 이동 (백엔드 API)
     * @return 삭제된 제보 개수, 실패 시 null
     */
    suspend fun deleteAllMyReports(): Int? = runCatching {
        val reports = api.getMyReports().data ?: return@runCatching 0
        var count = 0
        reports.forEach { item ->
            item.reportId?.let { reportId ->
                runCatching { api.deleteReport(reportId) }.onSuccess { count++ }
            }
        }
        count
    }.getOrNull()

    suspend fun getLikedReports() = runCatching {
        api.getLikedReports()
    }

    suspend fun withdraw() = runCatching {
        api.withdraw()
    }
}
