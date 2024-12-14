package com.tazmin.messenger

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class GeneralChatFragment : Fragment(R.layout.fragment_general_chat) {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: GeneralChatAdapter
    private lateinit var viewModel: GeneralChatViewModel
    private lateinit var messageInput: EditText
    private val chatRepository = ChatRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messagesRecyclerView = view.findViewById(R.id.generalChatRecyclerView)
        messageAdapter = GeneralChatAdapter(FirebaseAuth.getInstance().currentUser?.uid ?: "")
        messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        messagesRecyclerView.adapter = messageAdapter
        messageInput = view.findViewById(R.id.messageInput)
        viewModel = ViewModelProvider(
            this,
            GeneralChatViewModelFactory(chatRepository)
        )[GeneralChatViewModel::class.java]

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.setMessages(messages)
            if (::messageAdapter.isInitialized && messages.isNotEmpty()) {
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }

        }

        viewModel.loadMessages()

        view.findViewById<ImageButton>(R.id.sendButton).setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val senderName = snapshot.getString("username") ?: "Anonymous"
                            Log.d("SendMessage", "Имя пользователя: $senderName")

                            val message = GeneralMessage(
                                senderId = currentUserId,
                                senderName = senderName,
                                message = messageText,
                                timestamp = System.currentTimeMillis()
                            )

                            viewModel.sendMessage(message)
                            messageInput.text.clear()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SendMessage", "Ошибка получения имени: ${exception.message}")
                            Toast.makeText(context, "Ошибка отправки сообщения.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sendMessageWithFile(it) }
        }
        view.findViewById<ImageButton>(R.id.attachFileButton).setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }
    private fun sendMessageWithFile(imageUri: Uri) {
        lifecycleScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val imageUrl = chatRepository.uploadImageToImgur(imageUri, requireContext())
            if (imageUrl != null) {
                val message = GeneralMessage(
                    senderId = currentUserId,
                    message = null,
                    fileUrl = imageUrl,
                    timestamp = System.currentTimeMillis()
                )
                viewModel.sendMessage(message)
            }
        }
    }
}

