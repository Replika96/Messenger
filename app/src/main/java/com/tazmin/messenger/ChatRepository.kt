package com.tazmin.messenger

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    fun getMessages(chatId: String, onMessagesLoaded: (List<Message>) -> Unit) {
        db.collection("chat")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, _ ->
                querySnapshot?.let {
                    val messages = it.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)
                    }
                    onMessagesLoaded(messages)
                }
            }
    }

    fun sendMessage(
        chatId: String,
        message: Message,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("chat")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .await()
                updateNewMessageFlag(message.receiverId, message.senderId)

                CoroutineScope(Dispatchers.Main).launch {
                    onSuccess()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onFailure(e)
                }
            }
        }
    }

    private suspend fun updateNewMessageFlag(receiverId: String?, senderId: String?) {
        if (receiverId == null || senderId == null) return
        db.collection("users").document(receiverId)
            .update("newMessageFrom", FieldValue.arrayUnion(senderId))
            .await()
    }

    /*private suspend fun sendNotificationToRecipient(receiverId: String, message: Message) {
        try {
            val document = db.collection("users").document(receiverId).get().await() // Добавляем .await() для асинхронного вызова
            val token = document.getString("token")

            if (!token.isNullOrEmpty()) {
                val notificationTitle = "Новое сообщение от ${message.senderId}"
                val notificationBody = message.message ?: "У вас новое сообщение"
                sendPushNotification(token, notificationTitle, notificationBody) // Вызываем suspend функцию в асинхронном контексте
            } else {
                Log.e("ChatRepository", "Токен не найден для пользователя $receiverId")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка получения токена: ${e.message}")
        }
    }



    private suspend fun sendPushNotification(token: String, title: String, body: String) {
        val jsonPayload = """
        {
            "to": "$token",
            "notification": {
                "title": "$title",
                "body": "$body"
            }
        }
    """.trimIndent()


        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://fcm.googleapis.com/fcm/send")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "key=BEXBAAx0ZJvxs2Zd1HzMe5M0GXAZJPihXeDK8C8yXXevesfGjycn4pxGUpd3cfhvhaVWyheFiTMqk8L5_DIsIvs")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("ChatRepository", "Уведомление успешно отправлено")
                } else {
                    Log.e("ChatRepository", "Ошибка отправки уведомления: Код $responseCode")
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Ошибка при отправке уведомления: ${e.message}")
            }
        }
    }*/
}