sed -i '/val itemText = currentItems\[i\].title/,/topLeft = Offset(textX - itemTextResult.size.width \/ 2f, textY)/c\
                    val itemText = currentItems[i].title\
                    val painter = itemPainters[i]\
                    val iconSize = 24.dp.toPx()\
\
                    if (labelStyle == "ICONS_ONLY") {\
                        translate(left = textX - iconSize / 2f, top = textY - iconSize / 2f) {\
                            with(painter) {\
                                draw(Size(iconSize, iconSize), alpha = 1f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(sliceTextColor))\
                            }\
                        }\
                    } else if (labelStyle == "WORDS_ONLY") {\
                        val itemTextResult = textMeasurer.measure(\
                            text = itemText,\
                            style = TextStyle(\
                                color = sliceTextColor,\
                                fontSize = 11.sp,\
                                fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,\
                                textAlign = TextAlign.Center\
                            )\
                        )\
                        drawText(\
                            textLayoutResult = itemTextResult,\
                            topLeft = Offset(textX - itemTextResult.size.width / 2f, textY - itemTextResult.size.height / 2f)\
                        )\
                    } else {\
                        translate(left = textX - iconSize / 2f, top = textY - iconSize) {\
                            with(painter) {\
                                draw(Size(iconSize, iconSize), alpha = 1f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(sliceTextColor))\
                            }\
                        }\
                        val itemTextResult = textMeasurer.measure(\
                            text = itemText,\
                            style = TextStyle(\
                                color = sliceTextColor,\
                                fontSize = 10.sp,\
                                fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,\
                                textAlign = TextAlign.Center\
                            )\
                        )\
                        drawText(\
                            textLayoutResult = itemTextResult,\
                            topLeft = Offset(textX - itemTextResult.size.width / 2f, textY)\
                        )\
                    }' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
