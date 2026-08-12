#!/bin/bash

# Extract lines before setContent
sed -n '1,/setContent {/p' /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt > /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt.new

# Append the new setContent block
cat << 'INNER_EOF' >> /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt.new
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                MyApplicationTheme {
                    val isImmersive by settingsManager.isImmersiveModeActive.collectAsState()
                    var isDraggingHovered by remember { mutableStateOf(false) }

                    val dragAndDropTarget = remember {
                        object : DragAndDropTarget {
                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                isDraggingHovered = false
                                val dragEvent = event.toAndroidDragEvent()
                                val clipData = dragEvent.clipData ?: return false
                                if (clipData.itemCount > 0) {
                                    val item = clipData.getItemAt(0)
                                    val uri = item.uri
                                    val text = item.text?.toString()
                                    
                                    val contentToSave = uri?.toString() ?: text
                                    if (!contentToSave.isNullOrBlank()) {
                                        lifecycleScope.launch {
                                            appRepository.addClipboardItem(contentToSave)
                                        }
                                        return true
                                    }
                                }
                                return false
                            }

                            override fun onStarted(event: DragAndDropEvent) {}
                            override fun onEntered(event: DragAndDropEvent) { isDraggingHovered = true }
                            override fun onExited(event: DragAndDropEvent) { isDraggingHovered = false }
                            override fun onEnded(event: DragAndDropEvent) { isDraggingHovered = false }
                            override fun onMoved(event: DragAndDropEvent) {}
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isDraggingHovered) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isImmersive) 0.3f else 0.85f))
                            .clickable { toggleRadialMenu() }
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { event ->
                                    val dragEvent = event.toAndroidDragEvent()
                                    val desc = dragEvent.clipDescription
                                    desc != null && (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || desc.hasMimeType("image/*") || desc.hasMimeType("*/*"))
                                },
                                target = dragAndDropTarget
                            ),
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
INNER_EOF

# Extract lines after updateTriggerPosition signature
sed -n '/val density = resources.displayMetrics.density/,$p' /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt >> /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt.new

mv /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt.new /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt

