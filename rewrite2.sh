#!/bin/bash
sed -i 's/CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)/CardDefaults.cardColors(containerColor = Color.Transparent)/g' /app/applet/app/src/main/java/com/example/ShortcutConfig.kt
sed -i 's/border = BorderStroke(1.dp, Color(0xFF333333))/border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)/g' /app/applet/app/src/main/java/com/example/ShortcutConfig.kt
