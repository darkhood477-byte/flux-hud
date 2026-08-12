import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

target = """            Canvas(modifier = Modifier.fillMaxSize()) {"""
replacement = """            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (pageIndex == 0) primary else glassColor, androidx.compose.foundation.shape.CircleShape)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (pageIndex == 1) primary else glassColor, androidx.compose.foundation.shape.CircleShape)
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {"""

content = content.replace(target, replacement)

with open(path, "w") as f:
    f.write(content)
