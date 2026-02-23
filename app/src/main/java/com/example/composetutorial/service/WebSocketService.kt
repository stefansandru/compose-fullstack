package com.example.composetutorial.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.composetutorial.ComposeTutorialApplication
import com.example.composetutorial.R
import com.example.composetutorial.data.ItemRepository
import com.example.composetutorial.data.remote.ItemEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WebSocketService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: ItemRepository

    companion object {
        const val CHANNEL_ID = "websocket_channel"
        const val NOTIFICATION_ID = 100
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as ComposeTutorialApplication
        repository = app.container.repository
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WebSocketService", "Service starting...")
        startForeground(NOTIFICATION_ID, createNotification("Listening for updates..."))
        
        // Ensure WebSocket is connected
        serviceScope.launch {
            // We leverage the repository's logic. 
            // If the repository's token is set, it might already be connecting.
            // However, the repository scope might be tied to the ViewModel or Application?
            // Actually, in the current AppContainer, the repository is a singleton.
            // But its *internal* scope implementation in the previous file showed:
            // private val scope = CoroutineScope(Dispatchers.IO)
            // This scope is not lifecycle aware specifically, so it lives as long as the Repo lives (App lives).
            // But Android kills background apps. Foregound Service prevents that.
            
            // To be doubly sure, we can also explicitely collect events here or just rely on the Repository
            // doing the collection.
            
            // The repository implementation:
            // init { scope.launch { ... distinctUntilChanged().collect { ... startWebSocketListener() } } }
            // This means as long as the process is alive, the repository is alive and listening.
            // So our main job here is just To BE ALIVE.
            
            Log.d("WebSocketService", "Service kept alive for WebSocket")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("WebSocketService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Real-time Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Sync Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
