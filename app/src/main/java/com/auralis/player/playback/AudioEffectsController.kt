package com.auralis.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
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
    val reverbSupported: Boolean = false,
    val bassBoost: Int = 0,
    val trebleBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val targetLoudnessDb: Int = AudioEffectsController.NEUTRAL_LOUDNESS_DB,
    val reverbPreset: Int = 0
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
    private var reverb: PresetReverb? = null
    private var sessionId: Int = 0

    /**
     * The equaliser curve the user actually chose. The treble control writes a
     * tilt on top of this when the levels are pushed to the platform, so the
     * two controls no longer overwrite each other's top bands.
     */
    private var baseLevelsMb: List<Int> = emptyList()

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
        runCatching { reverb = PresetReverb(0, audioSessionId) }
            .onFailure { Log.w(TAG, "PresetReverb unavailable", it) }
        publish()
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        runCatching { reverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        reverb = null
        sessionId = 0
        baseLevelsMb = emptyList()
    }

    fun setEnabled(enabled: Boolean) {
        val current = _state.value
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled && current.bassBoost > 0 }
        runCatching { virtualizer?.enabled = enabled && current.virtualizer > 0 }
        _state.value = current.copy(enabled = enabled)
        // Loudness and reverb are independent of the equaliser switch, but the
        // treble tilt lives in the equaliser bands, so the curve is re-pushed.
        if (enabled) pushLevels(current.presetName)
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        if (equalizer == null) return
        val levels = currentBaseLevels().toMutableList()
        if (band !in levels.indices) return
        levels[band] = levelMb
        baseLevelsMb = levels
        pushLevels("Custom")
    }

    fun applyLevels(levelsMb: List<Int>, presetName: String) {
        if (equalizer == null) return
        baseLevelsMb = levelsMb
        pushLevels(presetName)
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

    /**
     * Treble is implemented as a tilt on the top equaliser bands. It is stored
     * separately from the user's curve and merged in [pushLevels], so moving
     * the treble slider no longer destroys the last two equaliser bands.
     */
    fun setTrebleBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(trebleBoost = clamped)
        pushLevels(_state.value.presetName)
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

    /** Manual make-up gain in millibels, 0..2000. */
    fun setLoudness(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, MAX_LOUDNESS_MB)
        _state.value = _state.value.copy(loudness = clamped)
        pushLoudness()
    }

    /**
     * A3 — target loudness normalization, expressed in dBFS (-24..-6).
     * [NEUTRAL_LOUDNESS_DB] means "leave the signal alone". Louder targets are
     * reached with the platform LoudnessEnhancer; quieter targets are applied
     * by the caller through the player volume, because the enhancer can only
     * add gain.
     */
    fun setTargetLoudness(db: Int) {
        val clamped = db.coerceIn(MIN_TARGET_LOUDNESS_DB, MAX_TARGET_LOUDNESS_DB)
        if (clamped == _state.value.targetLoudnessDb) return
        _state.value = _state.value.copy(targetLoudnessDb = clamped)
        pushLoudness()
    }

    /**
     * A6 — 0 = off, 1..6 map onto the platform preset reverbs
     * (small room, medium room, large room, medium hall, large hall, plate).
     */
    fun setReverbPreset(preset: Int) {
        val clamped = preset.coerceIn(0, REVERB_PRESETS.lastIndex)
        _state.value = _state.value.copy(reverbPreset = clamped)
        runCatching {
            reverb?.let {
                it.preset = REVERB_PRESETS[clamped]
                it.enabled = clamped > 0
            }
        }.onFailure { Log.w(TAG, "Unable to apply reverb preset", it) }
    }

    fun currentLevels(): List<Int> = _state.value.levelsMb

    // ---- internals ---------------------------------------------------------

    private fun currentBaseLevels(): List<Int> {
        if (baseLevelsMb.isNotEmpty()) return baseLevelsMb
        val fromState = _state.value.levelsMb
        if (fromState.isNotEmpty()) return fromState
        val bands = runCatching { equalizer?.numberOfBands?.toInt() }.getOrNull() ?: 0
        return List(bands) { 0 }
    }

    /** Writes base curve + treble tilt to the platform equaliser. */
    private fun pushLevels(presetName: String) {
        val eq = equalizer ?: return
        val base = currentBaseLevels()
        if (base.isEmpty()) return
        baseLevelsMb = base
        val treble = _state.value.trebleBoost
        runCatching {
            val bands = eq.numberOfBands.toInt()
            val minMb = eq.bandLevelRange[0].toInt()
            val maxMb = eq.bandLevelRange[1].toInt()
            val tilt = (maxMb * treble / 1000.0).toInt()
            eq.enabled = true
            for (index in 0 until bands) {
                val level = base.getOrElse(index) { 0 }
                val tilted = if (treble > 0 && index >= (bands - 2).coerceAtLeast(0)) {
                    level + tilt
                } else {
                    level
                }
                eq.setBandLevel(index.toShort(), tilted.coerceIn(minMb, maxMb).toShort())
            }
        }.onFailure { Log.w(TAG, "Unable to apply equalizer levels", it) }
        _state.value = _state.value.copy(
            levelsMb = base,
            presetName = presetName,
            enabled = true
        )
    }

    /** Manual gain and target-loudness make-up share one LoudnessEnhancer. */
    private fun pushLoudness() {
        val current = _state.value
        val normalizationMb = ((current.targetLoudnessDb - NEUTRAL_LOUDNESS_DB) * 100)
            .coerceAtLeast(0)
        val totalMb = (current.loudness + normalizationMb).coerceIn(0, MAX_LOUDNESS_MB)
        runCatching {
            loudness?.let {
                it.setTargetGain(totalMb)
                it.enabled = totalMb > 0
            }
        }.onFailure { Log.w(TAG, "Unable to apply loudness gain", it) }
    }

    private fun publish() {
        val eq = equalizer
        if (eq == null) {
            _state.value = EqualizerState(
                available = false,
                loudnessSupported = loudness != null,
                reverbSupported = reverb != null,
                targetLoudnessDb = _state.value.targetLoudnessDb,
                reverbPreset = _state.value.reverbPreset,
                loudness = _state.value.loudness
            )
            pushLoudness()
            setReverbPreset(_state.value.reverbPreset)
            return
        }
        val bands = runCatching { eq.numberOfBands.toInt() }.getOrDefault(0)
        val range = runCatching { eq.bandLevelRange }.getOrNull()
        val previous = _state.value
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
            loudnessSupported = loudness != null,
            reverbSupported = reverb != null,
            // Values that live outside the platform equaliser survive re-attach.
            bassBoost = previous.bassBoost,
            trebleBoost = previous.trebleBoost,
            virtualizer = previous.virtualizer,
            loudness = previous.loudness,
            targetLoudnessDb = previous.targetLoudnessDb,
            reverbPreset = previous.reverbPreset
        )
        baseLevelsMb = _state.value.levelsMb
        pushLoudness()
        setReverbPreset(previous.reverbPreset)
    }

    companion object {
        private const val TAG = "AudioEffects"

        /** Streaming-standard target; means "no normalization" in this app. */
        const val NEUTRAL_LOUDNESS_DB = -14
        const val MIN_TARGET_LOUDNESS_DB = -24
        const val MAX_TARGET_LOUDNESS_DB = -6
        private const val MAX_LOUDNESS_MB = 2000

        /** Index 0 is "off"; the rest map to the platform preset reverbs. */
        private val REVERB_PRESETS: List<Short> = listOf(
            PresetReverb.PRESET_NONE,
            PresetReverb.PRESET_SMALLROOM,
            PresetReverb.PRESET_MEDIUMROOM,
            PresetReverb.PRESET_LARGEROOM,
            PresetReverb.PRESET_MEDIUMHALL,
            PresetReverb.PRESET_LARGEHALL,
            PresetReverb.PRESET_PLATE
        )

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
