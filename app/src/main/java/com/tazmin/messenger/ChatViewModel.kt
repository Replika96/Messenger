package com.tazmin.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    fun loadMessages(chatId: String) {
        chatRepository.getMessages(chatId) { messages ->
            _messages.value = messages.sortedBy { it.timestamp }

        }
    }
    fun sendMessage(chatId: String, message: Message) {
        chatRepository.sendMessage(chatId, message, {
            // Сообщение успешно отправлено
        }, {
            // Обработка ошибки
        })
    }
}

