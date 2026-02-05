package com.example.fillin.feature.mypage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.repository.MypageRepository

class MyPageViewModelFactory(
    private val context: Context,
    private val appPreferences: AppPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyPageViewModel(
                appPreferences = appPreferences,
                mypageRepository = MypageRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
