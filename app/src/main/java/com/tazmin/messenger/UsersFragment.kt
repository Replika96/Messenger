package com.tazmin.messenger

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class UsersFragment : Fragment(R.layout.fragment_users) {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var adapter: UsersAdapter
    private var usersList = mutableListOf<User>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        usersRecyclerView = view.findViewById(R.id.usersRecyclerView)
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        usersRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        adapter = UsersAdapter(usersList, currentUserNewMessages) { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("userName", user.username)
            intent.putExtra("userId", user.uid)


            removeNewMessageFlag(user.uid)
            startActivity(intent)
        }
        usersRecyclerView.adapter = adapter

        fetchUsers()
    }
    private fun removeNewMessageFlag(senderId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserId)
            .update("newMessageFrom", FieldValue.arrayRemove(senderId))
            .addOnSuccessListener {
                Log.d("UsersFragment", "Флаг нового сообщения удален для пользователя $senderId")
            }
            .addOnFailureListener { e ->
                Log.e("UsersFragment", "Ошибка при удалении флага: ${e.message}")
            }
    }

    private fun fetchUsers() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUserId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("UsersFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val newMessageFrom = documentSnapshot?.get("newMessageFrom") as? List<String> ?: emptyList()

                db.collection("users")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val users = snapshot.documents.mapNotNull { document ->
                            val uid = document.id
                            if (uid != currentUserId) {
                                val username = document.getString("username") ?: return@mapNotNull null
                                val avatarUrl = document.getString("avatarUrl")
                                User(uid, username, avatarUrl = avatarUrl)
                            } else {
                                null
                            }
                        }

                        usersList.clear()
                        usersList.addAll(users)
                        currentUserNewMessages.clear()
                        currentUserNewMessages.addAll(newMessageFrom)
                        adapter.notifyDataSetChanged()
                    }
            }
    }


    companion object {
        private val currentUserNewMessages = mutableSetOf<String>()
    }
}

