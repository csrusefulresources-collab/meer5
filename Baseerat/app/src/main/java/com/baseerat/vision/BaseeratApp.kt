package com.baseerat.vision

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.baseerat.vision.data.SettingsRepository
import com.baseerat.vision.util.SpeechHelper

class BaseeratApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var speechHelper: SpeechHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        speechHelper = SpeechHelper(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_OVERLAY,
                getString(R.string.notification_channel_overlay),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_overlay_text)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY = "baseerat_overlay"
        lateinit var instance: BaseeratApp
            private set
    }
}
