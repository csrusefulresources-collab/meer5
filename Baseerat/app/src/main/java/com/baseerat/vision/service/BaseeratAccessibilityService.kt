package com.baseerat.vision.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.baseerat.vision.BaseeratApp
import com.baseerat.vision.vision.VisionEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

class BaseeratAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var visionEngine: VisionEngine? = null

    private var lastFocusedNode: AccessibilityNodeInfo? = null
    private var lastVolumeUpTime = 0L
    private var lastVolumeDownTime = 0L
    private val doubleTapTimeout = 450L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        visionEngine = VisionEngine(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
        ) {
            event.source?.let {
                lastFocusedNode = it
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || event.action != KeyEvent.ACTION_DOWN) return false

        val app = applicationContext as BaseeratApp
        val now = System.currentTimeMillis()

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val enabled = runBlocking { app.settingsRepository.volumeUpScreen.first() }
                if (enabled) {
                    if (now - lastVolumeUpTime <= doubleTapTimeout) {
                        lastVolumeUpTime = 0L
                        triggerVibrate()
                        describeFullScreen()
                        return true
                    }
                    lastVolumeUpTime = now
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val enabled = runBlocking { app.settingsRepository.volumeDownFocus.first() }
                if (enabled) {
                    if (now - lastVolumeDownTime <= doubleTapTimeout) {
                        lastVolumeDownTime = 0L
                        triggerVibrate()
                        describeFocusedItem()
                        return true
                    }
                    lastVolumeDownTime = now
                }
            }
        }
        return false
    }

    fun describeFocusedItem() {
        serviceScope.launch {
            val app = applicationContext as BaseeratApp
            val lang = app.settingsRepository.language.first()
            app.speechHelper.setLanguage(lang)

            val node = findActiveFocusedNode()
            if (node == null) {
                // اگر کوئی مخصوص آئٹم فوکس میں نہیں ہے تو پوری اسکرین پڑھ کر سنائے
                describeFullScreen()
                return@launch
            }

            val textParts = mutableListOf<String>()
            collectNodeText(node, textParts)

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // اگر فوکس میں کوئی تصویر ہے یا ٹیکسٹ خالی ہے تو اس حصے کا اسکرین شاٹ لے کر AI Vision چلائے
            if (textParts.isEmpty() || node.className?.contains("Image", ignoreCase = true) == true) {
                app.speechHelper.speak(if (lang == "ur") "فوکس کا تجزیہ کیا جا رہا ہے..." else "Analyzing focused item...")
                captureAndAnalyzeRegion(bounds, lang)
            } else {
                val combined = textParts.distinct().joinToString("، ")
                val prefix = if (lang == "ur") "فوکس میں ہے: " else "In focus: "
                app.speechHelper.speak(prefix + combined)
            }
        }
    }

    fun describeFullScreen() {
        serviceScope.launch {
            val app = applicationContext as BaseeratApp
            val lang = app.settingsRepository.language.first()
            app.speechHelper.setLanguage(lang)
            app.speechHelper.speak(if (lang == "ur") "اسکرین کا تجزیہ ہو رہا ہے..." else "Analyzing screen...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val hwBitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            screenshot.hardwareBuffer.close()
                            if (hwBitmap != null) {
                                val softBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                hwBitmap.recycle()
                                analyzeBitmapWithVision(softBitmap, lang)
                            } else {
                                readAllWindowTextFallback(lang)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            readAllWindowTextFallback(lang)
                        }
                    }
                )
            } else {
                readAllWindowTextFallback(lang)
            }
        }
    }

    private fun captureAndAnalyzeRegion(bounds: Rect, lang: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val hwBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )
                        screenshot.hardwareBuffer.close()
                        if (hwBitmap != null) {
                            val full = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            hwBitmap.recycle()
                            val cropped = cropBitmapSafely(full, bounds)
                            analyzeBitmapWithVision(cropped, lang)
                        } else {
                            readAllWindowTextFallback(lang)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        readAllWindowTextFallback(lang)
                    }
                }
            )
        } else {
            readAllWindowTextFallback(lang)
        }
    }

    private fun cropBitmapSafely(source: Bitmap, rect: Rect): Bitmap {
        val left = rect.left.coerceIn(0, source.width - 1)
        val top = rect.top.coerceIn(0, source.height - 1)
        val width = rect.width().coerceIn(1, source.width - left)
        val height = rect.height().coerceIn(1, source.height - top)
        return if (width > 10 && height > 10) {
            Bitmap.createBitmap(source, left, top, width, height)
        } else {
            source
        }
    }

    private fun analyzeBitmapWithVision(bitmap: Bitmap, lang: String) {
        serviceScope.launch(Dispatchers.IO) {
            val app = applicationContext as BaseeratApp
            try {
                val tempFile = File(cacheDir, "screen_capture.jpg")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val detailed = app.settingsRepository.detailedDefault.first()
                val res = visionEngine?.describe(Uri.fromFile(tempFile), detailed, lang)
                withContext(Dispatchers.Main) {
                    if (res != null) {
                        app.speechHelper.speak(if (detailed) res.detailed else res.summary)
                    } else {
                        readAllWindowTextFallback(lang)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    readAllWindowTextFallback(lang)
                }
            }
        }
    }

    private fun readAllWindowTextFallback(lang: String) {
        val app = applicationContext as BaseeratApp
        val root = rootInActiveWindow
        if (root == null) {
            app.speechHelper.speak(
                if (lang == "ur") "اسکرین کا مواد دستیاب نہیں ہے۔" else "Screen content is unavailable."
            )
            return
        }
        val list = mutableListOf<String>()
        collectNodeText(root, list)
        if (list.isEmpty()) {
            app.speechHelper.speak(
                if (lang == "ur") "اسکرین پر کوئی متن نہیں ملا۔" else "No text found on screen."
            )
        } else {
            val text = list.distinct().joinToString("۔ ")
            app.speechHelper.speak(text)
        }
    }

    private fun findActiveFocusedNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return lastFocusedNode
        return root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: lastFocusedNode
    }

    private fun collectNodeText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            collectNodeText(node.getChild(i), out)
        }
    }

    private fun triggerVibrate() {
        serviceScope.launch {
            val app = applicationContext as BaseeratApp
            if (!app.settingsRepository.vibrate.first()) return@launch
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        visionEngine?.close()
        serviceScope.cancel()
    }

    companion object {
        @Volatile
        var instance: BaseeratAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}

