package com.example.fillin2.report

import androidx.lifecycle.ViewModelProvider
import com.example.fillin2.db.FirestoreRepository

class ReportViewModelFactory(private val repository: FirestoreRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}