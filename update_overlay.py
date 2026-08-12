import re

path = "/app/applet/app/src/main/java/com/example/overlay/OverlayService.kt"
with open(path, "r") as f:
    content = f.read()

target1 = """                    var isDraggingHovered by remember { mutableStateOf(false) }"""
replacement1 = """                    var isDraggingHovered by remember { mutableStateOf(false) }
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isDraggingHovered) 1.2f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)
                    )"""
content = content.replace(target1, replacement1)

target2 = """                            .clip(CircleShape)"""
replacement2 = """                            .scale(scale)
                            .clip(CircleShape)"""
content = content.replace(target2, replacement2)

with open(path, "w") as f:
    f.write(content)
