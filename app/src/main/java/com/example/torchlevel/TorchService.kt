package com.example.torchlevel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.IBinder
import android.util.Log

/**
 * am start-foreground-service -n com.example.torchlevel/.TorchService --ei level 3
 * am start-foreground-service -n com.example.torchlevel/.TorchService --ez off true
 */
class TorchService : Service() {

    private val tag = "TorchLevel"
    private var cameraId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        val level = intent?.getIntExtra("level", -1) ?: -1
        val off = intent?.getBooleanExtra("off", false) ?: false
        val cm = getSystemService(CameraManager::class.java)

        try {
            val id = cm.cameraIdList.first {
                val c = cm.getCameraCharacteristics(it)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            cameraId = id

            if (off || level <= 0) {
                cm.setTorchMode(id, false)
                Log.i(tag, "torch off")
                stopSelf()
            } else {
                val max = cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                val applied = level.coerceIn(1, max)
                cm.turnOnTorchWithStrengthLevel(id, applied)
                Log.i(tag, "torch on: level $applied (requested $level, max $max)")
            }
        } catch (e: Exception) {
            Log.e(tag, "failed", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            val id = cameraId
            if (id != null) getSystemService(CameraManager::class.java).setTorchMode(id, false)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("torch", "Torch", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, "torch")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Torch level active")
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }
}
