package com.example.fillin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

// Context 확장: DataStore 생성
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

class AppPreferences(private val context: Context) {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_TERMS_ACCEPTED = booleanPreferencesKey("is_terms_accepted")
        val IS_PERMISSION_GRANTED = booleanPreferencesKey("is_permission_granted")
        val IS_LOCATION_HISTORY_CONSENT = booleanPreferencesKey("is_location_history_consent")
        val IS_MARKETING_CONSENT = booleanPreferencesKey("is_marketing_consent")
    }

    // 상태 Flow들
    val isLoggedInFlow = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val isTermsAcceptedFlow = context.dataStore.data.map { preferences ->
        preferences[IS_TERMS_ACCEPTED] ?: false
    }

    val isPermissionGrantedFlow = context.dataStore.data.map { preferences ->
        preferences[IS_PERMISSION_GRANTED] ?: false
    }

    // 상태 저장 함수들
    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = value
        }
    }

    suspend fun setTermsAccepted(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_TERMS_ACCEPTED] = value
        }
    }

    suspend fun setPermissionGranted(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PERMISSION_GRANTED] = value
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val isLocationHistoryConsent = context.dataStore.data.map {
        it[IS_LOCATION_HISTORY_CONSENT] ?: false
    }

    val isMarketingConsent = context.dataStore.data.map {
        it[IS_MARKETING_CONSENT] ?: false
    }

    suspend fun setLocationHistoryConsent(value: Boolean) {
        context.dataStore.edit {
            it[IS_LOCATION_HISTORY_CONSENT] = value
        }
    }

    suspend fun setMarketingConsent(value: Boolean) {
        context.dataStore.edit {
            it[IS_MARKETING_CONSENT] = value
        }
    }


}
