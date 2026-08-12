import re
with open("/app/applet/app/src/main/java/com/example/data/SettingsManager.kt", "r") as f:
    content = f.read()

target = """class SettingsManager(context: Context) {"""
replacement = """class SettingsManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
"""

content = content.replace(target, replacement)
with open("/app/applet/app/src/main/java/com/example/data/SettingsManager.kt", "w") as f:
    f.write(content)
