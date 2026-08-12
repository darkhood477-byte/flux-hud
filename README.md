# Flux - Radial HUD Overlay & Utility App

**Flux** is an interactive, transparent Head-Up Display (HUD) overlay and utility application built for Android using Kotlin and Jetpack Compose. It allows users to quickly trigger system controls, launch applications, monitor clipboards, scan QR codes, and run quick tools through a responsive, multi-page radial menu available anywhere on their screen.

---

## 🌟 Key Features

### 1. ⭕ Radial HUD Menu
* **Continuous Sliding Gesture**: Touch, drag, and release to highlight slices and execute actions smoothly.
* **Two-Level Radial Rings**:
  * **Inner Ring**: Primary tools, apps, and categories (e.g., Clipboard, Timer, Caffeine, QR, Settings, Folders).
  * **Outer Ring**: Expanded sub-items for nested controls (e.g., Brightness levels, Volume levels, Folder directories, Timer presets).
* **Paginated Multi-Page Radial Wheel**:
  * **Page 1**: Core utilities, Quick Apps, Folders, and Tools.
  * **Page 2**: Quick System Toggles (Wi-Fi, Bluetooth, Flashlight, Screenshot, Do Not Disturb, Screen Rotation, Volume, Brightness).
* **Swipe Gesture Navigation**: Swipe left or right on the overlay to seamlessly change pages.

---

### 2. 🛠️ Built-in Quick Utilities
* **QR Code Scanner**: Embedded live camera preview within the HUD hub for instant barcode/QR scanning directly into the clipboard.
* **Clipboard History Manager**: Background clipboard monitor that saves copied texts and allows quick re-copying or pasting from the HUD.
* **Caffeine Mode**: Keeps the screen awake for customizable durations (5m, 15m, 30m, 1h) using WakeLocks.
* **Quick Timer**: Set quick countdown timers (5m, 15m, 30m, 1h) directly from the radial menu.
* **Folder Shortcuts**: Quick navigation and access to systemic user folders (Downloads, Documents, Pictures, Music).

---

### 3. ⚙️ System Quick Controls
* **Wi-Fi Toggle**: Quick access to device network settings.
* **Bluetooth Toggle**: Open or manage Bluetooth connection status.
* **Flashlight Control**: Instant torch toggle.
* **Screenshot Capture**: Take instant screenshots using the optional Flux Accessibility Service.
* **Do Not Disturb (DND)**: Enable or disable notification priority policies.
* **Screen Rotation**: Toggle auto-rotation or adjust orientation.
* **Volume Control**: Multi-step outer ring presets (0%, 25%, 50%, 75%, 100%).
* **Brightness Control**: Direct display backlight adjustments (0%, 25%, 50%, 75%, 100%).

---

### 4. 🎨 HUD Customization
* **Ring Opacity**: Adjustable transparency slider for glassmorphism aesthetics.
* **Label Styles**:
  * **Icons Only**: Clean, minimalist vector icon layout.
  * **Words Only**: Direct textual labels.
  * **Icons & Words**: Informative dual layout.
* **Custom Shortcuts**: Map specific installed apps or custom actions to inner radial slots.

---

## 🏗️ Architecture & How It Works

```
                        ┌────────────────────────┐
                        │     MainActivity       │
                        │  (Settings & Config)   │
                        └───────────┬────────────┘
                                    │ Launches / Controls
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            OverlayService                              │
│                    (Foreground Floating Service)                       │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                      RadialMenuOverlay                         │   │
│   │                 (Canvas + Pointer Tracking)                    │   │
│   │                                                                │   │
│   │   • Pointer Tracking (awaitPointerEventScope)                  │   │
│   │   • Trigonometry (atan2, sqrt) -> Hover Index Calculation     │   │
│   │   • Swipe Detection -> Page Navigation                         │   │
│   │   • Haptic Feedback & Spring Animations                        │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│                                   ▼                                    │
│                         ┌───────────────────┐                          │
│                         │   ActionRouter    │                          │
│                         └─────────┬─────────┘                          │
│                                   │                                    │
│        ┌──────────────────────────┼──────────────────────────┐         │
│        ▼                          ▼                          ▼         │
│  CaffeineManager          QuickTimerManager          SystemActionManager│
│  ClipboardMonitor         CameraPreviewCenter        AppRepository (Room)│
└────────────────────────────────────────────────────────────────────────┘
```

### Core Architecture Components

1. **`OverlayService.kt`**:
   * A persistent foreground Android Service that holds a floating window (`WindowManager`) with `TYPE_APPLICATION_OVERLAY`.
   * Hosts the floating side trigger handle and manages HUD visibility states.

2. **`RadialMenuOverlay.kt`**:
   * Pure Jetpack Compose HUD rendered on a transparent overlay canvas.
   * Uses `pointerInput` with `awaitPointerEventScope` to capture real-time finger position (`X`, `Y`).
   * Calculates angles (`atan2(dy, dx)`) and distance (`sqrt(dx^2 + dy^2)`) relative to center to determine active slice hovers.
   * Emits haptic clicks/ticks via `VibrationHelper` when hovering over new slices or toggling pages.

3. **`ActionRouter.kt`**:
   * Dispatches menu actions selected from the HUD to their respective managers (`CaffeineManager`, `QuickTimerManager`, `SystemActionManager`, etc.).

4. **`AppDatabase.kt` & `AppRepository.kt`**:
   * Room Database persistence layer managing customized user shortcuts and clipboard history logs.

---

## 🔑 Permissions & Requirements

To enable all features, Flux utilizes standard Android permissions requested in the **Permissions** tab of the settings screen:

| Permission | Purpose |
| :--- | :--- |
| **System Overlay (`SYSTEM_ALERT_WINDOW`)** | Required to render the floating trigger handle and HUD menu over other applications. |
| **Write Settings (`WRITE_SETTINGS`)** | Required to adjust screen brightness levels directly from the HUD. |
| **Do Not Disturb Access** | Required to toggle system notification policies. |
| **Camera (`CAMERA`)** | Required for live QR code scanning in the HUD center hub. |
| **Accessibility Service** | Optional service used to trigger system-level screenshots. |
| **Bluetooth / Flashlight** | Standard hardware control permissions. |

---

## 🚀 How to Use Flux

1. **Launch Flux**: Open the main application screen.
2. **Grant Permissions**: Go to the **Permissions** tab and grant **Overlay Permission** (and any optional permissions like Write Settings or DND).
3. **Enable HUD**: Toggle the **Enable HUD** switch ON.
4. **Interact with HUD**:
   * Tap or swipe the edge trigger handle to show the Radial HUD.
   * **Touch & Slide**: Drag your finger over any inner or outer slice to preview the selection.
   * **Page Switch**: Swipe left/right or tap the center hub to flip between Page 1 (Utilities) and Page 2 (System Quick Controls).
   * **Release**: Lift your finger off the highlighted slice to execute the action immediately.
5. **Customize**: Open the main app screen to configure **HUD Opacity**, **Label Styles**, or map **Custom Shortcuts**.

---

## 🪟 Floating Windows Setup

For third-party floating apps to launch as freeform windows on standard Android devices:
1. Enable **Developer Options** on your Android device (settings -> About Phone -> tap Build Number 7 times).
2. Go to **Developer Options** and enable **Force activities to be resizable**.
3. Open a shell/terminal and execute:
   ```bash
   adb shell settings put global enable_freeform_support 1
   ```
4. Reboot your device.

### OEM Limitations (Xiaomi, Oppo, Vivo)
Manufacturers using heavily modified Android skins often block third-party Freeform API calls. If floating windows launch in full-screen on these devices, standard Android APIs cannot override the system kernel. Future implementations will require Shizuku (ADB over Wi-Fi) to force the windowing mode via shell commands.


