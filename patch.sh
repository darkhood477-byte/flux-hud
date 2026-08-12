sed -i 's/androidx.compose.ui.graphics.drawscope.withTransform({/translate(/g' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
sed -i 's/translate(left = textX - iconSize \/ 2f, top = textY - iconSize \/ 2f)//g' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
sed -i 's/translate(left = textX - iconSize \/ 2f, top = textY - iconSize)//g' /app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt
