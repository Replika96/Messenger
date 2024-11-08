package com.tazmin.messenger

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

    fun sendMessage(chatId: String, message: Message, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("chat")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}