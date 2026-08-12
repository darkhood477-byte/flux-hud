import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

target = "    val itemPainters = currentItems.map { androidx.compose.ui.graphics.vector.rememberVectorPainter(getIconForId(it.id)) }"
replacement = """    val itemPainters = currentItems.map { androidx.compose.ui.graphics.vector.rememberVectorPainter(getIconForId(it.id)) }
    val subItemPainters = currentItems.map { item ->
        item.subItems.map { subItem ->
            androidx.compose.ui.graphics.vector.rememberVectorPainter(getIconForId(subItem.id))
        }
    }"""

content = content.replace(target, replacement)

with open(path, "w") as f:
    f.write(content)
