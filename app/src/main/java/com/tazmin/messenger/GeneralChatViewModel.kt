package com.tazmin.messenger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GeneralChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _messages = MutableLiveData<List<GeneralMessage>>()
    val messages: LiveData<List<GeneralMessage>> get() = _messages

    fun loadMessages() {
        chatRepository.getGeneralChatMessages { messages ->
            _messages.value = messages
        }
    }

    fun sendMessage(message: GeneralMessage) {
        chatRepository.sendGeneralChatMessage(message, {
            // Сообщение успешно отправлено
        }, {
            // Обработка ошибки
        })
    }
}