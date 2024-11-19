package com.tazmin.messenger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usernameText: TextView
    private lateinit var logoutButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        usernameText = view.findViewById(R.id.usernameText)
        logoutButton = view.findViewById(R.id.logoutButton)

        // Загрузка имени пользователя
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    usernameText.text = username ?: "No name"
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load username", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик кнопки выхода
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
