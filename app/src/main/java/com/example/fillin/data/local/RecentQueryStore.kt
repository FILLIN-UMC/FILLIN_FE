package com.example.fillin.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecentQueryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyList = "recent_queries"
    private val maxSize = 10
    private val _flow = MutableStateFlow(getAll())
    fun flow(): Flow<List<String>> = _flow.asStateFlow()

    fun getAll(): List<String> {
        val set = prefs.getStringSet(keyList, null) ?: return emptyList()
        return set.toList().reversed()
    }

    suspend fun push(query: String) {
        if (query.isBlank()) return
        val current = getAll().toMutableList()
        current.remove(query)
        current.add(0, query)
        val trimmed = current.take(maxSize)
        prefs.edit().putStringSet(keyList, trimmed.toSet()).apply()
        _flow.value = getAll()
    }

    suspend fun remove(query: String) {
        val current = getAll().toMutableList()
        current.remove(query)
        prefs.edit().putStringSet(keyList, current.toSet()).apply()
        _flow.value = getAll()
    }

    companion object {
        private const val PREFS_NAME = "search_recent"
    }
}
