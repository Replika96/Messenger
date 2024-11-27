package com.tazmin.messenger

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

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

        findViewById<ImageButton>(R.id.attachFileButton).setOnClickListener {
            pickFile()
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

    private fun pickFile() {
        pickFileLauncher.launch("*/*")
    }
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadFileToFirebase(it)
        }
    }
    private fun uploadFileToFirebase(fileUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = fileUri.lastPathSegment ?: "unknown_file"
        val fileRef = storageRef.child("chat_files/${System.currentTimeMillis()}_$fileName")

        fileRef.putFile(fileUri)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                Log.d("UploadProgress", "Загружено: $progress%")
            }
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    sendMessageWithFile(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки файла: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessageWithFile(fileUrl: String) {
        val message = Message(
            senderId = currentUserId,
            receiverId = chatUserId,
            message = null,
            fileUrl = fileUrl,
            timestamp = System.currentTimeMillis()
        )
        viewModel.sendMessage(getChatId(currentUserId, chatUserId), message)
    }



    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }
}
