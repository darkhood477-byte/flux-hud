import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

# Add imports
if "import androidx.compose.ui.draw.alpha" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.alpha\nimport androidx.compose.ui.draw.scale")

if "import androidx.compose.animation.core.Animatable" not in content:
    content = content.replace("import androidx.compose.animation.core.Spring", "import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.tween\nimport androidx.compose.animation.core.FastOutSlowInEasing\nimport androidx.compose.animation.core.Spring")

target = """    val glassColor = Color.Black.copy(alpha = 0.4f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)"""

replacement = """    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
    }

    val glassColor = Color.Black.copy(alpha = 0.4f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .alpha(entryAnim.value)
            .scale(0.8f + 0.2f * entryAnim.value)"""

content = content.replace(target, replacement)

with open(path, "w") as f:
    f.write(content)
