package com.milesseventh.ussmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat

class WebSocketService : Service() {
    private var webSocketServer: Server? = null

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val toast = {
            message: String ->
            Toast
                .makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                )
                .show()
        }

        if (bluetoothManager == null) {
            toast("Can't retrieve bluetooth manager")
            return
        }

        val platform = Platform(bluetoothManager)
        webSocketServer = Server(platform) { message: String ->
            Toast
                .makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                )
                .show()
        }.apply {
            start()
        }
        startForeground(1, createNotification())
    }

    override fun onDestroy() {
        webSocketServer?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "WebSocketServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WebSocket Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WebSocket Server Running")
            .setContentText("The WebSocket server is running in the foreground.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }
}