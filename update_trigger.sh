#!/bin/bash
cat << 'INNER_EOF' > /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt.new
package com.example.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.features.CaffeineManager
import com.example.features.ClipboardMonitor
import com.example.features.QuickTimerManager
import com.example.features.SystemActionManager
import com.example.ui.theme.MyApplicationTheme

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var triggerComposeView: ComposeView? = null
    private var overlayComposeView: ComposeView? = null

    private lateinit var appRepository: AppRepository
    private lateinit var clipboardMonitor: ClipboardMonitor
    private lateinit var caffeineManager: CaffeineManager
    private lateinit var quickTimerManager: QuickTimerManager
    private lateinit var systemActionManager: SystemActionManager

    private lateinit var settingsManager: com.example.data.SettingsManager
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val database = AppDatabase.getDatabase(this)
        appRepository = AppRepository(database.shortcutDao(), database.clipboardDao())
        clipboardMonitor = ClipboardMonitor(this, appRepository)
        caffeineManager = CaffeineManager(this)
        quickTimerManager = QuickTimerManager(this)
        systemActionManager = SystemActionManager(this)
        settingsManager = com.example.data.SettingsManager(this)
        
        clipboardMonitor.start()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupTopRightTrigger()
        
        lifecycleScope.launch {
            settingsManager.isImmersiveModeActive.collectLatest { isImmersive ->
                updateTriggerPosition(isImmersive)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private var paramsTrigger: WindowManager.LayoutParams? = null

    private fun setupTopRightTrigger() {
        val density = resources.displayMetrics.density
        val sizePx = (52 * density).toInt()

        paramsTrigger = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        triggerComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setContent {
                MyApplicationTheme {
                    val isImmersive by settingsManager.isImmersiveModeActive.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isImmersive) 0.3f else 0.85f))
                            .clickable { toggleRadialMenu() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BlurOn,
                            contentDescription = "Flux HUD Trigger",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (isImmersive) 0.5f else 1.0f),
                            modifier = Modifier.size(if (isImmersive) 20.dp else 28.dp)
                        )
                    }
                }
            }
        }

        // Initialize position based on current state
        updateTriggerPosition(settingsManager.isImmersiveModeActive.value, true)
    }

    private fun updateTriggerPosition(isImmersive: Boolean, initial: Boolean = false) {
        val density = resources.displayMetrics.density
        val params = paramsTrigger ?: return
        
        if (isImmersive) {
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.x = 0
            params.y = (16 * density).toInt()
        } else {
            params.gravity = Gravity.TOP or Gravity.END
            params.x = (16 * density).toInt()
            params.y = (48 * density).toInt()
        }
        
        triggerComposeView?.let {
            if (initial) {
                windowManager.addView(it, params)
            } else {
                windowManager.updateViewLayout(it, params)
            }
        }
    }

    private fun toggleRadialMenu() {
        if (overlayComposeView != null) {
            hideRadialMenu()
        } else {
            showRadialMenu()
        }
    }

    private fun showRadialMenu() {
        if (overlayComposeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setContent {
                MyApplicationTheme {
                    RadialMenuOverlay(
                        repository = appRepository,
                        caffeineManager = caffeineManager,
                        quickTimerManager = quickTimerManager,
                        settingsManager = settingsManager,
                        onExecuteAction = { sliceIndex ->
                            executeAction(sliceIndex)
                        },
                        onDismiss = { hideRadialMenu() }
                    )
                }
            }
        }

        windowManager.addView(overlayComposeView, params)
    }

    private fun hideRadialMenu() {
        overlayComposeView?.let {
            windowManager.removeView(it)
            overlayComposeView = null
        }
    }

    fun executeAction(sliceIndex: Int) {
        systemActionManager.executeAction(sliceIndex)
        hideRadialMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardMonitor.stop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        triggerComposeView?.let {
            windowManager.removeView(it)
            triggerComposeView = null
        }
        hideRadialMenu()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
INNER_EOF
mv /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt.new /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt
