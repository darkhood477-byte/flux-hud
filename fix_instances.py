import re
with open("/app/applet/app/src/main/java/com/example/MainActivity.kt", "r") as f:
    main_content = f.read()

main_content = main_content.replace("val settingsManager = remember { SettingsManager(context) }", "val settingsManager = remember { SettingsManager.getInstance(context) }")
main_content = main_content.replace("fun HudConfigSection(settingsManager: com.example.data.SettingsManager) {", "fun HudConfigSection(settingsManager: SettingsManager) {")

with open("/app/applet/app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(main_content)

with open("/app/applet/app/src/main/java/com/example/overlay/OverlayService.kt", "r") as f:
    service_content = f.read()

service_content = service_content.replace("settingsManager = com.example.data.SettingsManager(this)", "settingsManager = com.example.data.SettingsManager.getInstance(this)")

with open("/app/applet/app/src/main/java/com/example/overlay/OverlayService.kt", "w") as f:
    f.write(service_content)
