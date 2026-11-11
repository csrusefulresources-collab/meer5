package com.baseerat.vision.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Simple TTS helper with Urdu + English support.
 * Screen-reader friendly: we keep utterances short and cancelable.
 */
class SpeechHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var currentRate = 1.0f

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            // Prefer Urdu if available, otherwise English
            val ur = Locale("ur")
            val result = tts?.setLanguage(ur)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.ENGLISH
            }
        }
    }

    fun setLanguage(code: String) {
        if (!ready) return
        val locale = if (code == "ur") Locale("ur") else Locale.ENGLISH
        tts?.language = locale
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(currentRate)
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!ready || text.isBlank()) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val id = UUID.randomUUID().toString()
        tts?.speak(text, mode, null, id)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
