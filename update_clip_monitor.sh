#!/bin/bash
sed -i 's/val text = clip.getItemAt(0).text?.toString()/val item = clip.getItemAt(0)\n                val uri = item.uri\n                val text = item.text?.toString()\n                val contentToSave = uri?.toString() ?: text\n                if (!contentToSave.isNullOrBlank()) {\n                    coroutineScope.launch {\n                        repository.addClipboardItem(contentToSave)\n                    }\n                }/g' /app/applet/app/src/main/java/com/example/features/ClipboardMonitor.kt

sed -i 's/if (!text.isNullOrBlank()) {/\/\/ removed/g' /app/applet/app/src/main/java/com/example/features/ClipboardMonitor.kt
sed -i 's/coroutineScope.launch {/\/\/ removed/g' /app/applet/app/src/main/java/com/example/features/ClipboardMonitor.kt
sed -i 's/repository.addClipboardItem(text)/\/\/ removed/g' /app/applet/app/src/main/java/com/example/features/ClipboardMonitor.kt
