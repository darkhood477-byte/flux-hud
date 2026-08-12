#!/bin/bash
cat << 'INNER_EOF' > /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt.new
package com.example.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.features.CaffeineManager
import com.example.features.QuickTimerManager
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RadialSubItem(
    val id: String,
    val title: String,
    val action: () -> Unit
)

data class RadialItemData(
    val id: String,
    val title: String,
    val subItems: List<RadialSubItem> = emptyList(),
    val action: (() -> Unit)? = null
)

@Composable
fun RadialMenuOverlay(
    repository: AppRepository,
    caffeineManager: CaffeineManager,
    quickTimerManager: QuickTimerManager,
    onExecuteAction: (Int) -> Unit = {},
    settingsManager: com.example.data.SettingsManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vibratorHelper = remember { VibrationHelper(context) }
    
    val shortcuts by repository.shortcuts.collectAsState(initial = emptyList())
    val clipboardHistory by repository.clipboardHistory.collectAsState(initial = emptyList())
    val ringOpacity by settingsManager.ringOpacity.collectAsState()
    val labelStyle by settingsManager.labelStyle.collectAsState()
    val isImmersive by settingsManager.isImmersiveModeActive.collectAsState()
        
    var activeUtility by remember { mutableStateOf<String?>(null) }
    var pageIndex by remember { mutableStateOf(0) }
    var lockedInnerIdx by remember { mutableStateOf<Int?>(null) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var centerDownTime by remember { mutableStateOf(0L) }
    var isLongPressTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteractionTime) {
        delay(4000)
        onDismiss()
    }

    LaunchedEffect(centerDownTime, isLongPressTriggered) {
        if (centerDownTime > 0L && !isLongPressTriggered) {
            delay(500) // Long press duration
            vibratorHelper.heavyClick()
            settingsManager.setImmersiveMode(!isImmersive)
            isLongPressTriggered = true
        }
    }

    val clipboardSubItems = remember(clipboardHistory) {
        clipboardHistory.take(5).mapIndexed { index, item ->
            val text = if (item.text.length > 10) item.text.take(8) + ".." else item.text
            RadialSubItem("c_$index", text) {
                val clipData = ClipData.newPlainText("Clipboard history", item.text)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(clipData)
                vibratorHelper.click()
                onDismiss()
            }
        }
    }

    val actionRouter = remember { com.example.features.ActionRouter(context) }
    
    val shortcutsSubItems = remember(shortcuts) {
        (0 until 4).map { i ->
            val shortcut = shortcuts.find { it.id == i }
            if (shortcut != null && shortcut.type != com.example.data.ActionType.EMPTY) {
                val title = when (shortcut.type) {
                    com.example.data.ActionType.APP -> "App"
                    com.example.data.ActionType.DEEP_LINK -> "Link"
                    com.example.data.ActionType.URL -> "Web"
                    com.example.data.ActionType.BROADCAST -> "Task"
                    com.example.data.ActionType.UTILITY -> shortcut.target
                    else -> "Action"
                }
                RadialSubItem("s_$i", title) {
                    vibratorHelper.click()
                    if (shortcut.type == com.example.data.ActionType.UTILITY) {
                        activeUtility = shortcut.target
                    } else {
                        actionRouter.execute(shortcut.type, shortcut.target)
                        onDismiss()
                    }
                }
            } else {
                RadialSubItem("s_$i", "") {
                    // Inactive placeholder
                }
            }
        }
    }

    val page1Items = remember(shortcutsSubItems, caffeineManager, quickTimerManager, clipboardSubItems) {
        listOf(
            RadialItemData(
                id = "folders",
                title = "Folders",
                subItems = listOf(
                    RadialSubItem("f_down", "Downloads") { vibratorHelper.click(); onDismiss() },
                    RadialSubItem("f_doc", "Documents") { vibratorHelper.click(); onDismiss() },
                    RadialSubItem("f_pic", "Pictures") { vibratorHelper.click(); onDismiss() },
                    RadialSubItem("f_music", "Music") { vibratorHelper.click(); onDismiss() }
                )
            ),
            RadialItemData(
                id = "caffeine",
                title = "Caffeine",
                subItems = listOf(
                    RadialSubItem("caf_5", "5m") { vibratorHelper.click(); caffeineManager.toggle(); onDismiss() },
                    RadialSubItem("caf_15", "15m") { vibratorHelper.click(); caffeineManager.toggle(); onDismiss() },
                    RadialSubItem("caf_30", "30m") { vibratorHelper.click(); caffeineManager.toggle(); onDismiss() },
                    RadialSubItem("caf_inf", "Inf") { vibratorHelper.click(); caffeineManager.toggle(); onDismiss() }
                ),
                action = { vibratorHelper.click(); caffeineManager.toggle(); onDismiss() }
            ),
            RadialItemData(
                id = "timer",
                title = "Timer",
                subItems = listOf(
                    RadialSubItem("t_5", "5m") { vibratorHelper.click(); quickTimerManager.startTimer(5); onDismiss() },
                    RadialSubItem("t_15", "15m") { vibratorHelper.click(); quickTimerManager.startTimer(15); onDismiss() },
                    RadialSubItem("t_30", "30m") { vibratorHelper.click(); quickTimerManager.startTimer(30); onDismiss() },
                    RadialSubItem("t_60", "60m") { vibratorHelper.click(); quickTimerManager.startTimer(60); onDismiss() }
                ),
                action = { activeUtility = "Timer" }
            ),
            RadialItemData(
                id = "clipboard",
                title = "Clipboard",
                subItems = clipboardSubItems,
                action = { activeUtility = "Clipboard" }
            ),
            RadialItemData(
                id = "qr",
                title = "QR Scanner",
                action = { vibratorHelper.click(); activeUtility = "QR" }
            ),
            RadialItemData(
                id = "apps",
                title = "Shortcuts",
                subItems = shortcutsSubItems
            ),
            RadialItemData(
                id = "settings",
                title = "Settings",
                action = {
                    vibratorHelper.click()
                    val intent = Intent(context, com.example.MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onDismiss()
                }
            )
        )
    }

    val page2Items = remember {
        listOf(
            RadialItemData("p2_1", "Wi-Fi", action = { vibratorHelper.click(); onExecuteAction(0) }),
            RadialItemData("p2_2", "Bluetooth", action = { vibratorHelper.click(); onExecuteAction(1) }),
            RadialItemData("p2_3", "Flashlight", action = { vibratorHelper.click(); onExecuteAction(2) }),
            RadialItemData("p2_4", "Screenshot", action = { vibratorHelper.click(); onExecuteAction(3) }),
            RadialItemData("p2_5", "Do Not Disturb", action = { vibratorHelper.click(); onExecuteAction(4) }),
            RadialItemData("p2_6", "Rotation", action = { vibratorHelper.click(); onExecuteAction(5) }),
            RadialItemData(
                id = "p2_7",
                title = "Volume",
                subItems = listOf(
                    RadialSubItem("v_0", "0%") { vibratorHelper.click(); setVolume(context, 0f); onDismiss() },
                    RadialSubItem("v_25", "25%") { vibratorHelper.click(); setVolume(context, 0.25f); onDismiss() },
                    RadialSubItem("v_50", "50%") { vibratorHelper.click(); setVolume(context, 0.5f); onDismiss() },
                    RadialSubItem("v_75", "75%") { vibratorHelper.click(); setVolume(context, 0.75f); onDismiss() },
                    RadialSubItem("v_100", "100%") { vibratorHelper.click(); setVolume(context, 1f); onDismiss() }
                ),
                action = { vibratorHelper.click(); onExecuteAction(6) }
            ),
            RadialItemData(
                id = "p2_8",
                title = "Brightness",
                subItems = listOf(
                    RadialSubItem("b_0", "0%") { vibratorHelper.click(); setBrightness(context, 0); onDismiss() },
                    RadialSubItem("b_25", "25%") { vibratorHelper.click(); setBrightness(context, 64); onDismiss() },
                    RadialSubItem("b_50", "50%") { vibratorHelper.click(); setBrightness(context, 128); onDismiss() },
                    RadialSubItem("b_75", "75%") { vibratorHelper.click(); setBrightness(context, 192); onDismiss() },
                    RadialSubItem("b_100", "100%") { vibratorHelper.click(); setBrightness(context, 255); onDismiss() }
                ),
                action = { vibratorHelper.click(); onExecuteAction(7) }
            )
        )
    }

    val currentItems = if (pageIndex == 0) page1Items else page2Items
    val itemPainters = currentItems.map { androidx.compose.ui.graphics.vector.rememberVectorPainter(getIconForId(it.id)) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    var touchPos by remember { mutableStateOf<Offset?>(null) }
    
    var prevRing by remember { mutableStateOf(-1) }
    var prevInnerIdx by remember { mutableStateOf(-1) }
    
    // Calculate tracked state outside Canvas to drive spring animations
    val derivedTracking by remember(touchPos, lockedInnerIdx, currentItems) {
        derivedStateOf {
            val pos = touchPos ?: return@derivedStateOf null
            val screenWidth = 1000f // We don't have size here, but we can do math without it by passing center
            null // We will just use touchPos inside Canvas for drawing and updating state, wait, we can't update state in draw scope!
        }
    }
INNER_EOF
