#!/bin/bash
sed -i 's/import androidx.lifecycle.LifecycleService//g' /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt
sed -i 's/class OverlayService : LifecycleService/import android.app.Service\nclass OverlayService : Service/g' /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt
