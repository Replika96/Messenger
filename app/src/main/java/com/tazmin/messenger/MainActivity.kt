package com.tazmin.messenger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var logoutButton: Button
    private lateinit var usernameText: TextView
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var adapter: UsersAdapter
    private var usersList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        if (auth.currentUser == null){
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
            return
        }

        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        usernameText = findViewById(R.id.usernameText)
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            val userId = user.uid
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    usernameText.text = username ?: "No name"
                } else {
                    usernameText.text = "No name"
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load username", Toast.LENGTH_SHORT).show()
            }
        }

        adapter = UsersAdapter(usersList) { user ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("userName", user.username)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }
        usersRecyclerView.adapter = adapter

        fetchUsers()
    }
//123123
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
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
