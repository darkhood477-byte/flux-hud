import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

# 1. Add initialTouchPos
target1 = """    var touchPos by remember { mutableStateOf<Offset?>(null) }"""
replacement1 = """    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var initialTouchPos by remember { mutableStateOf<Offset?>(null) }"""
content = content.replace(target1, replacement1)

# 2. Track initialTouchPos on down
target2 = """                        if (change.pressed) {
                            val isDown = !change.previousPressed
                            touchPos = change.position
                            val pos = change.position"""
replacement2 = """                        if (change.pressed) {
                            val isDown = !change.previousPressed
                            touchPos = change.position
                            if (isDown) {
                                initialTouchPos = change.position
                            }
                            val pos = change.position"""
content = content.replace(target2, replacement2)

# 3. Remove immediate dismiss on down outside radius
target3 = """                                    if (isDown) {
                                        val maxActiveRadius = if (lockedInnerIdx != null) rOuterEnd else rInnerEnd
                                        if (dist > maxActiveRadius) {
                                            lockedInnerIdx = null
                                            vibratorHelper.click()
                                            onDismiss()
                                        }
                                    }"""
replacement3 = """"""
content = content.replace(target3, replacement3)

# 4. Handle swipe and dismiss on release
target4 = """                        } else {
                            centerDownTime = 0L
                            val pos = touchPos
                            touchPos = null
                            val currentLockedIdx = lockedInnerIdx
                            lockedInnerIdx = null
                            
                            prevHapticRing = -1
                            prevHapticIdx = -1
                            
                            if (activeUtility == null && pos != null) {"""
replacement4 = """                        } else {
                            centerDownTime = 0L
                            val pos = touchPos ?: change.position
                            val initial = initialTouchPos ?: pos
                            
                            val dxDrag = pos.x - initial.x
                            val dyDrag = pos.y - initial.y
                            
                            initialTouchPos = null
                            touchPos = null
                            val currentLockedIdx = lockedInnerIdx
                            lockedInnerIdx = null
                            
                            prevHapticRing = -1
                            prevHapticIdx = -1
                            
                            if (activeUtility == null) {"""
content = content.replace(target4, replacement4)

target5 = """                                    val maxActiveRadius = if (currentLockedIdx != null) rOuterEnd else rInnerEnd

                                    if (dist > maxActiveRadius) {
                                        vibratorHelper.click()
                onDismiss()
                                    } else if (dist < rCenter) {"""
replacement5 = """                                    val maxActiveRadius = if (currentLockedIdx != null) rOuterEnd else rInnerEnd

                                    if (dist > maxActiveRadius) {
                                        if (abs(dxDrag) > 100f && abs(dxDrag) > abs(dyDrag)) {
                                            if (dxDrag > 0) {
                                                pageIndex = (pageIndex - 1 + 2) % 2
                                            } else {
                                                pageIndex = (pageIndex + 1) % 2
                                            }
                                            vibratorHelper.click()
                                        } else {
                                            vibratorHelper.click()
                                            onDismiss()
                                        }
                                    } else if (dist < rCenter) {"""
content = content.replace(target5, replacement5)

with open(path, "w") as f:
    f.write(content)
