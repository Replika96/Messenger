package com.tazmin.messenger



import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject



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

    fun getGeneralChatMessages(onMessagesLoaded: (List<GeneralMessage>) -> Unit) {
        db.collection("generalChat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, _ ->
                querySnapshot?.let {
                    val messages = it.documents.mapNotNull { doc -> doc.toObject(GeneralMessage::class.java) }
                    onMessagesLoaded(messages)
                }
            }
    }

    fun sendGeneralChatMessage(
        message: GeneralMessage,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SendGeneralMessage", "Отправка сообщения: $message")
                db.collection("generalChat")
                    .add(message)
                    .await()

                withContext(Dispatchers.Main) {
                    Log.d("SendGeneralMessage", "Сообщение успешно отправлено")
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("SendGeneralMessage", "Ошибка при отправке сообщения: ${e.message}")
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }

    private val client = OkHttpClient()

    suspend fun uploadImageToImgur(imageUri: Uri, context: Context): String? {
        return try {
            val bitmap = ImageUtils.getBitmapFromUri(context, imageUri)?.let {
                ImageUtils.resizeBitmap(it, 1024, 1024)
            } ?: return null

            val encodedImage = ImageUtils.encodeImageToBase64(bitmap)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", encodedImage)
                .build()

            val request = Request.Builder()
                .url("https://api.imgur.com/3/image")
                .addHeader("Authorization", "Client-ID BuildConfig.API_KEY}")
                .post(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                JSONObject(responseBody!!).getJSONObject("data").getString("link")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Imgur", "Ошибка загрузки изображения: ${e.message}")
            null
        }
    }
}