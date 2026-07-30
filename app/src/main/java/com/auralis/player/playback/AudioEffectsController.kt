package com.auralis.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.auralis.player.domain.model.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bandCount: Int = 5,
    val centerFrequencies: List<Int> = emptyList(),
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val levelsMb: List<Int> = emptyList(),
    val presetName: String = "Flat",
    val bassBoostSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
    val bassBoost: Int = 0,
    val trebleBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0
)

/**
 * Wraps the platform audio effects. Every control degrades gracefully when the
 * device does not expose a given effect, so no UI control is ever fake.
 */
@Singleton
class AudioEffectsController @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var sessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == sessionId) return
        release()
        sessionId = audioSessionId
        runCatching {
            equalizer = Equalizer(0, audioSessionId)
        }.onFailure { Log.w(TAG, "Equalizer unavailable", it) }
        runCatching { bassBoost = BassBoost(0, audioSessionId) }
        runCatching { virtualizer = Virtualizer(0, audioSessionId) }
        runCatching { loudness = LoudnessEnhancer(audioSessionId) }
        publish()
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        sessionId = 0
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled && _state.value.bassBoost > 0 }
        runCatching { virtualizer?.enabled = enabled && _state.value.virtualizer > 0 }
        runCatching { loudness?.enabled = enabled && _state.value.loudness > 0 }
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = true
            eq.setBandLevel(band.toShort(), levelMb.toShort())
        }
        val levels = _state.value.levelsMb.toMutableList()
        if (band in levels.indices) levels[band] = levelMb
        _state.value = _state.value.copy(levelsMb = levels, enabled = true, presetName = "Custom")
    }

    fun applyLevels(levelsMb: List<Int>, presetName: String) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = true
            levelsMb.forEachIndexed { index, level ->
                if (index < eq.numberOfBands) {
                    eq.setBandLevel(index.toShort(), level.coerceIn(
                        eq.bandLevelRange[0].toInt(), eq.bandLevelRange[1].toInt()
                    ).toShort())
                }
            }
        }
        _state.value = _state.value.copy(levelsMb = levelsMb, presetName = presetName, enabled = true)
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.let {
                it.enabled = clamped > 0
                it.setStrength(clamped.toShort())
            }
        }
        _state.value = _state.value.copy(bassBoost = clamped)
    }

    /** Treble is implemented by lifting the top equalizer bands. */
    fun setTrebleBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val eq = equalizer
        if (eq != null) {
            runCatching {
                val bands = eq.numberOfBands.toInt()
                val max = eq.bandLevelRange[1].toInt()
                val gain = (max * clamped / 1000.0).toInt()
                eq.enabled = true
                for (band in (bands - 2).coerceAtLeast(0) until bands) {
                    eq.setBandLevel(band.toShort(), gain.toShort())
                }
            }
        }
        _state.value = _state.value.copy(trebleBoost = clamped)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.let {
                it.enabled = clamped > 0
                it.setStrength(clamped.toShort())
            }
        }
        _state.value = _state.value.copy(virtualizer = clamped)
    }

    fun setLoudness(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 2000)
        runCatching {
            loudness?.let {
                it.setTargetGain(clamped)
                it.enabled = clamped > 0
            }
        }
        _state.value = _state.value.copy(loudness = clamped)
    }

    fun currentLevels(): List<Int> = _state.value.levelsMb

    private fun publish() {
        val eq = equalizer
        if (eq == null) {
            _state.value = EqualizerState(available = false)
            return
        }
        val bands = runCatching { eq.numberOfBands.toInt() }.getOrDefault(0)
        val range = runCatching { eq.bandLevelRange }.getOrNull()
        _state.value = EqualizerState(
            available = bands > 0,
            enabled = runCatching { eq.enabled }.getOrDefault(false),
            bandCount = bands,
            centerFrequencies = (0 until bands).map {
                runCatching { eq.getCenterFreq(it.toShort()) / 1000 }.getOrDefault(0)
            },
            minLevelMb = range?.get(0)?.toInt() ?: -1500,
            maxLevelMb = range?.get(1)?.toInt() ?: 1500,
            levelsMb = (0 until bands).map {
                runCatching { eq.getBandLevel(it.toShort()).toInt() }.getOrDefault(0)
            },
            bassBoostSupported = runCatching { bassBoost?.strengthSupported == true }.getOrDefault(false),
            virtualizerSupported = runCatching { virtualizer?.strengthSupported == true }.getOrDefault(false),
            loudnessSupported = loudness != null
        )
    }

    companion object {
        private const val TAG = "AudioEffects"

        /** Built-in presets expressed as relative gain per band (-10..10). */
        val BUILT_IN_PRESETS: List<Pair<String, List<Int>>> = listOf(
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Rock" to listOf(5, 3, -1, 3, 5),
            "Pop" to listOf(-1, 3, 5, 2, -1),
            "Classical" to listOf(4, 2, -2, 3, 5),
            "Jazz" to listOf(3, 2, -1, 2, 4),
            "Electronic" to listOf(6, 2, 0, 2, 6),
            "Hip-Hop" to listOf(7, 4, -1, 2, 4),
            "Vocal" to listOf(-3, 1, 6, 3, -2),
            "Bass Boost" to listOf(9, 6, 1, 0, 0),
            "Acoustic" to listOf(4, 2, 1, 3, 4)
        )

        fun presetToMillibels(preset: List<Int>, bandCount: Int, minMb: Int, maxMb: Int): List<Int> {
            if (bandCount <= 0) return emptyList()
            return (0 until bandCount).map { index ->
                val position = if (bandCount == 1) 0f else index.toFloat() / (bandCount - 1)
                val sourceIndex = position * (preset.size - 1)
                val low = preset[sourceIndex.toInt().coerceIn(0, preset.lastIndex)]
                val high = preset[(sourceIndex.toInt() + 1).coerceIn(0, preset.lastIndex)]
                val fraction = sourceIndex - sourceIndex.toInt()
                val value = low + (high - low) * fraction
                (value / 10f * maxMb).toInt().coerceIn(minMb, maxMb)
            }
        }

        fun asPresetList(): List<EqualizerPreset> = BUILT_IN_PRESETS.mapIndexed { index, (name, levels) ->
            EqualizerPreset(-(index + 1).toLong(), name, levels, isBuiltIn = true)
        }
    }
}
