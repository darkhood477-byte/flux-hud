#!/bin/bash
# We need to find the `} else if (activeUtility == "QR") {` and add `}` before it.
sed -i 's/        else if (activeUtility == "QR") {/            }\n        } else if (activeUtility == "QR") {/g' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
