package com.example.fillin.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * FILLIN 백엔드 API용 Retrofit 클라이언트
 * Base URL: https://api.fillin.site
 */
object RetrofitClient {
    private const val BASE_URL = "https://api.fillin.site/"
    private const val LOG_TAG = "FILLIN_API"

    private fun createOkHttpClient(context: android.content.Context): OkHttpClient {
        val appContext = context.applicationContext
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthTokenInterceptor(appContext))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // 디버그: 요청/응답 로깅 (Logcat에서 확인 가능)
        if (com.example.fillin.BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor { message -> Log.d(LOG_TAG, message) }
                    .apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        return builder.build()
    }

    private var retrofit: Retrofit? = null

    fun getRetrofit(context: android.content.Context): Retrofit {
        val appContext = context.applicationContext
        return retrofit ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(appContext))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .also { retrofit = it }
    }

    fun getUserApi(context: android.content.Context): UserApiService =
        getRetrofit(context).create(UserApiService::class.java)

    fun getMypageApi(context: android.content.Context): MypageApiService =
        getRetrofit(context).create(MypageApiService::class.java)
}
