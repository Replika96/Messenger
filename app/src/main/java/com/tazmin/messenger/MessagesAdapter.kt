package com.tazmin.messenger

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.PropertyName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(
    val senderId: String? = null,
    val receiverId: String? = null,
    val message: String? = null,
    val fileUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
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
        private val fileImageView: ImageView = itemView.findViewById(R.id.fileImageView)
        private val readStatusIcon: ImageView? = itemView.findViewById(R.id.readStatusIcon)

        fun bind(message: Message) {
            messageTextView.text = message.message


            message.timestamp?.let {
                val date = Date(it)
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeTextView.text = format.format(date)
            }

            if (!message.fileUrl.isNullOrEmpty()) {
                fileImageView.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.fileUrl)
                    .into(fileImageView)

                fileImageView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.fileUrl))
                    itemView.context.startActivity(intent)
                }
            } else {
                fileImageView.visibility = View.GONE
            }


            readStatusIcon?.let {
                if (message.senderId == currentUserId) {
                    it.visibility = View.VISIBLE
                    if (message.read) {
                        it.setImageResource(R.drawable.ic_read)
                    } else {
                        it.setImageResource(R.drawable.ic_unread)
                    }
                } else {
                    it.visibility = View.GONE
                }
            }
        }
    }


    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}


