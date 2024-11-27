package com.tazmin.messenger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class User(
    val uid: String,
    val username: String,
    val avatarUrl: String? = null,
    val newMessageFrom: List<String> = emptyList())

class UsersAdapter(
    private var users: List<User>,
    private val currentUserNewMessages: Set<String>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private val animatedPositions = mutableSetOf<Int>()

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val newMessageIcon: ImageView = itemView.findViewById(R.id.newMessageIcon)
        private val avatarImageView: ImageView = itemView.findViewById(R.id.user_icon)

        fun bind(user: User, isNewMessage: Boolean) {
            usernameTextView.text = user.username
            newMessageIcon.visibility = if (isNewMessage) View.VISIBLE else View.GONE

            if (!user.avatarUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(user.avatarUrl)
                    .placeholder(R.drawable.ic_userpic)
                    .into(avatarImageView)
            } else {
                avatarImageView.setImageResource(R.drawable.ic_userpic)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val isNewMessage = currentUserNewMessages.contains(user.uid)
        holder.bind(user, isNewMessage)

        if (!animatedPositions.contains(position)) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 50f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
            animatedPositions.add(position)
        }

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}

