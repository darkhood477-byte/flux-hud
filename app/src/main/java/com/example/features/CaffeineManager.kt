package com.example.features

import android.content.Context
import android.os.PowerManager

class CaffeineManager(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    val isActive: Boolean
        get() = wakeLock?.isHeld == true

    fun toggle() {
        if (isActive) {
            wakeLock?.release()
            wakeLock = null
        } else {
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "OrbsMobile::CaffeineWakeLock"
            )
            wakeLock?.acquire()
        }
    }
}
