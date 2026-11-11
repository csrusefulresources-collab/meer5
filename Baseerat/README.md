# Baseerat (بصیرت) — Offline Visual Understanding for Android

**Baseerat** is an accessible Android app inspired by VisionMate.  
It helps blind and low-vision users (and everyone else) understand images and screen content **offline**, with full **Urdu + English** support and a floating overlay button that works over any app.

---

## Features

| Feature | Description |
|---------|-------------|
| **Camera / Gallery** | Pick or capture an image and get a description |
| **Floating Overlay Button** | Draggable button over any app – tap to describe |
| **Offline Engine** | On-device OCR + Image Labeling (ML Kit) – no internet required |
| **Urdu + English** | Full UI and spoken results in both languages |
| **TalkBack Friendly** | Large targets, content descriptions, semantic headings |
| **Follow-up Questions** | Ask simple questions about the current image |
| **Share Target** | Share any image to Baseerat from other apps |
| **Auto-speak + Vibration** | Configurable feedback for screen-reader users |

---

## Why Kotlin?

Kotlin + Jetpack Compose was chosen because:

1. Best support for **Accessibility Services** and **SYSTEM_ALERT_WINDOW** overlays.
2. Excellent TalkBack / contentDescription support in Compose.
3. GitHub Actions can build signed/unsigned APKs reliably with the official Android Gradle Plugin.
4. Modern, maintainable codebase.

Python (Kivy / BeeWare) would make overlay + accessibility service much harder and less reliable.

---

## Project Structure

```
Baseerat/
├── app/
│   ├── src/main/
│   │   ├── java/com/baseerat/vision/
│   │   │   ├── BaseeratApp.kt
│   │   │   ├── data/SettingsRepository.kt
│   │   │   ├── service/
│   │   │   │   ├── OverlayService.kt
│   │   │   │   └── BaseeratAccessibilityService.kt
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── DescribeActivity.kt
│   │   │   │   └── theme/Theme.kt
│   │   │   ├── util/SpeechHelper.kt
│   │   │   └── vision/VisionEngine.kt
│   │   ├── res/ ...
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/build.yml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## How to Build APK (GitHub Actions)

1. Create a new GitHub repository.
2. Upload the entire `Baseerat` folder contents to the root of the repo.
3. Push to `main`.
4. Go to **Actions** tab → the workflow `Build APK` will run automatically.
5. Download the APK from the workflow artifacts.

### Local build (optional)

```bash
# Requires Android SDK + JDK 17
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

---

## First-time Setup on Device

1. Install the APK.
2. Open **Baseerat**.
3. Go to **Settings → Accessibility** and enable **Baseerat Accessibility**.
4. Grant **Display over other apps** permission when asked.
5. Tap **Show Floating Button**.
6. Now open any app and use the floating button, or use Camera / Gallery from the home screen.

---

## Offline Model

Current version uses **Google ML Kit** (on-device):

- Text Recognition (OCR)
- Image Labeling

This gives reliable offline results without large model downloads.  
The architecture in `VisionEngine.kt` is designed so a real local Vision-Language Model (llama.cpp Android / ONNX) can be plugged in later.

---

## License

GPL-2.0-or-later (same spirit as VisionMate).  
You are free to use, modify and distribute.

---

## Credits

Inspired by **VisionMate** (NVDA add-on by Athiban Vijay) – local, private, offline visual understanding for accessibility.
