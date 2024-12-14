package com.tazmin.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }

        remoteMessage.data.let {
            Log.d("FCM", "Data payload: $it")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "FCM-токен обновлен: $token")

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update("token", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Токен успешно обновлен для пользователя $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Ошибка обновления токена: ${e.message}")
                }
        }
    }


    /*private fun sendTokenToServer(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .update("token", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Токен успешно обновлен для пользователя $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Ошибка обновления токена: ${e.message}")
                }
        } else {
            Log.e("FCM", "Пользователь не авторизован, токен не может быть обновлен")
        }
    }*/


    private fun showNotification(title: String?, body: String?) {
        val channelId = "fcm_default_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FCM Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title ?: "Уведомление")
            .setContentText(body ?: "Новое сообщение")
            .setSmallIcon(R.drawable.ic_eye)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}





