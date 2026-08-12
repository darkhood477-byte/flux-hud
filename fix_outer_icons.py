import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

target = """val subTextResult = textMeasurer.measure(
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
                                )"""

replacement = """val subPainter = subItemPainters[hoveredInnerIdx][j]
                                val subIconSize = 20.dp.toPx()

                                if (labelStyle == "ICONS_ONLY") {
                                    translate(left = subTextX - subIconSize / 2f, top = subTextY - subIconSize / 2f) {
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
                                        topLeft = Offset(subTextX - subTextResult.size.width / 2f, subTextY - subTextResult.size.height / 2f)
                                    )
                                } else {
                                    translate(left = subTextX - subIconSize / 2f, top = subTextY - subIconSize) {
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
                                        topLeft = Offset(subTextX - subTextResult.size.width / 2f, subTextY)
                                    )
                                }"""

content = content.replace(target, replacement)
with open(path, "w") as f:
    f.write(content)
