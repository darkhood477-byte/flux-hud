package com.example.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.ActionType

class ActionRouter(private val context: Context) {

    fun execute(type: String, target: String) {
        try {
            when (type) {
                ActionType.APP -> {
                    val intent = context.packageManager.getLaunchIntentForPackage(target)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionType.DEEP_LINK -> {
                    if (target == "share://prompt") {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply { setType("text/plain"); putExtra(Intent.EXTRA_TEXT, "Shared from Flux") }
                        val chooser = Intent.createChooser(shareIntent, "Share to...").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(chooser)
                        return
                    } else if (target == "note://new") {
                        try {
                           val noteIntent = Intent(Intent.ACTION_SEND).apply { setType("text/plain"); setPackage("com.google.android.keep"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                           context.startActivity(noteIntent)
                        } catch(e: Exception) {
                           Toast.makeText(context, "Note app not found", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }
                    val intent = Intent.parseUri(target, Intent.URI_INTENT_SCHEME)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                ActionType.SHORTCUT -> {
                    val parts = target.split("||")
                    if (parts.size == 2) {
                        val packageName = parts[0]
                        val shortcutId = parts[1]
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
                            try {
                                launcherApps.startShortcut(packageName, shortcutId, null, null, android.os.Process.myUserHandle())
                            } catch(e: Exception) { android.widget.Toast.makeText(context, "Shortcut failed", android.widget.Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
                ActionType.URL -> {
                    var url = target
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                ActionType.BROADCAST -> {
                    val intent = Intent(target)
                    context.sendBroadcast(intent)
                }
            }
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e("ActionRouter", "Activity not found for target: $target", e)
            Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ActionRouter", "Failed to execute action: $target", e)
            Toast.makeText(context, "Failed to execute action", Toast.LENGTH_SHORT).show()
        }
    }
}
