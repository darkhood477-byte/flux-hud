import re

with open('/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.scale
import com.example.overlay.VibrationHelper
"""

content = content.replace('import kotlin.math.sqrt', 'import kotlin.math.sqrt\n' + imports)

# Add variables at the start of RadialMenuOverlay
vars_addition = """
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
"""

content = re.sub(r'var lastInteractionTime by remember \{ mutableStateOf\(System\.currentTimeMillis\(\)\) \}',
                 r'var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }\n' + vars_addition,
                 content)

# Add haptic ticks to actions
content = content.replace('onDismiss()', 'vibratorHelper.click()\n                onDismiss()')
# Clean up any double vibratorHelper calls if they already existed (none should)

# Outer ring animation
animation_addition = """
    val outerRingScale by animateFloatAsState(
        targetValue = if (lockedInnerIdx != null) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
"""
content = re.sub(r'var touchPos by remember \{ mutableStateOf<Offset\?>\(null\) \}',
                 r'var touchPos by remember { mutableStateOf<Offset?>(null) }\n' + animation_addition,
                 content)


# Modify pointer event loop to track haptics and center long press
pointer_replace = """
                        if (change.pressed) {
                            val isDown = !change.previousPressed
                            touchPos = change.position
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

                                    if (isDown) {
                                        val maxActiveRadius = if (lockedInnerIdx != null) rOuterEnd else rInnerEnd
                                        if (dist > maxActiveRadius) {
                                            lockedInnerIdx = null
                                            vibratorHelper.click()
                                            onDismiss()
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
                            val pos = touchPos
                            touchPos = null
                            val currentLockedIdx = lockedInnerIdx
                            lockedInnerIdx = null
                            
                            prevHapticRing = -1
                            prevHapticIdx = -1
                            
                            if (activeUtility == null && pos != null) {
"""
content = re.sub(r'if \(change\.pressed\) \{.*?if \(activeUtility == null && pos != null\) \{',
                 pointer_replace,
                 content,
                 flags=re.DOTALL)

# Now, implement scale logic for outer ring
outer_draw_replace = """
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
    
                                val subBgColor = if (isOuterHovered) primary else surfaceVariant.copy(alpha = ringOpacity)
                                val subTextColor = if (isOuterHovered) onPrimary else onSurfaceVariant
    
                                drawArc(
                                    color = subBgColor,
                                    startAngle = oStart + 1f,
                                    sweepAngle = subSliceSweep - 2f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - rOuterMid, center.y - rOuterMid),
                                    size = Size(rOuterMid * 2, rOuterMid * 2),
                                    style = Stroke(width = outerThickness)
                                )
    
                                val oMidAngleRad = Math.toRadians((oStart + subSliceSweep / 2f).toDouble())
                                val subTextX = center.x + rOuterMid * cos(oMidAngleRad).toFloat()
                                val subTextY = center.y + rOuterMid * sin(oMidAngleRad).toFloat()
    
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
                                    topLeft = Offset(subTextX - subTextResult.size.width / 2f, subTextY - subTextResult.size.height / 2f)
                                )
                            }
                        }
                    }
                }
"""

content = re.sub(r'// 3\. Draw Outer Ring Slices \(Contextual Sub-menu\).*?(?=\s*\}\s*else if \(activeUtility == "QR"\) \{)',
                 outer_draw_replace,
                 content,
                 flags=re.DOTALL)


with open('/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt', 'w') as f:
    f.write(content)

