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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneralMessage(
    val senderId: String? = null,
    val senderName: String? = null,
    val message: String? = null,
    val fileUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
class GeneralChatAdapter(
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages: List<GeneralMessage> = listOf()

    fun setMessages(newMessages: List<GeneralMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val view = layoutInflater.inflate(R.layout.item_general_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.item_general_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }


    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.time)
        private val fileImageView: ImageView = itemView.findViewById(R.id.fileImageView)
        fun bind(message: GeneralMessage) {
            messageTextView.text = message.message
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeTextView.text = timeFormat.format(Date(message.timestamp))
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

        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderNameTextView: TextView = itemView.findViewById(R.id.senderNameTextView)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.time)
        private val fileImageView: ImageView = itemView.findViewById(R.id.fileImageView)

        fun bind(message: GeneralMessage) {
            senderNameTextView.text = message.senderName ?: "Anonymous"
            messageTextView.text = message.message
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeTextView.text = timeFormat.format(Date(message.timestamp))
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
        }
    }
    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}



