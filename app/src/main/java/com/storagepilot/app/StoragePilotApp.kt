package com.storagepilot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StoragePilotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val scanChannel = NotificationChannel(
                CHANNEL_SCAN,
                getString(R.string.scan_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.scan_channel_desc)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(scanChannel)
        }
    }

    companion object {
        const val CHANNEL_SCAN = "scanning_service"
    }
}
