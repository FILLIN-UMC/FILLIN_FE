package com.example.fillin.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * FILLIN 백엔드 API용 Retrofit 클라이언트
 * - data.kakao.RetrofitClient: 카카오 지도/주소 API
 * - data.api.RetrofitClient: FILLIN 서버 API (로그인, 회원가입 등)
 *
 * local.properties에 BASE_URL 추가 후 BuildConfig로 주입 가능
 */
object RetrofitClient {
    // TODO: API 명세서의 서버 URL로 변경, local.properties + BuildConfig 권장
    private const val BASE_URL = "https://api.example.com/"

    private fun createOkHttpClient(context: android.content.Context): OkHttpClient {
        val appContext = context.applicationContext
        return OkHttpClient.Builder()
            .addInterceptor(AuthTokenInterceptor(appContext))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
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
}
