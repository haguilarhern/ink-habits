package com.inkhabits.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device handwriting -> text via ML Kit Digital Ink Recognition. Used to convert
 * the handwritten anchor cue into clean text. Fully offline once the (one-time)
 * language model downloads; degrades gracefully (returns null) if recognition or
 * Play services are unavailable, so callers can fall back to keeping the ink.
 */
object InkRecognizer {

    private val modelId by lazy {
        DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
    }
    private val model by lazy {
        modelId?.let { DigitalInkRecognitionModel.builder(it).build() }
    }
    private val recognizer by lazy {
        model?.let { DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(it).build()) }
    }
    private val manager by lazy { RemoteModelManager.getInstance() }

    @Volatile private var ready = false

    /** Kick off the model download ahead of time (fire-and-forget). */
    fun preload() {
        val m = model ?: return
        manager.isModelDownloaded(m).addOnSuccessListener { downloaded ->
            if (downloaded) ready = true
            else manager.download(m, DownloadConditions.Builder().build())
                .addOnSuccessListener { ready = true }
        }
    }

    /** Recognize serialized [StrokeSerializer] ink. Returns the top text candidate, or null. */
    suspend fun recognize(strokeData: String): String? {
        val rec = recognizer ?: return null
        val m = model ?: return null
        val ink = buildInk(strokeData) ?: return null
        if (!ensureModel(m)) return null
        return try {
            rec.recognize(ink).awaitValue()?.candidates?.firstOrNull()?.text?.trim()?.ifBlank { null }
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun ensureModel(m: DigitalInkRecognitionModel): Boolean {
        if (ready) return true
        val downloaded = manager.isModelDownloaded(m).awaitValue() ?: false
        if (downloaded) { ready = true; return true }
        val ok = manager.download(m, DownloadConditions.Builder().build()).awaitDone()
        ready = ok
        return ok
    }

    private fun buildInk(strokeData: String): Ink? {
        val data = StrokeSerializer.deserialize(strokeData)
        if (data.isEmpty) return null
        val builder = Ink.builder()
        var t = 0L
        for (stroke in data.strokes) {
            if (stroke.isEmpty()) continue
            val sb = Ink.Stroke.builder()
            for (p in stroke) sb.addPoint(Ink.Point.create(p.x, p.y, t++))
            builder.addStroke(sb.build())
        }
        return builder.build()
    }

    private suspend fun <T> Task<T>.awaitValue(): T? = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resume(null) }
    }

    private suspend fun <T> Task<T>.awaitDone(): Boolean = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(true) }
        addOnFailureListener { c.resume(false) }
    }
}
