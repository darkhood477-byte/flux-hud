import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

# 1. Update background and introduce glassColor
target_bg = """    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))"""
replacement_bg = """    val glassColor = Color.Black.copy(alpha = 0.4f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)"""
content = content.replace(target_bg, replacement_bg)

# 2. Update Center Hub Drawing and remove text
target_hub = """                // 1. Draw Center Hub
                val centerBgColor = if (isTouchInCenter) primary else surfaceVariant
                val centerTextColor = if (isTouchInCenter) onPrimary else onSurfaceVariant
                drawCircle(
                    color = centerBgColor,
                    radius = rCenter,
                    center = center
                )
                drawCircle(
                    color = primary.copy(alpha = 0.5f),
                    radius = rCenter,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                val hubTitle = "Flux < ${pageIndex + 1}/2 >"
                val textResult = textMeasurer.measure(
                    text = hubTitle,
                    style = TextStyle(
                        color = centerTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(center.x - textResult.size.width / 2f, center.y - textResult.size.height / 2f)
                )"""
replacement_hub = """                // 1. Draw Center Hub
                val centerBgColor = if (isTouchInCenter) primary else glassColor
                drawCircle(
                    color = centerBgColor,
                    radius = rCenter,
                    center = center
                )"""
content = content.replace(target_hub, replacement_hub)

# 3. Inner Slices Gap and Color
target_inner_slice = """                    val sliceBgColor = if (isHovered) primary else surfaceVariant.copy(alpha = ringOpacity)
                    val sliceTextColor = if (isHovered) onPrimary else onSurfaceVariant

                    drawArc(
                        color = sliceBgColor,
                        startAngle = startAngle + 1f,
                        sweepAngle = innerSweep - 2f,
                        useCenter = false,
                        topLeft = Offset(center.x - rInnerMid, center.y - rInnerMid),
                        size = Size(rInnerMid * 2, rInnerMid * 2),
                        style = Stroke(width = innerThickness)
                    )"""
replacement_inner_slice = """                    val sliceBgColor = if (isHovered) primary else glassColor.copy(alpha = ringOpacity)
                    val sliceTextColor = if (isHovered) onPrimary else onSurfaceVariant

                    drawArc(
                        color = sliceBgColor,
                        startAngle = startAngle + 2f,
                        sweepAngle = innerSweep - 4f,
                        useCenter = false,
                        topLeft = Offset(center.x - rInnerMid, center.y - rInnerMid),
                        size = Size(rInnerMid * 2, rInnerMid * 2),
                        style = Stroke(width = innerThickness)
                    )"""
content = content.replace(target_inner_slice, replacement_inner_slice)

# 4. Outer Slices Gap and Color
target_outer_slice = """                                val subHovered = hoveredOuterIdx == j
                                val subSliceBgColor = if (subHovered) primary else surfaceVariant.copy(alpha = ringOpacity)
                                val subSliceTextColor = if (subHovered) onPrimary else onSurfaceVariant
                                
                                drawArc(
                                    color = subSliceBgColor,
                                    startAngle = oStart + 1f,
                                    sweepAngle = subSliceSweep - 2f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - rOuterMid, center.y - rOuterMid),
                                    size = Size(rOuterMid * 2, rOuterMid * 2),
                                    style = Stroke(width = outerThickness)
                                )"""
replacement_outer_slice = """                                val subHovered = hoveredOuterIdx == j
                                val subSliceBgColor = if (subHovered) primary else glassColor.copy(alpha = ringOpacity)
                                val subSliceTextColor = if (subHovered) onPrimary else onSurfaceVariant
                                
                                drawArc(
                                    color = subSliceBgColor,
                                    startAngle = oStart + 2f,
                                    sweepAngle = subSliceSweep - 4f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - rOuterMid, center.y - rOuterMid),
                                    size = Size(rOuterMid * 2, rOuterMid * 2),
                                    style = Stroke(width = outerThickness)
                                )"""
content = content.replace(target_outer_slice, replacement_outer_slice)

with open(path, "w") as f:
    f.write(content)
