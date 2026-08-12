#!/bin/bash
sed -i '/Canvas(modifier = Modifier.fillMaxSize()) {/i \
            if (currentItems.isNotEmpty() && lockedInnerIdx != null) {\
                val subItems = currentItems[lockedInnerIdx!!].subItems\
                if (subItems.isNotEmpty() && currentItems[lockedInnerIdx!!].id == "clipboard") {\
                    val rOuterMid = with(density) { 182.5.dp.toPx() }\
                    val innerSweep = 360f / currentItems.size\
                    val innerCenterAngle = -90f + lockedInnerIdx!! * innerSweep + innerSweep / 2f\
                    val outerSweep = maxOf(60f, subItems.size * 35f)\
                    val outerStartAngle = innerCenterAngle - outerSweep / 2f\
                    val subSliceSweep = outerSweep / subItems.size\
                    val screenWidth = constraints.maxWidth.toFloat()\
                    val screenHeight = constraints.maxHeight.toFloat()\
                    val centerX = screenWidth / 2f\
                    val centerY = screenHeight / 2f\
                    subItems.forEachIndexed { j, subItem ->\
                        val oStart = outerStartAngle + j * subSliceSweep\
                        val oMidAngleRad = Math.toRadians((oStart + subSliceSweep / 2f).toDouble())\
                        val subTextX = centerX + rOuterMid * kotlin.math.cos(oMidAngleRad).toFloat()\
                        val subTextY = centerY + rOuterMid * kotlin.math.sin(oMidAngleRad).toFloat()\
                        val boxSize = 60.dp\
                        val offsetX = with(density) { subTextX.toDp() - boxSize / 2 }\
                        val offsetY = with(density) { subTextY.toDp() - boxSize / 2 }\
                        if (j < clipboardHistory.size) {\
                            val item = clipboardHistory[j]\
                            Box(\
                                modifier = Modifier\
                                    .offset(x = offsetX, y = offsetY)\
                                    .size(boxSize)\
                                    .dragAndDropSource {\
                                        detectTapGestures(onLongPress = { _ ->\
                                            val isUri = item.text.startsWith("content:\/\/") || item.text.startsWith("file:\/\/")\
                                            val clipDataDrag = if (isUri) {\
                                                val uri = android.net.Uri.parse(item.text)\
                                                val mimeType = context.contentResolver.getType(uri) ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(android.webkit.MimeTypeMap.getFileExtensionFromUrl(item.text)) ?: "*\/*"\
                                                android.content.ClipData(android.content.ClipDescription("Dragged File", arrayOf(mimeType)), android.content.ClipData.Item(uri))\
                                            } else {\
                                                android.content.ClipData.newPlainText("Clipboard history", item.text)\
                                            }\
                                            startTransfer(\
                                                androidx.compose.ui.draganddrop.DragAndDropTransferData(\
                                                    clipData = clipDataDrag,\
                                                    flags = android.view.View.DRAG_FLAG_GLOBAL or android.view.View.DRAG_FLAG_GLOBAL_URI_READ\
                                                )\
                                            )\
                                        })\
                                    }\
                            )\
                        }\
                    }\
                }\
            }\
' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
