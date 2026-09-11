package com.deepseek.dshmobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.deepseek.dshmobile.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DshEngineService : Service() {

    companion object {
        private const val TAG = "DshEngineService"
        private const val CHANNEL_ID = "dsh_engine_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.deepseek.dshmobile.ACTION_START"
        const val ACTION_STOP = "com.deepseek.dshmobile.ACTION_STOP"
        const val ACTION_STATUS = "com.deepseek.dshmobile.ACTION_STATUS"

        fun isRunning(): Boolean = DshEngineManager.isRunning
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须尽快进入前台，否则 Android 8+ 会直接杀进程
        startForeground(NOTIFICATION_ID, buildNotification("启动中..."))

        when (intent?.action) {
            ACTION_STOP -> {
                stopEngine()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STATUS -> sendStatusBroadcast()
            else -> startEngine()
        }
        return START_STICKY
    }

    private fun startEngine() {
        serviceScope.launch {
            try {
                val manager = DshEngineManager.get(this@DshEngineService)
                val success = manager.initialize()
                if (success) {
                    updateNotification("引擎已启动", true)
                    sendStatusBroadcast()
                } else {
                    updateNotification("启动失败（缺少引擎二进制文件）", false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Engine start failed", e)
                updateNotification("启动异常: ${e.message}", false)
            }
        }
    }

    private fun stopEngine() {
        serviceScope.launch {
            DshEngineManager.get(this@DshEngineService).stop()
            Log.i(TAG, "Engine stopped")
        }
    }

    private fun updateNotification(text: String, isRunning: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, isRunning))
    }

    private fun buildNotification(text: String, isRunning: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DeepSeek Harness")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunning)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Harness 引擎服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示 DeepSeek Harness 引擎运行状态"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun sendStatusBroadcast() {
        val intent = Intent("com.deepseek.dshmobile.ENGINE_STATUS")
        intent.putExtra("running", DshEngineManager.isRunning)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        DshEngineManager.get(this).stop()
        Log.i(TAG, "Service destroyed")
    }
}
