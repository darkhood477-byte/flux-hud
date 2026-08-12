import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

target_scales = """    val outerRingScale by animateFloatAsState(
        targetValue = if (lockedInnerIdx != null) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )"""

replacement_scales = target_scales + """

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
"""
content = content.replace(target_scales, replacement_scales)


target_center = """                // 1. Draw Center Hub
                val centerBgColor = if (isTouchInCenter) primary else glassColor
                drawCircle(
                    color = centerBgColor,
                    radius = rCenter,
                    center = center
                )"""
replacement_center = """                // 1. Draw Center Hub
                scale(scale = centerScale, pivot = center) {
                    val centerBgColor = if (isTouchInCenter) primary else glassColor
                    drawCircle(
                        color = centerBgColor,
                        radius = rCenter,
                        center = center
                    )
                }"""
content = content.replace(target_center, replacement_center)


target_inner_render = """                    drawArc(
                        color = sliceBgColor,
                        startAngle = startAngle + 2f,
                        sweepAngle = innerSweep - 4f,
                        useCenter = false,
                        topLeft = Offset(center.x - rInnerMid, center.y - rInnerMid),
                        size = Size(rInnerMid * 2, rInnerMid * 2),
                        style = Stroke(width = innerThickness)
                    )

                    val midAngleRad = Math.toRadians((startAngle + innerSweep / 2f).toDouble())
                    val textX = center.x + rInnerMid * cos(midAngleRad).toFloat()
                    val textY = center.y + rInnerMid * sin(midAngleRad).toFloat()

                    val itemText = currentItems[i].title
                    val painter = itemPainters[i]
                    val iconSize = 24.dp.toPx()

                    if (labelStyle == "ICONS_ONLY") {"""

replacement_inner_render = """                    val midAngleRad = Math.toRadians((startAngle + innerSweep / 2f).toDouble())
                    val textX = center.x + rInnerMid * cos(midAngleRad).toFloat()
                    val textY = center.y + rInnerMid * sin(midAngleRad).toFloat()

                    val itemText = currentItems[i].title
                    val painter = itemPainters[i]
                    val iconSize = 24.dp.toPx()

                    val innerScale = innerScales.getOrElse(i) { 1f }
                    scale(scale = innerScale, pivot = Offset(textX, textY)) {
                        drawArc(
                            color = sliceBgColor,
                            startAngle = startAngle + 2f,
                            sweepAngle = innerSweep - 4f,
                            useCenter = false,
                            topLeft = Offset(center.x - rInnerMid, center.y - rInnerMid),
                            size = Size(rInnerMid * 2, rInnerMid * 2),
                            style = Stroke(width = innerThickness)
                        )

                        if (labelStyle == "ICONS_ONLY") {"""
content = content.replace(target_inner_render, replacement_inner_render)


target_inner_end = """                        drawText(
                            textLayoutResult = itemTextResult,
                            topLeft = Offset(textX - itemTextResult.size.width / 2f, textY)
                        )
                    }
                }"""
replacement_inner_end = """                        drawText(
                            textLayoutResult = itemTextResult,
                            topLeft = Offset(textX - itemTextResult.size.width / 2f, textY)
                        )
                    }
                    } // End of scale
                }"""
content = content.replace(target_inner_end, replacement_inner_end)


target_outer_render = """                                drawArc(
                                    color = subSliceBgColor,
                                    startAngle = oStart + 2f,
                                    sweepAngle = subSliceSweep - 4f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - rOuterMid, center.y - rOuterMid),
                                    size = Size(rOuterMid * 2, rOuterMid * 2),
                                    style = Stroke(width = outerThickness)
                                )
       
                                val oMidAngleRad = Math.toRadians((oStart + subSliceSweep / 2f).toDouble())
                                val subTextX = center.x + rOuterMid * cos(oMidAngleRad).toFloat()
                                val subTextY = center.y + rOuterMid * sin(oMidAngleRad).toFloat()
       
                                val subTextResult = textMeasurer.measure("""

replacement_outer_render = """                                val oMidAngleRad = Math.toRadians((oStart + subSliceSweep / 2f).toDouble())
                                val subTextX = center.x + rOuterMid * cos(oMidAngleRad).toFloat()
                                val subTextY = center.y + rOuterMid * sin(oMidAngleRad).toFloat()
       
                                val outerScale = outerScales.getOrElse(j) { 1f }
                                scale(scale = outerScale, pivot = Offset(subTextX, subTextY)) {
                                    drawArc(
                                        color = subSliceBgColor,
                                        startAngle = oStart + 2f,
                                        sweepAngle = subSliceSweep - 4f,
                                        useCenter = false,
                                        topLeft = Offset(center.x - rOuterMid, center.y - rOuterMid),
                                        size = Size(rOuterMid * 2, rOuterMid * 2),
                                        style = Stroke(width = outerThickness)
                                    )

                                    val subTextResult = textMeasurer.measure("""
content = content.replace(target_outer_render, replacement_outer_render)


target_outer_end = """                                drawText(
                                    textLayoutResult = subTextResult,
                                    topLeft = Offset(subTextX - subTextResult.size.width / 2f, subTextY - subTextResult.size.height / 2f)
                                )
                            }
                        }"""
replacement_outer_end = """                                drawText(
                                    textLayoutResult = subTextResult,
                                    topLeft = Offset(subTextX - subTextResult.size.width / 2f, subTextY - subTextResult.size.height / 2f)
                                )
                                } // End of outer scale
                            }
                        }"""
content = content.replace(target_outer_end, replacement_outer_end)

with open(path, "w") as f:
    f.write(content)
