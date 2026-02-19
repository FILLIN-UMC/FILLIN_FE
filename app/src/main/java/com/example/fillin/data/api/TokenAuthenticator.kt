package com.example.fillin.data.api

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(private val context: Context) : Authenticator {

    private val appPreferences by lazy { AppPreferences(context) }
    private val authRepository by lazy { AuthRepository(context) }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code == 401) {
            Log.d("AUTH_DEBUG", "401 Unauthorized detected. Attempting to reissue token...")

            val result = runBlocking { authRepository.reissueToken() }

            return if (result.isSuccess) {
                Log.d("AUTH_DEBUG", "Token reissue success. Retrying request.")
                val newAccessToken = TokenManager.getAccessToken(context)
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                Log.e("AUTH_DEBUG", "Token reissue failed. Logging out.")
                runBlocking {
                    appPreferences.clearAll()
                    TokenManager.clearTokens(context)
                }

                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)

                null
            }
        }
        return null
    }
}