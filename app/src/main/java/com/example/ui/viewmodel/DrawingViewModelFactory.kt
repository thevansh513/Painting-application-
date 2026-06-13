package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.DrawingRepository

class DrawingViewModelFactory(
    private val repository: DrawingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DrawingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DrawingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class Exception")
    }
}
