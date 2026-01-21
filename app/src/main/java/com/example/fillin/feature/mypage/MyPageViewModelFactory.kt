package com.example.fillin.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MyPageViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyPageViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
