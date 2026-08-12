# Flux - System Overlay & Radial Menu

Flux is a comprehensive Android application that provides a gesture-based radial menu overlay, allowing users to quickly access system toggles, utilities, and shortcuts from anywhere on their device. 

## 🏗 Core Architecture

### 1. Overlay Service (`OverlayService.kt`)
The heart of the application is a foreground `Service` that leverages the `WindowManager` API (with `TYPE_APPLICATION_OVERLAY`) to draw UI elements over other applications. 
- **Trigger**: A small floating dot is rendered in the top-right corner. Tapping it toggles the visibility of the main HUD.
- **Compose Integration**: Jetpack Compose is embedded directly into the WindowManager using `ComposeView`. The service implements `LifecycleOwner`, `SavedStateRegistryOwner`, and `ViewModelStoreOwner` to fully support Compose's lifecycle and state management outside of a traditional Activity.

### 2. Radial Menu UI (`RadialMenuOverlay.kt`)
A custom-built, highly interactive radial menu built entirely with Jetpack Compose `Canvas`.
- **Gesture Handling**: Uses `pointerInput` and `awaitPointerEventScope` to track touch coordinates in real-time.
- **Math & Geometry**: Calculates the distance and angle (via `atan2`) from the center of the screen to the user's touch point to determine which "slice" or "ring" of the menu is being interacted with.
- **Pages**: Supports multiple pages of actions (swapped by touching the center hub).
- **Sub-menus**: Items on the inner ring can expand into an outer ring of sub-items based on drag proximity.

### 3. Permissions Dashboard (`MainActivity.kt`)
Because Flux deeply integrates with the Android system, it requires several special permissions. The `MainActivity` acts as a permissions dashboard, guiding the user through granting:
- **Display Over Other Apps** (`SYSTEM_ALERT_WINDOW`): Required for the OverlayService.
- **Modify System Settings** (`WRITE_SETTINGS`): Required for toggling screen rotation and adjusting brightness.
- **Do Not Disturb Access** (`ACCESS_NOTIFICATION_POLICY`): Required to toggle DND mode.
- **Accessibility Service**: Required to execute global system actions (like taking screenshots).
- **Runtime Permissions**: Camera (for Flashlight) and Bluetooth Connect (for Android 12+).

## ✨ Key Features & Modules

### System Action Manager (`SystemActionManager.kt`)
A centralized router for executing system-level actions triggered by the radial menu.
- **Flashlight**: Uses `CameraManager` to toggle the device's torch.
- **Screenshot**: Routes to the `FluxAccessibilityService` to perform a `GLOBAL_ACTION_TAKE_SCREENSHOT`.
- **Volume & Brightness**: Uses `AudioManager` and `Settings.System` to adjust media volume and screen brightness levels.
- **DND & Rotation**: Modifies the `NotificationManager` interruption filter and system accelerometer rotation settings.

### Clipboard Monitor (`ClipboardMonitor.kt`)
A background listener that hooks into the `ClipboardManager`. Whenever the user copies text, it is intercepted and saved locally.
- **Persistence**: Backed by a Room Database (`AppDatabase`, `AppRepository`) to keep a history of copied items, which can be accessed and restored from the radial menu.

### Caffeine Manager (`CaffeineManager.kt`)
A utility that acquires a `PowerManager.WakeLock` (specifically `SCREEN_BRIGHT_WAKE_LOCK` or `PARTIAL_WAKE_LOCK` + `FLAG_KEEP_SCREEN_ON`) to prevent the device from sleeping while active.

### Quick Timer (`QuickTimerManager.kt`)
Allows users to rapidly set countdown timers (e.g., 5 minutes). It uses the `AlarmManager` to schedule a `PendingIntent` that broadcasts to `TimerReceiver` when the time is up, notifying the user.

### Accessibility Service (`FluxAccessibilityService.kt`)
A lightweight implementation of `AccessibilityService`. Its primary purpose is to bypass standard screen-recording prompts by utilizing the accessibility framework to capture instant screenshots on behalf of the user.

## 🛠 Technology Stack

- **UI**: Jetpack Compose (Material 3, Custom Canvas Drawing)
- **Architecture**: MVVM, Clean Architecture principles
- **Database**: Room (SQLite)
- **Concurrency**: Kotlin Coroutines & Flow
- **System Integration**: WindowManager, AccessibilityService, AlarmManager, Camera2, AudioManager
