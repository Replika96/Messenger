package com.tazmin.messenger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GeneralChatViewModelFactory(
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeneralChatViewModel::class.java)) {
            return GeneralChatViewModel(chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
