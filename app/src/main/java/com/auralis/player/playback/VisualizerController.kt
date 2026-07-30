package com.auralis.player.playback

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.ln

/**
 * Real-time FFT capture from the playback session. Emits normalised magnitudes
 * (0..1). When the capture permission is missing the flow stays empty and the
 * UI falls back to a beat-synced animation instead of showing a fake spectrum.
 */
@Singleton
class VisualizerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var visualizer: Visualizer? = null
    private var sessionId = 0
    private var smoothed = FloatArray(0)

    private val _magnitudes = MutableStateFlow(FloatArray(0))
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    var barCount: Int = 48
    var sensitivity: Float = 1f
    var smoothing: Float = 0.6f

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    @Synchronized
    fun start(audioSessionId: Int) {
        if (!hasPermission() || audioSessionId == 0) return
        if (visualizer != null && sessionId == audioSessionId) return
        stop()
        sessionId = audioSessionId
        runCatching {
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) = Unit

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                            if (fft != null) publish(fft)
                        }
                    },
                    (Visualizer.getMaxCaptureRate() / 2).coerceAtMost(20000),
                    false,
                    true
                )
                enabled = true
            }
            _active.value = true
        }.onFailure {
            visualizer = null
            _active.value = false
        }
    }

    @Synchronized
    fun stop() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        sessionId = 0
        smoothed = FloatArray(0)
        _magnitudes.value = FloatArray(0)
        _active.value = false
    }

    private fun publish(fft: ByteArray) {
        val bars = barCount.coerceIn(8, 128)
        if (smoothed.size != bars) smoothed = FloatArray(bars)
        val usable = fft.size / 2
        val output = FloatArray(bars)
        val alpha = smoothing.coerceIn(0f, 0.95f)
        for (i in 0 until bars) {
            // logarithmic band mapping keeps low frequencies readable
            val startRatio = ln(1f + i.toFloat() / bars * 9f) / ln(10f)
            val endRatio = ln(1f + (i + 1).toFloat() / bars * 9f) / ln(10f)
            val start = (startRatio * usable).toInt().coerceIn(1, usable - 1)
            val end = (endRatio * usable).toInt().coerceIn(start + 1, usable)
            var peak = 0f
            for (k in start until end) {
                val real = fft[2 * k].toFloat()
                val imaginary = fft[2 * k + 1].toFloat()
                val magnitude = hypot(real, imaginary)
                if (magnitude > peak) peak = magnitude
            }
            val normalised = (ln(1f + peak) / ln(1f + 128f) * sensitivity).coerceIn(0f, 1f)
            smoothed[i] = smoothed[i] * alpha + normalised * (1f - alpha)
            output[i] = smoothed[i]
        }
        _magnitudes.value = output
    }
}
