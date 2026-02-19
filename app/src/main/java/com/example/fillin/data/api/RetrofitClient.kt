package com.example.fillin.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.fillin.site/"
    private const val LOG_TAG = "FILLIN_API"

    private fun createOkHttpClient(context: android.content.Context): OkHttpClient {
        val appContext = context.applicationContext
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthTokenInterceptor(appContext))
            .authenticator(TokenAuthenticator(appContext))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

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

    fun getReportApi(context: android.content.Context): ReportApiService =
        getRetrofit(context).create(ReportApiService::class.java)

    fun getAlarmApi(context: android.content.Context): AlarmApiService =
        getRetrofit(context).create(AlarmApiService::class.java)

    fun getMemberApi(context: android.content.Context): MemberApiService =
        getRetrofit(context).create(MemberApiService::class.java)
}
