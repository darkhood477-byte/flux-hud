#!/bin/bash
sed -i 's/Button(onClick = {/OutlinedButton(onClick = {/g' /app/applet/app/src/main/java/com/example/ShortcutConfig.kt
sed -i 's/OutlinedButton(onClick = {/OutlinedButton(onClick = {, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)/g' /app/applet/app/src/main/java/com/example/ShortcutConfig.kt
