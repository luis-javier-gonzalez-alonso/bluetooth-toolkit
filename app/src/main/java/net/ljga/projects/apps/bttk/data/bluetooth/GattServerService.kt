package net.ljga.projects.apps.bttk.data.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import net.ljga.projects.apps.bttk.R
import net.ljga.projects.apps.bttk.ui.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class GattServerService : Service() {

    @Inject
    lateinit var bluetoothController: BluetoothController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME)
                startForegroundService(deviceName)
            }
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    private fun startForegroundService(deviceName: String?) {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        bluetoothController.startGattServer(deviceName)
    }

    private fun stopService() {
        bluetoothController.stopGattServer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GATT Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running GATT Server"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, GattServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("GATT server is active")
            .setContentText("TODO here we should add the name of the advertisement")
            .setContentIntent(mainActivityPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gatt_server_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME"
    }
}
