package com.example.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconForId(id: String): ImageVector {
    return when {
        id == "folders" -> Icons.Default.Folder
        id == "caffeine" -> Icons.Default.LocalCafe
        id == "timer" -> Icons.Default.Timer
        id == "clipboard" -> Icons.Default.ContentPaste
        id == "qr" -> Icons.Default.QrCodeScanner
        id == "floating" -> Icons.Default.OpenInNew
        id == "apps" -> Icons.Default.Apps
        id == "settings" -> Icons.Default.Settings
        id == "p2_1" -> Icons.Default.Wifi
        id == "p2_2" -> Icons.Default.Bluetooth
        id == "p2_3" -> Icons.Default.FlashlightOn
        id == "p2_4" -> Icons.Default.Screenshot
        id == "p2_5" -> Icons.Default.DoNotDisturbOn
        id == "p2_6" -> Icons.Default.ScreenRotation
        id == "p2_7" -> Icons.AutoMirrored.Filled.VolumeUp
        id == "p2_8" -> Icons.Default.BrightnessHigh
        id.startsWith("c_") -> Icons.Default.ContentPaste
        id.startsWith("s_") -> Icons.AutoMirrored.Filled.Launch
        id.startsWith("fl_") -> Icons.AutoMirrored.Filled.Launch
        id == "f_down" -> Icons.Default.Download
        id == "f_doc" -> Icons.Default.Description
        id == "f_pic" -> Icons.Default.Image
        id == "f_music" -> Icons.Default.LibraryMusic
        id.startsWith("caf_") -> Icons.Default.LocalCafe
        id.startsWith("t_") -> Icons.Default.Timer
        id.startsWith("v_") -> Icons.AutoMirrored.Filled.VolumeUp
        id.startsWith("b_") -> Icons.Default.BrightnessHigh
        else -> Icons.Default.Star
    }
}
