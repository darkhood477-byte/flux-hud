@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import android.content.ClipDescription
import android.view.View
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.overlay.VibrationHelper


data class RadialMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val subItems: List<RadialMenuItem> = emptyList(),
    val action: (() -> Unit)? = null
) {
    constructor(id: String, title: String, action: () -> Unit) : this(
        id = id,
        title = title,
        icon = null,
        subItems = emptyList(),
        action = action
    )

    constructor(id: String, title: String, icon: ImageVector, action: () -> Unit) : this(
        id = id,
        title = title,
        icon = icon,
        subItems = emptyList(),
        action = action
    )
}


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
    val shortcuts by repository.shortcuts.collectAsState(initial = emptyList())
    val clipboardHistory by repository.clipboardHistory.collectAsState(initial = emptyList())
    val ringOpacity by settingsManager.ringOpacity.collectAsState()
    val labelStyle by settingsManager.labelStyle.collectAsState()
    
    var activeUtility by remember { mutableStateOf<String?>(null) }
    var pageIndex by remember { mutableStateOf(0) }
    var lockedInnerIdx by remember { mutableStateOf<Int?>(null) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val vibratorHelper = remember { VibrationHelper(context) }
    val isImmersive by settingsManager.isImmersiveModeActive.collectAsState()
    
    var centerDownTime by remember { mutableStateOf(0L) }
    var isLongPressTriggered by remember { mutableStateOf(false) }
    
    var prevHapticRing by remember { mutableStateOf(-1) }
    var prevHapticIdx by remember { mutableStateOf(-1) }

    LaunchedEffect(centerDownTime, isLongPressTriggered) {
        if (centerDownTime > 0L && !isLongPressTriggered) {
            delay(500)
            vibratorHelper.heavyClick()
            settingsManager.setImmersiveMode(!isImmersive)
            isLongPressTriggered = true
        }
    }


    androidx.compose.runtime.LaunchedEffect(lastInteractionTime) {
        kotlinx.coroutines.delay(4000)
        vibratorHelper.click()
                onDismiss()
    }

    val clipboardSubItems = remember(clipboardHistory) {
        clipboardHistory.take(5).mapIndexed { index, item ->
            val text = if (item.text.length > 10) item.text.take(8) + ".." else item.text
            RadialMenuItem("c_$index", text) {
                val isUri = item.text.startsWith("content://") || item.text.startsWith("file://")
                val clipData = if (isUri) {
                    val parsedUri = Uri.parse(item.text)
                        val uri = if (parsedUri.scheme == "file") {
                            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(parsedUri.path ?: ""))
                        } else {
                            parsedUri
                        }
                    val mimeType = context.contentResolver.getType(uri) ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(android.webkit.MimeTypeMap.getFileExtensionFromUrl(item.text)) ?: "*/*"
                    ClipData(ClipDescription("Dragged File", arrayOf(mimeType)), ClipData.Item(uri))
                } else {
                    ClipData.newPlainText("Clipboard history", item.text)
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(clipData)
                vibratorHelper.click()
                onDismiss()
            }
        }
    }

    val actionRouter = remember { com.example.features.ActionRouter(context) }
    val systemActionManager = remember { com.example.features.SystemActionManager(context) }

    val floatingAppsSubItems = remember {
        listOf(
            RadialMenuItem("fl_wa", "WhatsApp") {
                systemActionManager.launchThirdPartyAppAsFloating(context, "com.whatsapp")
                vibratorHelper.click()
                onDismiss()
            },
            RadialMenuItem("fl_tg", "Telegram") {
                systemActionManager.launchThirdPartyAppAsFloating(context, "org.telegram.messenger")
                vibratorHelper.click()
                onDismiss()
            },
            RadialMenuItem("fl_ch", "Chrome") {
                systemActionManager.launchThirdPartyAppAsFloating(context, "com.android.chrome")
                vibratorHelper.click()
                onDismiss()
            },
            RadialMenuItem("fl_yt", "YouTube") {
                systemActionManager.launchThirdPartyAppAsFloating(context, "com.google.android.youtube")
                vibratorHelper.click()
                onDismiss()
            }
        )
    }
    
    val shortcutsSubItems = remember(shortcuts) {
        (0 until 4).map { i ->
            val shortcut = shortcuts.find { it.id == i }
            if (shortcut != null && shortcut.type != com.example.data.ActionType.EMPTY) {
                val title = when (shortcut.type) {
                    com.example.data.ActionType.APP -> "App"
                    com.example.data.ActionType.SHORTCUT -> "Shortcut"
                    com.example.data.ActionType.DEEP_LINK -> "Link"
                    com.example.data.ActionType.URL -> "Web"
                    com.example.data.ActionType.BROADCAST -> "Task"
                    com.example.data.ActionType.UTILITY -> shortcut.target
                    else -> "Action"
                }
                RadialMenuItem("s_$i", title) {
                    if (shortcut.type == com.example.data.ActionType.UTILITY) {
                        activeUtility = shortcut.target
                    } else {
                        actionRouter.execute(shortcut.type, shortcut.target)
                        vibratorHelper.click()
                onDismiss()
                    }
                }
            } else {
                RadialMenuItem("s_$i", "") {
                    // Inactive placeholder
                }
            }
        }
    }

    val hudItemsOrder by settingsManager.hudItems.collectAsState()

    val allItemsMap = remember(shortcutsSubItems, caffeineManager, quickTimerManager, clipboardSubItems, floatingAppsSubItems) {
        listOf(
            RadialMenuItem(
                id = "folders",
                title = "Folders",
                subItems = listOf(
                    RadialMenuItem("f_down", "Downloads") { vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("f_doc", "Documents") { vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("f_pic", "Pictures") { vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("f_music", "Music") { vibratorHelper.click()
                onDismiss() }
                )
            ),
            RadialMenuItem(
                id = "floating",
                title = "Floating Apps",
                subItems = floatingAppsSubItems
            ),
            RadialMenuItem(
                id = "caffeine",
                title = "Caffeine",
                subItems = listOf(
                    RadialMenuItem("caf_5", "5m") { caffeineManager.toggle(); vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("caf_15", "15m") { caffeineManager.toggle(); vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("caf_30", "30m") { caffeineManager.toggle(); vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("caf_60", "1h") { caffeineManager.toggle(); vibratorHelper.click()
                onDismiss() }
                ),
                action = { caffeineManager.toggle(); vibratorHelper.click()
                onDismiss() }
            ),
            RadialMenuItem(
                id = "timer",
                title = "Timer",
                subItems = listOf(
                    RadialMenuItem("t_5", "5m") { quickTimerManager.startTimer(5); vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("t_15", "15m") { quickTimerManager.startTimer(15); vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("t_30", "30m") { quickTimerManager.startTimer(30); vibratorHelper.click()
                onDismiss() },
                    RadialMenuItem("t_60", "1h") { quickTimerManager.startTimer(60); vibratorHelper.click()
                onDismiss() }
                )
            ),
            RadialMenuItem(
                id = "clipboard",
                title = "Clipboard",
                subItems = clipboardSubItems.ifEmpty {
                    listOf(RadialMenuItem("c_empty", "Empty") { vibratorHelper.click()
                onDismiss() })
                },
                action = { activeUtility = "Clipboard" }
            ),
            RadialMenuItem(
                id = "qr",
                title = "QR Scanner",
                action = { activeUtility = "QR" }
            ),
            RadialMenuItem(
                id = "apps",
                title = "Shortcuts",
                subItems = shortcutsSubItems
            ),
            RadialMenuItem(
                id = "settings",
                title = "Settings",
                action = {
                    val intent = Intent(context, com.example.MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    vibratorHelper.click()
                onDismiss()
                }
            ),
            RadialMenuItem("p2_1", "Wi-Fi", icon = Icons.Default.Wifi, action = { onExecuteAction(0) }),
            RadialMenuItem("p2_2", "Bluetooth", icon = Icons.Default.Bluetooth, action = { onExecuteAction(1) }),
            RadialMenuItem("p2_3", "Flashlight", icon = Icons.Default.FlashlightOn, action = { onExecuteAction(2) }),
            RadialMenuItem("p2_4", "Screenshot", icon = Icons.Default.Screenshot, action = { onExecuteAction(3) }),
            RadialMenuItem("p2_5", "Do Not Disturb", icon = Icons.Default.DoNotDisturbOn, action = { onExecuteAction(4) }),
            RadialMenuItem("p2_6", "Rotation", icon = Icons.Default.ScreenRotation, action = { onExecuteAction(5) }),
            RadialMenuItem(
                id = "p2_7",
                title = "Volume",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                subItems = listOf(
                    RadialMenuItem("v_0", "0%", icon = Icons.AutoMirrored.Filled.VolumeMute) { setVolume(context, 0f); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("v_25", "25%", icon = Icons.AutoMirrored.Filled.VolumeDown) { setVolume(context, 0.25f); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("v_50", "50%", icon = Icons.AutoMirrored.Filled.VolumeDown) { setVolume(context, 0.5f); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("v_75", "75%", icon = Icons.AutoMirrored.Filled.VolumeUp) { setVolume(context, 0.75f); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("v_100", "100%", icon = Icons.AutoMirrored.Filled.VolumeUp) { setVolume(context, 1f); vibratorHelper.click(); onDismiss() }
                ),
                action = { onExecuteAction(6) }
            ),
            RadialMenuItem(
                id = "p2_8",
                title = "Brightness",
                icon = Icons.Default.BrightnessHigh,
                subItems = listOf(
                    RadialMenuItem("b_0", "0%", icon = Icons.Default.BrightnessLow) { setBrightness(context, 0); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("b_25", "25%", icon = Icons.Default.BrightnessMedium) { setBrightness(context, 64); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("b_50", "50%", icon = Icons.Default.BrightnessMedium) { setBrightness(context, 128); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("b_75", "75%", icon = Icons.Default.BrightnessHigh) { setBrightness(context, 192); vibratorHelper.click(); onDismiss() },
                    RadialMenuItem("b_100", "100%", icon = Icons.Default.BrightnessHigh) { setBrightness(context, 255); vibratorHelper.click(); onDismiss() }
                ),
                action = { onExecuteAction(7) }
            )
        ).associateBy { it.id }
    }

    val currentItems = remember(pageIndex, hudItemsOrder, allItemsMap) {
        val pageIds = if (pageIndex == 0) hudItemsOrder.take(8) else hudItemsOrder.drop(8).take(8)
        pageIds.mapNotNull { allItemsMap[it] }
    }

    val itemPainters = currentItems.map { item ->
        val vector = item.icon ?: getIconForId(item.id)
        androidx.compose.runtime.key(item.id) {
            rememberVectorPainter(vector)
        }
    }
    val subItemPainters = currentItems.map { item ->
        item.subItems.map { subItem ->
            val vector = subItem.icon ?: getIconForId(subItem.id)
            androidx.compose.runtime.key(subItem.id) {
                rememberVectorPainter(vector)
            }
        }
    }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val accentColorHex by settingsManager.accentColor.collectAsState()
    
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val defaultOnPrimary = MaterialTheme.colorScheme.onPrimary
    
    val primary = remember(accentColorHex, defaultPrimary) {
        if (accentColorHex == "DEFAULT") defaultPrimary else try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(accentColorHex))
        } catch(e: Exception) { defaultPrimary }
    }
    
    val onPrimary = remember(primary, defaultOnPrimary) {
        if (accentColorHex == "DEFAULT") defaultOnPrimary else {
            // A simple luminance check for contrast could go here, but for now we'll just use white or black
            val luminance = (0.299 * primary.red + 0.587 * primary.green + 0.114 * primary.blue)
            if (luminance > 0.5) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
        }
    }

    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var initialTouchPos by remember { mutableStateOf<Offset?>(null) }

    val outerRingScale by animateFloatAsState(
        targetValue = if (lockedInnerIdx != null) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val centerScale by animateFloatAsState(
        targetValue = if (prevHapticRing == 0) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val innerScales = currentItems.indices.map { i ->
        val isHovered = (prevHapticRing == 1 && prevHapticIdx == i) || (prevHapticRing == 2 && lockedInnerIdx == i)
        animateFloatAsState(
            targetValue = if (isHovered) 1.15f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ).value
    }

    val maxSubItems = currentItems.maxOfOrNull { it.subItems.size } ?: 0
    val outerScales = (0 until maxSubItems).map { j ->
        val isHovered = (prevHapticRing == 2 && prevHapticIdx == j)
        animateFloatAsState(
            targetValue = if (isHovered) 1.15f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ).value
    }



    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
    }

    val glassColor = Color.Black.copy(alpha = 0.4f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .alpha(entryAnim.value)
            .scale(0.8f + 0.2f * entryAnim.value)
            .pointerInput(pageIndex) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        
                        lastInteractionTime = System.currentTimeMillis()
                        
                        
                        if (change.pressed) {
                            val isDown = !change.previousPressed
                            touchPos = change.position
                            if (isDown) {
                                initialTouchPos = change.position
                            }
                            val pos = change.position
                            if (activeUtility == null) {
                                val screenWidth = size.width.toFloat()
                                val screenHeight = size.height.toFloat()
                                val center = Offset(screenWidth / 2f, screenHeight / 2f)

                                val dx = pos.x - center.x
                                val dy = pos.y - center.y
                                val dist = sqrt(dx * dx + dy * dy)

                                with(density) {
                                    val rCenter = 65.dp.toPx()
                                    val rInnerStart = 70.dp.toPx()
                                    val rInnerEnd = 145.dp.toPx()
                                    val rOuterStart = 150.dp.toPx()
                                    val rOuterEnd = 215.dp.toPx()
                                    
                                    var currentRing = -1
                                    var currentIdx = -1
                                    
                                    if (dist < rCenter) {
                                        currentRing = 0
                                        if (isDown) {
                                            centerDownTime = System.currentTimeMillis()
                                            isLongPressTriggered = false
                                        }
                                    } else {
                                        if (isDown) {
                                            centerDownTime = 0L
                                            isLongPressTriggered = false
                                        }
                                        if (dist in rInnerStart..rInnerEnd) {
                                            currentRing = 1
                                        } else if (dist in rOuterStart..rOuterEnd) {
                                            currentRing = 2
                                        }
                                    }



                                    if (dist in rInnerStart..rOuterEnd) {
                                        var angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                                        if (angleDeg < 0) angleDeg += 360f
                                        val normAngle = (angleDeg + 90f) % 360f

                                        val numInner = currentItems.size
                                        val innerSweep = 360f / numInner
                                        
                                        val hoveredInnerIdx = (normAngle / innerSweep).toInt().coerceIn(0, numInner - 1)
                                        val innerItem = currentItems[hoveredInnerIdx]
                                        
                                        if (currentRing == 1) {
                                            currentIdx = hoveredInnerIdx
                                            if (innerItem.subItems.isNotEmpty()) {
                                                lockedInnerIdx = hoveredInnerIdx
                                            }
                                        } else if (currentRing == 2) {
                                            if (lockedInnerIdx != null) {
                                                val subItems = currentItems[lockedInnerIdx!!].subItems
                                                if (subItems.isNotEmpty()) {
                                                    val innerCenterAngle = -90f + lockedInnerIdx!! * innerSweep + innerSweep / 2f
                                                    val outerSweep = maxOf(60f, subItems.size * 35f)
                                                    val outerStartAngle = innerCenterAngle - outerSweep / 2f
                    
                                                    var relAngle = (angleDeg - outerStartAngle) % 360f
                                                    if (relAngle < 0) relAngle += 360f
                    
                                                    if (relAngle in 0f..outerSweep) {
                                                        currentIdx = (relAngle / (outerSweep / subItems.size)).toInt().coerceIn(0, subItems.size - 1)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Haptic Logic
                                    if (currentRing != prevHapticRing && currentRing != -1) {
                                        if (prevHapticRing != -1) {
                                            vibratorHelper.heavyClick()
                                        }
                                        prevHapticRing = currentRing
                                        prevHapticIdx = currentIdx
                                    } else if (currentRing != -1 && currentIdx != prevHapticIdx) {
                                        vibratorHelper.tick()
                                        prevHapticIdx = currentIdx
                                    }
                                }
                            }
                        } else {
                            centerDownTime = 0L
                            val pos = touchPos ?: change.position
                            val initial = initialTouchPos ?: pos
                            
                            val dxDrag = pos.x - initial.x
                            val dyDrag = pos.y - initial.y
                            
                            initialTouchPos = null
                            touchPos = null
                            val currentLockedIdx = lockedInnerIdx
                            lockedInnerIdx = null
                            
                            prevHapticRing = -1
                            prevHapticIdx = -1
                            
                            if (activeUtility == null) {

                                val screenWidth = size.width.toFloat()
                                val screenHeight = size.height.toFloat()
                                val center = Offset(screenWidth / 2f, screenHeight / 2f)

                                val dx = pos.x - center.x
                                val dy = pos.y - center.y
                                val dist = sqrt(dx * dx + dy * dy)

                                with(density) {
                                    val rCenter = 65.dp.toPx()
                                    val rInnerStart = 70.dp.toPx()
                                    val rInnerEnd = 145.dp.toPx()
                                    val rOuterStart = 150.dp.toPx()
                                    val rOuterEnd = 215.dp.toPx()

                                    val maxActiveRadius = if (currentLockedIdx != null) rOuterEnd else rInnerEnd

                                    if (dist > maxActiveRadius) {
                                        if (abs(dxDrag) > 100f && abs(dxDrag) > abs(dyDrag)) {
                                            if (dxDrag > 0) {
                                                pageIndex = (pageIndex - 1 + 2) % 2
                                            } else {
                                                pageIndex = (pageIndex + 1) % 2
                                            }
                                            vibratorHelper.click()
                                        } else {
                                            vibratorHelper.click()
                                            onDismiss()
                                        }
                                    } else if (dist < rCenter) {
                                        pageIndex = (pageIndex + 1) % 2
                                    } else {
                                        var angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                                        if (angleDeg < 0) angleDeg += 360f
                                        val normAngle = (angleDeg + 90f) % 360f

                                        val numInner = currentItems.size
                                        val innerSweep = 360f / numInner
                                        
                                        val hoveredInnerIdx = currentLockedIdx ?: (normAngle / innerSweep).toInt().coerceIn(0, numInner - 1)
                                        val innerItem = currentItems[hoveredInnerIdx]

                                        if (dist in rInnerStart..rInnerEnd) {
                                            if (innerItem.subItems.isEmpty()) {
                                                innerItem.action?.invoke() ?: vibratorHelper.click()
                onDismiss()
                                            } else {
                                                lockedInnerIdx = hoveredInnerIdx
                                            }
                                        } else if (dist in rOuterStart..rOuterEnd) {
                                            val subItems = innerItem.subItems
                                            if (subItems.isNotEmpty()) {
                                                val innerCenterAngle = -90f + hoveredInnerIdx * innerSweep + innerSweep / 2f
                                                val outerSweep = maxOf(60f, subItems.size * 35f)
                                                val outerStartAngle = innerCenterAngle - outerSweep / 2f

                                                var relAngle = (angleDeg - outerStartAngle) % 360f
                                                if (relAngle < 0) relAngle += 360f

                                                if (relAngle in 0f..outerSweep) {
                                                    val subIdx = (relAngle / (outerSweep / subItems.size)).toInt().coerceIn(0, subItems.size - 1)
                                                    subItems[subIdx].action?.invoke()
                                                }
                                            } else {
                                                vibratorHelper.click()
                onDismiss()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (activeUtility == null) {
            if (currentItems.isNotEmpty() && lockedInnerIdx != null) {
                val subItems = currentItems[lockedInnerIdx!!].subItems
                if (subItems.isNotEmpty() && currentItems[lockedInnerIdx!!].id == "clipboard") {
                    val rOuterMid = with(density) { 182.5.dp.toPx() }
                    val innerSweep = 360f / currentItems.size
                    val innerCenterAngle = -90f + lockedInnerIdx!! * innerSweep + innerSweep / 2f
                    val outerSweep = maxOf(60f, subItems.size * 35f)
                    val outerStartAngle = innerCenterAngle - outerSweep / 2f
                    val subSliceSweep = outerSweep / subItems.size
                    val screenWidth = constraints.maxWidth.toFloat()
                    val screenHeight = constraints.maxHeight.toFloat()
                    val centerX = screenWidth / 2f
                    val centerY = screenHeight / 2f
                    subItems.forEachIndexed { j, subItem ->
                        val oStart = outerStartAngle + j * subSliceSweep
                        val oMidAngleRad = Math.toRadians((oStart + subSliceSweep / 2f).toDouble())
                        val subTextX = centerX + rOuterMid * kotlin.math.cos(oMidAngleRad).toFloat()
                        val subTextY = centerY + rOuterMid * kotlin.math.sin(oMidAngleRad).toFloat()
                        val boxSize = 60.dp
                        val offsetX = with(density) { subTextX.toDp() - boxSize / 2 }
                        val offsetY = with(density) { subTextY.toDp() - boxSize / 2 }
                        if (j < clipboardHistory.size) {
                            val item = clipboardHistory[j]
                            Box(
                                modifier = Modifier
                                    .offset(x = offsetX, y = offsetY)
                                    .size(boxSize)
                                    .dragAndDropSource {
                                        detectTapGestures(onLongPress = { _ ->
                                            val isUri = item.text.startsWith("content://") || item.text.startsWith("file://")
                                            val clipDataDrag = if (isUri) {
                                                val parsedUri = android.net.Uri.parse(item.text)
                                                val uri = if (parsedUri.scheme == "file") {
                                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(parsedUri.path ?: ""))
                                                } else {
                                                    parsedUri
                                                }
                                                android.content.ClipData.newUri(context.contentResolver, "Dragged File", uri)
                                            } else {
                                                android.content.ClipData.newPlainText("Clipboard history", item.text)
                                            }
                                            startTransfer(
                                                androidx.compose.ui.draganddrop.DragAndDropTransferData(
                                                    clipData = clipDataDrag,
                                                    flags = android.view.View.DRAG_FLAG_GLOBAL or android.view.View.DRAG_FLAG_GLOBAL_URI_READ
                                                )
                                            )
                                        })
                                    }
                            )
                        }
                    }
                }
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (pageIndex == 0) primary else glassColor, androidx.compose.foundation.shape.CircleShape)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (pageIndex == 1) primary else glassColor, androidx.compose.foundation.shape.CircleShape)
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val rCenter = 65.dp.toPx()
                val rInnerStart = 70.dp.toPx()
                val rInnerEnd = 145.dp.toPx()
                val rOuterStart = 150.dp.toPx()
                val rOuterEnd = 215.dp.toPx()

                val innerThickness = rInnerEnd - rInnerStart
                val outerThickness = rOuterEnd - rOuterStart

                val curTouch = touchPos
                var hoveredInnerIdx = lockedInnerIdx ?: -1
                var hoveredOuterIdx = -1
                var isTouchInCenter = false
                var isTouchInInner = false
                var isTouchInOuter = false

                if (curTouch != null) {
                    val dx = curTouch.x - center.x
                    val dy = curTouch.y - center.y
                    val dist = sqrt(dx * dx + dy * dy)

                    var angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                    if (angleDeg < 0) angleDeg += 360f
                    val normAngle = (angleDeg + 90f) % 360f

                    val numInner = currentItems.size
                    val innerSweep = 360f / numInner

                    if (dist < rCenter) {
                        isTouchInCenter = true
                    } else if (dist in rInnerStart..rInnerEnd) {
                        isTouchInInner = true
                        if (lockedInnerIdx == null) {
                            hoveredInnerIdx = (normAngle / innerSweep).toInt().coerceIn(0, numInner - 1)
                        }
                    } else if (dist in rOuterStart..rOuterEnd) {
                        isTouchInOuter = true
                        if (lockedInnerIdx == null) {
                            hoveredInnerIdx = (normAngle / innerSweep).toInt().coerceIn(0, numInner - 1)
                        }
                        
                        if (hoveredInnerIdx != -1) {
                            val subItems = currentItems[hoveredInnerIdx].subItems
                            if (subItems.isNotEmpty()) {
                                val innerCenterAngle = -90f + hoveredInnerIdx * innerSweep + innerSweep / 2f
                                val outerSweep = maxOf(60f, subItems.size * 35f)
                                val outerStartAngle = innerCenterAngle - outerSweep / 2f

                                var relAngle = (angleDeg - outerStartAngle) % 360f
                                if (relAngle < 0) relAngle += 360f

                                if (relAngle in 0f..outerSweep) {
                                    hoveredOuterIdx = (relAngle / (outerSweep / subItems.size)).toInt().coerceIn(0, subItems.size - 1)
                                }
                            }
                        }
                    }
                }

                // 1. Draw Center Hub
                scale(scale = centerScale, pivot = center) {
                    val centerBgColor = if (isTouchInCenter) primary else glassColor
                    drawCircle(
                        color = centerBgColor,
                        radius = rCenter,
                        center = center
                    )
                }

                // 2. Draw Inner Ring Slices
                val numInner = currentItems.size
                val innerSweep = 360f / numInner
                val rInnerMid = (rInnerStart + rInnerEnd) / 2f

                for (i in 0 until numInner) {
                    val startAngle = -90f + i * innerSweep
                    val isHovered = (hoveredInnerIdx == i) && (isTouchInInner || isTouchInOuter || lockedInnerIdx == i)

                    val sliceBgColor = if (isHovered) primary else glassColor.copy(alpha = ringOpacity)
                    val sliceTextColor = if (isHovered) onPrimary else onSurfaceVariant

                    // Trigonometric calculation for slice midpoint and icon placement
                    val radius = rInnerMid
                    val bisectingAngleDeg = startAngle + innerSweep / 2f
                    val angleInRadians = Math.toRadians(bisectingAngleDeg.toDouble()).toFloat()
                    val iconX = center.x + (radius * cos(angleInRadians))
                    val iconY = center.y + (radius * sin(angleInRadians))

                    val itemText = currentItems[i].title
                    val painter = itemPainters[i]
                    val iconSize = 24.dp.toPx()

                    val innerScale = innerScales.getOrElse(i) { 1f }
                    scale(scale = innerScale, pivot = Offset(iconX, iconY)) {
                        drawArc(
                            color = sliceBgColor,
                            startAngle = startAngle + 2f,
                            sweepAngle = innerSweep - 4f,
                            useCenter = false,
                            topLeft = Offset(center.x - rInnerMid, center.y - rInnerMid),
                            size = Size(rInnerMid * 2, rInnerMid * 2),
                            style = Stroke(width = innerThickness)
                        )

                        if (labelStyle == "ICONS_ONLY") {
                            translate(left = iconX - iconSize / 2f, top = iconY - iconSize / 2f) {
                                with(painter) {
                                    draw(Size(iconSize, iconSize), alpha = 1f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(sliceTextColor))
                                }
                            }
                        } else if (labelStyle == "WORDS_ONLY") {
                            val itemTextResult = textMeasurer.measure(
                                text = itemText,
                                style = TextStyle(
                                    color = sliceTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                            drawText(
                                textLayoutResult = itemTextResult,
                                topLeft = Offset(iconX - itemTextResult.size.width / 2f, iconY - itemTextResult.size.height / 2f)
                            )
                        } else {
                            translate(left = iconX - iconSize / 2f, top = iconY - iconSize) {
                                with(painter) {
                                    draw(Size(iconSize, iconSize), alpha = 1f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(sliceTextColor))
                                }
                            }
                            val itemTextResult = textMeasurer.measure(
                                text = itemText,
                                style = TextStyle(
                                    color = sliceTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                            drawText(
                                textLayoutResult = itemTextResult,
                                topLeft = Offset(iconX - itemTextResult.size.width / 2f, iconY)
                            )
                        }
                    } // End of scale
                }

                
                // 3. Draw Outer Ring Slices (Contextual Sub-menu)
                if (hoveredInnerIdx in 0 until numInner && outerRingScale > 0.01f) {
                    val hoveredItem = currentItems[hoveredInnerIdx]
                    val subItems = hoveredItem.subItems
                    if (subItems.isNotEmpty()) {
                        scale(scale = outerRingScale, pivot = center) {
                            val innerCenterAngle = -90f + hoveredInnerIdx * innerSweep + innerSweep / 2f
                            val outerSweep = maxOf(60f, subItems.size * 35f)
                            val outerStartAngle = innerCenterAngle - outerSweep / 2f
                            val subSliceSweep = outerSweep / subItems.size
    
                            val rOuterMid = (rOuterStart + rOuterEnd) / 2f
    
                            for (j in subItems.indices) {
                                val oStart = outerStartAngle + j * subSliceSweep
                                val isOuterHovered = (isTouchInOuter && hoveredOuterIdx == j)
    
                                val subBgColor = if (isOuterHovered) primary else glassColor.copy(alpha = ringOpacity)
                                val subTextColor = if (isOuterHovered) onPrimary else onSurfaceVariant

                                val radius = rOuterMid
                                val bisectingAngleDeg = oStart + subSliceSweep / 2f
                                val angleInRadians = Math.toRadians(bisectingAngleDeg.toDouble()).toFloat()
                                val iconX = center.x + (radius * cos(angleInRadians))
                                val iconY = center.y + (radius * sin(angleInRadians))
    
                                val outerScale = outerScales.getOrElse(j) { 1f }
                                scale(scale = outerScale, pivot = Offset(iconX, iconY)) {
    
                                    drawArc(
                                        color = subBgColor,
                                        startAngle = oStart + 2f,
                                        sweepAngle = subSliceSweep - 4f,
                                        useCenter = false,
                                        topLeft = Offset(center.x - rOuterMid, center.y - rOuterMid),
                                        size = Size(rOuterMid * 2, rOuterMid * 2),
                                        style = Stroke(width = outerThickness)
                                    )
    
                                    val subPainter = subItemPainters[hoveredInnerIdx][j]
                                    val subIconSize = 20.dp.toPx()

                                    if (labelStyle == "ICONS_ONLY") {
                                        translate(left = iconX - subIconSize / 2f, top = iconY - subIconSize / 2f) {
                                            with(subPainter) {
                                                draw(Size(subIconSize, subIconSize), alpha = 1f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(subTextColor))
                                            }
                                        }
                                    } else if (labelStyle == "WORDS_ONLY") {
                                        val subTextResult = textMeasurer.measure(
                                            text = subItems[j].title,
                                            style = TextStyle(
                                                color = subTextColor,
                                                fontSize = 10.sp,
                                                fontWeight = if (isOuterHovered) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                        drawText(
                                            textLayoutResult = subTextResult,
                                            topLeft = Offset(iconX - subTextResult.size.width / 2f, iconY - subTextResult.size.height / 2f)
                                        )
                                    } else {
                                        translate(left = iconX - subIconSize / 2f, top = iconY - subIconSize) {
                                            with(subPainter) {
                                                draw(Size(subIconSize, subIconSize), alpha = 1f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(subTextColor))
                                            }
                                        }
                                        val subTextResult = textMeasurer.measure(
                                            text = subItems[j].title,
                                            style = TextStyle(
                                                color = subTextColor,
                                                fontSize = 9.sp,
                                                fontWeight = if (isOuterHovered) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                        drawText(
                                            textLayoutResult = subTextResult,
                                            topLeft = Offset(iconX - subTextResult.size.width / 2f, iconY)
                                        )
                                    }
                                } // End of outer scale
                            }
                        }
                    }
                }

            }
        } else if (activeUtility == "QR") {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                CameraPreviewCenter(onBarcodeDetected = { barcodeValue ->
                    val clipData = ClipData.newPlainText("QR Code", barcodeValue)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(clipData)
                    vibratorHelper.click()
                onDismiss()
                })
            }
        } else if (activeUtility == "Clipboard") {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .widthIn(max = 280.dp)
            ) {
                items(clipboardHistory) { item ->
                    val isUri = item.text.startsWith("content://") || item.text.startsWith("file://")
                    val clipDataDrag = if (isUri) {
                        val parsedUri = Uri.parse(item.text)
                        val uri = if (parsedUri.scheme == "file") {
                            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(parsedUri.path ?: ""))
                        } else {
                            parsedUri
                        }
                        ClipData.newUri(context.contentResolver, "Dragged File", uri)
                    } else {
                        ClipData.newPlainText("Clipboard history", item.text)
                    }
                    
                    TextButton(
                        modifier = Modifier.dragAndDropSource {
                            detectTapGestures(onLongPress = {
                                startTransfer(
                                    DragAndDropTransferData(
                                        clipData = clipDataDrag,
                                        flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                                    )
                                )
                            })
                        },
                        onClick = {
                            val clipData = ClipData.newPlainText("Clipboard history", item.text)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(clipData)
                            vibratorHelper.click()
                onDismiss()
                        }
                    ) {
                        Text(text = item.text, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
    }
}

private fun setBrightness(context: Context, value: Int) {
    if (android.provider.Settings.System.canWrite(context)) {
        try {
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                value
            )
        } catch (e: Exception) {}
    }
}

private fun setVolume(context: Context, percent: Float) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (max * percent).toInt(), android.media.AudioManager.FLAG_SHOW_UI)
    } catch (e: Exception) {}
}

