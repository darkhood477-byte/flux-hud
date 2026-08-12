package com.example.features

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.services.FluxAccessibilityService

class SystemActionManager(private val context: Context) {

    private var isTorchOn = false

    fun launchThirdPartyAppAsFloating(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                // Force OS to destroy any cached full-screen instance and create a fresh task window
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                )
                
                intent.putExtra("android.intent.extra.WINDOW_MODE", 5)
                intent.putExtra("miui.intent.extra.IS_FREEFORM_WINDOW", true)

                val options = ActivityOptions.makeBasic()
                options.launchBounds = Rect(150, 200, 900, 1400)
                
                val displayId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    context.display?.displayId ?: 0
                } else {
                    0
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    options.launchDisplayId = displayId
                }

                try {
                    val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    method.invoke(options, 5) // 5 = WINDOWING_MODE_FREEFORM
                } catch (e: Exception) {
                    Log.e("FluxWindowMode", "Freeform reflection failed to execute", e)
                    // TODO: Implement Shizuku/Root integration to execute shell command if standard intents continue to be blocked
                    // val mainActivityName = intent.component?.className ?: ""
                    // executeShellCommand("am start --windowingMode 5 -n $packageName/$mainActivityName")
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                Toast.makeText(context, "App not installed: $packageName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FluxWindowMode", "Failed to launch floating app", e)
            Toast.makeText(context, "Failed to launch floating app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun executeAction(sliceIndex: Int) {
        when (sliceIndex) {
            0 -> toggleWifi()
            1 -> toggleBluetooth()
            2 -> toggleFlashlight()
            3 -> takeScreenshot()
            4 -> toggleDoNotDisturb()
            5 -> toggleRotation()
            6 -> adjustVolume()
            7 -> cycleBrightness()
            else -> {
                Toast.makeText(context, "Action $sliceIndex selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleWifi() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open Wi-Fi settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleBluetooth() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open Bluetooth settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
                Toast.makeText(context, if (isTorchOn) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun takeScreenshot() {
        val success = FluxAccessibilityService.takeScreenshot()
        if (!success) {
            Toast.makeText(context, "Enable Flux Accessibility Service for instant screenshots", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleDoNotDisturb() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(context, "Grant DND Permission", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val currentFilter = notificationManager.currentInterruptionFilter
            val newFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            notificationManager.setInterruptionFilter(newFilter)
            val msg = if (newFilter == NotificationManager.INTERRUPTION_FILTER_ALL) "DND OFF" else "DND ON"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRotation() {
        if (!Settings.System.canWrite(context)) {
            Toast.makeText(context, "Grant Write Settings permission", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val current = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )
            val newValue = if (current == 1) 0 else 1
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                newValue
            )
            val msg = if (newValue == 1) "Auto-Rotate ON" else "Auto-Rotate OFF"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun adjustVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Volume control error", Toast.LENGTH_SHORT).show()
        }
    }

    fun cycleBrightness() {
        if (!Settings.System.canWrite(context)) {
            Toast.makeText(context, "Grant Write Settings permission", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            try {
                val current = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                val nextBrightness = when {
                    current < 80 -> 128
                    current < 200 -> 255
                    else -> 40
                }
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    nextBrightness
                )
                Toast.makeText(context, "Brightness: ${(nextBrightness * 100) / 255}%", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Brightness error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
