package com.tazmin.messenger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()// _messages: Это MutableLiveData,
    // хранящая список сообщений (List<Message>). Используется для хранения и обновления данных в ViewModel.
    val messages: LiveData<List<Message>> get() = _messages // только для чтения

    fun loadMessages(chatId: String) {
        chatRepository.getMessages(chatId) { messages ->
            _messages.value = messages.sortedBy { it.timestamp }
        }
    }

    fun sendMessage(chatId: String, message: Message) {
        chatRepository.sendMessage(chatId, message, {
            // сообщение отправлено успешно
        }, {
            // обработка ошибки
        })
    }
}
