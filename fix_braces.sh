#!/bin/bash
sed -i 's/        } else if (activeUtility == "QR") {/            }\n        }\n        } else if (activeUtility == "QR") {/g' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
