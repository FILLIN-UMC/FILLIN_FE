package com.example.fillin.ui.login

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleSignInClient {

    /** return: Google ID Token */
    suspend fun getIdToken(
        activity: Activity,
        webClientId: String
    ): String {

        val credentialManager = CredentialManager.create(activity)

        suspend fun request(filterByAuthorized: Boolean): String {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId) // ✅ "웹 클라이언트 ID"
                .setFilterByAuthorizedAccounts(filterByAuthorized)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
            return googleCred.idToken
        }

        return try {
            // 1) 기존 승인된 계정만 먼저
            request(filterByAuthorized = true)
        } catch (e: NoCredentialException) {
            // 2) 없으면 전체 계정 대상으로 다시 (🔥 이게 fallback)
            request(filterByAuthorized = false)
        }
    }
}
