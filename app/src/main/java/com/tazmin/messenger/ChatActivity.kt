package com.tazmin.messenger

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.min

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var viewModel: ChatViewModel
    private lateinit var currentUserId: String
    private lateinit var chatUserId: String
    private lateinit var chatUserName: String
    private val chatRepository = ChatRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        chatUserId = intent.getStringExtra("userId") ?: ""
        chatUserName = intent.getStringExtra("userName") ?: "No name"

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter(currentUserId)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messageAdapter

        // ChatViewModel абстрагирует и управляет данными для View (интерфейса)
        // и взаимодействует с Model (в данном случае, ChatRepository).

        viewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(chatRepository)
        )[ChatViewModel::class.java]

        //заголовок
        findViewById<TextView>(R.id.title).text = chatUserName
        //// наблюдение за сообщениями
        viewModel.messages.observe(this) { messages ->
            messageAdapter.setMessages(messages)
            messagesRecyclerView.scrollToPosition(messages.size - 1)

        }

        viewModel.loadMessages(getChatId(currentUserId, chatUserId))

        //Другой способ. Отправка сообщения
        findViewById<ImageButton>(R.id.sendButton).setOnClickListener {
            val messageText = findViewById<EditText>(R.id.messageInput).text.toString().trim()
            if (messageText.isNotEmpty()) {
                val message = Message(
                    senderId = currentUserId,
                    receiverId = chatUserId,
                    message = messageText,
                    fileUrl = null,
                    timestamp = System.currentTimeMillis()
                )
                viewModel.sendMessage(getChatId(currentUserId, chatUserId), message)
                findViewById<EditText>(R.id.messageInput).text.clear()
            } else {
                Toast.makeText(this, "Сообщение не должно быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sendMessageWithFile(it) }
        }
        findViewById<ImageButton>(R.id.attachFileButton).setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener{
            finish()
        }

    }
    override fun onResume() {
        super.onResume()
        val chatId = getChatId(currentUserId, chatUserId)
        markMessagesAsRead(chatId, currentUserId)
        clearNewMessageFlag(currentUserId)
    }
    private fun clearNewMessageFlag(senderId: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUserRef = db.collection("users").document(currentUserId)
        currentUserRef.update("newMessageFrom", FieldValue.arrayRemove(senderId))
            .addOnSuccessListener {
                Log.d("ChatActivity", "Флаг нового сообщения очищен для пользователя $senderId")
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Ошибка очистки флага: ${e.message}")
            }
    }
    private fun markMessagesAsRead(chatId: String, currentUserId: String) {
        val db = FirebaseFirestore.getInstance()
        val messagesRef = db.collection("chat").document(chatId).collection("messages")

        messagesRef
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                Log.d("Chat", "Найдено ${querySnapshot.size()} сообщений для обновления")
                for (document in querySnapshot.documents) {
                    Log.d("Chat", "Сообщение: ${document.data}")
                    val docRef = messagesRef.document(document.id)
                    batch.update(docRef, "read", true)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Chat", "Сообщения помечены как прочитанные")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Chat", "Ошибка при обновлении статуса сообщений: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Chat", "Ошибка при получении сообщений: ${e.message}")
            }
    }

    private fun sendMessageWithFile(imageUri: Uri) {
        lifecycleScope.launch {
            val imageUrl = chatRepository.uploadImageToImgur(imageUri, this@ChatActivity)
            if (imageUrl != null) {
                val message = Message(
                    senderId = currentUserId,
                    receiverId = chatUserId,
                    message = null,
                    fileUrl = imageUrl,
                    timestamp = System.currentTimeMillis()
                )
                viewModel.sendMessage(getChatId(currentUserId, chatUserId), message)
            } else {
                Toast.makeText(this@ChatActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }
}
