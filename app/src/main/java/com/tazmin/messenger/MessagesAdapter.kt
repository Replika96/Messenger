package com.tazmin.messenger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(
    val senderId: String? = null,
    val receiverId: String? = null,
    val message: String? = null,
    val timestamp: Long? = null
) {
    // Конструктор без аргументов требуется для Firebase
    constructor() : this(null, null, null, null)
}
class MessageAdapter(private val currentUserId: String) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages: List<Message> = listOf()

    fun setMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            if (viewType == VIEW_TYPE_SENT) R.layout.item_message_sent else R.layout.item_message_received,
            parent,
            false
        )
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.time)
        fun bind(message: Message) {
            messageTextView.text = message.message
            message.timestamp?.let {
                val date = Date(it)
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeTextView.text = format.format(date)
            } ?: run {
                // Если времени нет, ничего не отображаем
                timeTextView.text = ""
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}


