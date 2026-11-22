package com.example.alarm

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

data class EnglishVoiceInfo(
    val name: String,
    val label: String
)

class VoiceAnnouncementManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val readyDeferred = CompletableDeferred<Boolean>()
    private var repeatJob: Job? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val res = tts?.setLanguage(Locale.US)
                    val ok = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                    if (!ok) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                    readyDeferred.complete(true)
                } else {
                    Log.w("VoiceTTS", "TextToSpeech init failed with status $status")
                    readyDeferred.complete(false)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceTTS", "TextToSpeech init exception", e)
            readyDeferred.complete(false)
        }
    }

    suspend fun awaitReady(): Boolean {
        return withTimeoutOrNull(5000L) { readyDeferred.await() } ?: false
    }

    fun getAvailableEnglishVoices(): List<EnglishVoiceInfo> {
        val engine = tts ?: return emptyList()
        return try {
            engine.voices
                ?.filter { it.locale.language.equals("en", ignoreCase = true) }
                ?.sortedBy { it.name }
                ?.map { voice ->
                    val country = voice.locale.displayCountry.ifBlank { "English" }
                    val quality = if (voice.quality >= 400) "HD" else "Standard"
                    EnglishVoiceInfo(
                        name = voice.name,
                        label = "$country ($quality) • ${voice.name.takeLast(14)}"
                    )
                } ?: emptyList()
        } catch (e: Exception) {
            Log.w("VoiceTTS", "Could not enumerate TTS voices", e)
            emptyList()
        }
    }

    fun speakOnce(
        text: String,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        voiceName: String = ""
    ) {
        repeatJob?.cancel()
        repeatJob = CoroutineScope(Dispatchers.Main).launch {
            if (!awaitReady()) return@launch
            applyConfig(speed, pitch, voiceName)
            speakUtteranceSuspend(text)
        }
    }

    fun speakRepeated(
        text: String,
        speed: Float,
        pitch: Float,
        voiceName: String,
        repetitions: Int,
        delaySeconds: Int,
        onComplete: () -> Unit = {}
    ) {
        repeatJob?.cancel()
        repeatJob = CoroutineScope(Dispatchers.Main).launch {
            val ready = awaitReady()
            if (!ready) {
                onComplete()
                return@launch
            }
            applyConfig(speed, pitch, voiceName)
            val count = repetitions.coerceIn(1, 10)
            for (i in 1..count) {
                if (!isActive) break
                speakUtteranceSuspend(text)
                if (i < count && isActive) {
                    delay(delaySeconds.coerceIn(1, 30) * 1000L)
                }
            }
            onComplete()
        }
    }

    private fun applyConfig(speed: Float, pitch: Float, voiceName: String) {
        val engine = tts ?: return
        engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        if (voiceName.isNotBlank()) {
            val chosen = engine.voices?.firstOrNull { it.name == voiceName }
            if (chosen != null) {
                engine.voice = chosen
            }
        }
    }

    private suspend fun speakUtteranceSuspend(text: String) {
        val engine = tts ?: return
        val utteranceId = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) done.complete(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) done.complete(Unit)
            }
            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) done.complete(Unit)
            }
        })
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            done.complete(Unit)
        } else {
            withTimeoutOrNull(30_000L) { done.await() }
        }
    }

    fun stop() {
        repeatJob?.cancel()
        repeatJob = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }
}
