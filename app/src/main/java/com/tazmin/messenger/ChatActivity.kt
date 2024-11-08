package com.tazmin.messenger

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var viewModel: ChatViewModel
    private lateinit var currentUserId: String
    private lateinit var chatUserId: String
    private lateinit var chatUserName: String

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


        val chatRepository = ChatRepository()
        // ChatViewModel абстрагирует и управляет данными для View (интерфейса)
        // и взаимодействует с Model (в данном случае, ChatRepository).
        viewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(chatRepository)
        )[ChatViewModel::class.java]

        val titleTextView = findViewById<TextView>(R.id.title)
        titleTextView.text = chatUserName

        viewModel.messages.observe(this) { messages ->
            messageAdapter.setMessages(messages)
        }

        viewModel.loadMessages(getChatId(currentUserId, chatUserId))

        //другой способ
        findViewById<ImageButton>(R.id.sendButton).setOnClickListener {
            val messageText = findViewById<EditText>(R.id.messageInput).text.toString().trim()
            if (messageText.isNotEmpty()) {
                val message = Message(currentUserId, chatUserId, messageText, null)
                viewModel.sendMessage(getChatId(currentUserId, chatUserId), message)
                findViewById<EditText>(R.id.messageInput).text.clear()
            } else {
                Toast.makeText(this, "Сообщение не должно быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener{
            finish()
        }
    }

    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }
}
