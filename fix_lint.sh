#!/bin/bash
sed -i 's/import android.util.Size/import android.util.Size\nimport androidx.camera.core.ExperimentalGetImage\nimport androidx.annotation.OptIn/g' /app/applet/app/src/main/java/com/example/overlay/CameraPreviewCenter.kt
