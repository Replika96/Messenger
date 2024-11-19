package com.tazmin.messenger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UsersAdapter(usersList) { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("userName", user.username)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }
        usersRecyclerView.adapter = adapter

        fetchUsers()
    }

    private fun fetchUsers() {
        val currentUserId = auth.currentUser?.uid
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = db.collection("users").get().await()
                val users = querySnapshot.documents.mapNotNull { document ->
                    val uid = document.id
                    if (uid != currentUserId) {
                        val username = document.getString("username") ?: return@mapNotNull null
                        User(uid, username)
                    } else {
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    usersList.clear()
                    usersList.addAll(users)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

