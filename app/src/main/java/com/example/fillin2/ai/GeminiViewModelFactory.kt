package com.example.fillin2.ai

// 매개변수(repository)가 있는 ViewModel을 만들기 위해서는 이 팩토리 클래스가 반드시 필요합니다.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fillin2.ai.GeminiRepository

class GeminiViewModelFactory(private val repository: GeminiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeminiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GeminiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}