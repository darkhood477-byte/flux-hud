import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

target1 = """val mimeType = context.contentResolver.getType(uri) ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(android.webkit.MimeTypeMap.getFileExtensionFromUrl(item.text)) ?: "*/*"
                                                android.content.ClipData(android.content.ClipDescription("Dragged File", arrayOf(mimeType)), android.content.ClipData.Item(uri))"""
replacement1 = """android.content.ClipData.newUri(context.contentResolver, "Dragged File", uri)"""
content = content.replace(target1, replacement1)

target2 = """val mimeType = context.contentResolver.getType(uri) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(item.text)) ?: "*/*"
                        // FileProvider Comment: Ensure any local files dragged from the HUD are exposed via a FileProvider (content:// URI). Passing raw file:// URIs will cause a FileUriExposedException crash.
                        ClipData(ClipDescription("Dragged File", arrayOf(mimeType)), ClipData.Item(uri))"""
replacement2 = """ClipData.newUri(context.contentResolver, "Dragged File", uri)"""
content = content.replace(target2, replacement2)

with open(path, "w") as f:
    f.write(content)
