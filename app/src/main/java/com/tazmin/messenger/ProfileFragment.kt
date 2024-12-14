package com.tazmin.messenger

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.min


class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usernameText: TextView
    private lateinit var logoutButton: Button
    private lateinit var avatarImageView: ImageView
    private lateinit var editUsernameField: EditText
    private lateinit var saveUsernameButton: Button
    private lateinit var editUsernameButton: ImageButton

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    lifecycleScope.launch {
                        uploadAvatarToImgur(selectedImageUri)
                    }
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        usernameText = view.findViewById(R.id.usernameText)
        avatarImageView = view.findViewById((R.id.avatarImageView))
        logoutButton = view.findViewById(R.id.logoutButton)
        editUsernameField = view.findViewById(R.id.editUsernameField)
        saveUsernameButton = view.findViewById(R.id.saveUsernameButton)
        editUsernameButton = view.findViewById(R.id.editUsername)

        // Загружаем информацию о пользователе
        loadUserInfo()

        avatarImageView.setOnClickListener {
            openGallery()
        }
        editUsernameButton.setOnClickListener {
            editUsernameField.visibility = View.VISIBLE
            saveUsernameButton.visibility = View.VISIBLE
            editUsernameField.setText(usernameText.text)
            usernameText.visibility = View.GONE
            editUsernameButton.visibility = View.GONE
        }
        saveUsernameButton.setOnClickListener {
            val newUsername = editUsernameField.text.toString()
            if (newUsername.isNotEmpty()) {
                updateUsername(newUsername)
            }
        }
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
    private fun updateUsername(newUsername: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .update("username", newUsername)
                .addOnSuccessListener {
                    usernameText.text = newUsername
                    usernameText.visibility = View.VISIBLE
                    saveUsernameButton.visibility = View.GONE
                    editUsernameField.visibility = View.GONE
                    editUsernameButton.visibility = View.VISIBLE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Ошибка при сохранении имени: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    val avatarUrl = document.getString("avatarUrl")

                    usernameText.text = username ?: "No name"
                    // Загрузка аватарки с помощью Glide
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_userpic)
                            .into(avatarImageView)
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        activityResultLauncher.launch(intent)
    }

    private suspend fun uploadAvatarToImgur(imageUri: Uri) {

        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading avatar...")
            setCancelable(false)
            show()
        }

        try {
            val bitmap = getBitmapFromUri(imageUri)?.let { resizeBitmap(it, 1024, 1024) }
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                return
            }

            val encodedImage = encodeImageToBase64(bitmap)

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", encodedImage)
                .build()

            val request = Request.Builder()
                .url("https://api.imgur.com/3/image")
                .addHeader("Authorization", "Client-ID ae061b5db48e23f")
                .post(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody!!)
                val avatarUrl = jsonResponse.getJSONObject("data").getString("link")
                saveAvatarToFirestore(avatarUrl)
            } else {
                Toast.makeText(requireContext(), "Failed to upload avatar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Imgur", "Error uploading avatar: ${e.message}")
            Toast.makeText(requireContext(), "Error uploading avatar", Toast.LENGTH_SHORT).show()
        } finally {
            progressDialog.dismiss()
        }
    }

    private fun saveAvatarToFirestore(avatarUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("avatarUrl", avatarUrl)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Avatar updated!", Toast.LENGTH_SHORT).show()
                loadUserInfo()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save avatar URL", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scale = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}


