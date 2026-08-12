package com.example.features

import android.content.ClipboardManager
import android.content.Context
import com.example.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClipboardMonitor(
    private val context: Context,
    private val repository: AppRepository
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        if (clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val uri = item.uri
                val text = item.text?.toString()
                
                val contentToSave = uri?.toString() ?: text
                
                if (!contentToSave.isNullOrBlank()) {
                    coroutineScope.launch {
                        repository.addClipboardItem(contentToSave)
                    }
                }
            }
        }
    }

    fun start() {
        clipboard.addPrimaryClipChangedListener(listener)
    }

    fun stop() {
        clipboard.removePrimaryClipChangedListener(listener)
    }
}
