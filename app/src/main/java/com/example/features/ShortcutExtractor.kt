package com.example.features

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.graphics.drawable.Drawable

object ShortcutExtractor {
    data class ExtractedShortcut(
        val id: String,
        val shortLabel: CharSequence?,
        val packageName: String,
        val type: String = "APP_SHORTCUT",
        val icon: Drawable? = null
    )

    fun getShortcutsForPackage(context: Context, packageName: String): List<ExtractedShortcut> {
        val shortcuts = mutableListOf<ExtractedShortcut>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            try {
                if (launcherApps.hasShortcutHostPermission()) {
                    val query = LauncherApps.ShortcutQuery()
                    query.setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or 
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                    query.setPackage(packageName)
                    val userHandle = Process.myUserHandle()
                    val result = launcherApps.getShortcuts(query, userHandle)
                    result?.forEach { shortcutInfo ->
                        shortcuts.add(
                            ExtractedShortcut(
                                id = shortcutInfo.id,
                                shortLabel = shortcutInfo.shortLabel ?: shortcutInfo.id,
                                packageName = shortcutInfo.getPackage(),
                                icon = try { launcherApps.getShortcutIconDrawable(shortcutInfo, context.resources.displayMetrics.densityDpi) } catch (e: Exception) { null }
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return shortcuts
    }
}
