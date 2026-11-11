package com.vesper.audio.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.pow
import kotlin.math.roundToInt

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelMb: Short,
    val formattedFreq: String
)

data class EqPreset(
    val name: String,
    val isCustom: Boolean = false,
    val description: String = "",
    val bandLevels: List<Short> = emptyList(),
    val bassBoostStrength: Short = 0
)

data class SignatureToneProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val weights: List<Float>,
    val bassBoostStrength: Short,
    val virtualizerStrength: Short,
    val requiresVirtualizer: Boolean = false
)

enum class ProcessingStatus(val label: String) {
    OFF("Processing: Off"),
    SOFTWARE("Processing: Software"),
    DEVICE_EFFECT("Processing: Device effect"),
    UNAVAILABLE("Processing: Effect unavailable")
}

data class HardwareCapabilities(
    val hasEqualizer: Boolean = false,
    val equalizerBandsCount: Int = 0,
    val hasBassBoost: Boolean = false,
    val hasVirtualizer: Boolean = false,
    val hasLoudnessEnhancer: Boolean = false,
    val hasDynamicsProcessing: Boolean = false,
    val queryError: String? = null
)

data class HeadroomStatus(
    val maxPositiveGainDb: Float,
    val preampAttenuationDb: Float,
    val isProtected: Boolean,
    val hasClipRisk: Boolean
)

class AudioEffectController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    companion object {
        val DEFAULT_10_BANDS = listOf(
            EqBand(0, 31, 0, "31"),
            EqBand(1, 62, 0, "62"),
            EqBand(2, 125, 0, "125"),
            EqBand(3, 250, 0, "250"),
            EqBand(4, 500, 0, "500"),
            EqBand(5, 1000, 0, "1k"),
            EqBand(6, 2000, 0, "2k"),
            EqBand(7, 4000, 0, "4k"),
            EqBand(8, 8000, 0, "8k"),
            EqBand(9, 16000, 0, "16k")
        )

        // 6 Persistent Basic EQ Modes per specification
        val BASIC_MODES = listOf(
            EqPreset(
                name = "Balanced",
                description = "Neutral baseline (0 dB across all bands, zero extra coloration)."
            ),
            EqPreset(
                name = "Bass",
                description = "Controlled low-frequency lift with clean sub-bass warmth."
            ),
            EqPreset(
                name = "Vocal",
                description = "Intelligible, forward midrange presence between 1 kHz and 4 kHz."
            ),
            EqPreset(
                name = "Rock",
                description = "Energetic low and high guitar presence without harsh clipping."
            ),
            EqPreset(
                name = "Pop",
                description = "Modest bass and treble lift with clear, articulate mids."
            ),
            EqPreset(
                name = "Acoustic",
                description = "Natural unplugged instrument presence and gentle warmth."
            )
        )

        // Full built-in preset catalogue (retains VESPER Signature, Flat / DSP Off, and extended presets for compatibility)
        val BUILT_IN_MODES = BASIC_MODES + listOf(
            EqPreset(
                name = "VESPER Signature",
                description = "Balanced house sound with controlled low-end weight, clear vocal presence, and smooth treble."
            ),
            EqPreset(
                name = "Flat / DSP Off",
                description = "Neutral 0 dB bypass. Disables app Equalizer, Bass Boost, and Virtualizer for A/B comparison."
            ),
            EqPreset(
                name = "Studio Reference",
                description = "Legacy alias for Flat / DSP Off (0 dB flat bypass)."
            ),
            EqPreset(
                name = "Warm",
                description = "Elevated 250 Hz–500 Hz warmth with relaxed upper frequencies."
            ),
            EqPreset(
                name = "Classical",
                description = "Wide dynamic contour preserving delicate pianissimo and brass."
            ),
            EqPreset(
                name = "Electronic",
                description = "Punchy low-end transient emphasis with crisp 8 kHz–16 kHz highs."
            ),
            EqPreset(
                name = "Cinematic",
                description = "Sub-bass extension and wide mid-high presence for soundtracks."
            )
        )

        const val DEFAULT_SIGNATURE_TONE_ID = "tone_vesper_signature"
        const val FLAT_BYPASS_TONE_ID = "tone_flat_bypass"
        const val LEGACY_STUDIO_REFERENCE_TONE_ID = "tone_studio_reference"

        val FLAT_BYPASS_PROFILE = SignatureToneProfile(
            id = FLAT_BYPASS_TONE_ID,
            title = "Flat / DSP Off",
            subtitle = "0 dB Unprocessed Utility Bypass",
            description = "Neutral 0 dB flat response with zero EQ, bass boost, or stage widening for direct A/B comparison.",
            weights = listOf(0f, 0f, 0f, 0f, 0f),
            bassBoostStrength = 0,
            virtualizerStrength = 0
        )

        // 6 Functional Signature Tones (Separate from the 6 Basic EQ Modes; genuine capability-tested DSP only)
        val SIGNATURE_TONES = listOf(
            SignatureToneProfile(
                id = DEFAULT_SIGNATURE_TONE_ID,
                title = "VESPER Signature",
                subtitle = "Balanced House Sound & Headroom",
                description = "Controlled low-end weight, articulate vocal/instrument presence, and smooth treble with headroom protection.",
                weights = listOf(0.18f, 0.14f, 0.04f, 0.16f, 0.22f),
                bassBoostStrength = 160,
                virtualizerStrength = 80
            ),
            SignatureToneProfile(
                id = "tone_warmth",
                title = "Warmth",
                subtitle = "Rich Low-Mids & Smooth Highs",
                description = "Gentle 62 Hz–250 Hz body with softened upper-treble glare and zero midrange mud.",
                weights = listOf(0.20f, 0.28f, 0.18f, 0.04f, -0.14f),
                bassBoostStrength = 200,
                virtualizerStrength = 0
            ),
            SignatureToneProfile(
                id = "tone_pulse",
                title = "Pulse",
                subtitle = "Sub-Bass Drive & Transient Punch",
                description = "Tight sub-bass lift paired with crisp upper-mid rhythmic definition and protected headroom.",
                weights = listOf(0.38f, 0.26f, -0.04f, 0.16f, 0.26f),
                bassBoostStrength = 360,
                virtualizerStrength = 90
            ),
            SignatureToneProfile(
                id = "tone_air",
                title = "Air",
                subtitle = "Open Treble Shimmer & Clarity",
                description = "Refined 4 kHz–16 kHz harmonic presence and open acoustic detail without harsh sibilance.",
                weights = listOf(0.06f, 0.02f, 0.10f, 0.24f, 0.34f),
                bassBoostStrength = 60,
                virtualizerStrength = 140
            ),
            SignatureToneProfile(
                id = "tone_depth",
                title = "Depth",
                subtitle = "Spatial Stage Contour",
                description = "Applies capability-tested Android Virtualizer stage expansion where supported by the device.",
                weights = listOf(0.22f, 0.14f, -0.06f, 0.18f, 0.26f),
                bassBoostStrength = 180,
                virtualizerStrength = 340,
                requiresVirtualizer = true
            ),
            SignatureToneProfile(
                id = "tone_night",
                title = "Night",
                subtitle = "Low-Volume Comfort & Clear Vocals",
                description = "Attenuates deep sub-rumble and sharp highs while keeping midrange vocals intelligible at low volume.",
                weights = listOf(-0.18f, 0.06f, 0.24f, 0.18f, -0.20f),
                bassBoostStrength = 0,
                virtualizerStrength = 0
            )
        )

        fun normalizeToneId(rawToneId: String?): String {
            if (rawToneId.isNullOrBlank()) return DEFAULT_SIGNATURE_TONE_ID
            return when (rawToneId.trim().lowercase()) {
                "tone_studio_reference", "studio reference", "tone_vesper_signature", "vesper signature" -> DEFAULT_SIGNATURE_TONE_ID
                "tone_flat_bypass", "flat / dsp off", "flat", "bypass" -> FLAT_BYPASS_TONE_ID
                else -> SIGNATURE_TONES.firstOrNull {
                    it.id.equals(rawToneId, ignoreCase = true) || it.title.equals(rawToneId, ignoreCase = true)
                }?.id ?: DEFAULT_SIGNATURE_TONE_ID
            }
        }
    }

    private val _isSupported = MutableStateFlow(true)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _isAbBypassed = MutableStateFlow(false)
    val isAbBypassed: StateFlow<Boolean> = _isAbBypassed.asStateFlow()

    private val _processingStatus = MutableStateFlow(ProcessingStatus.DEVICE_EFFECT)
    val processingStatus: StateFlow<ProcessingStatus> = _processingStatus.asStateFlow()

    private val _hardwareCapabilities = MutableStateFlow(queryHardwareCapabilities())
    val hardwareCapabilities: StateFlow<HardwareCapabilities> = _hardwareCapabilities.asStateFlow()

    private val _bands = MutableStateFlow<List<EqBand>>(DEFAULT_10_BANDS)
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()

    private val _bandLevelRange = MutableStateFlow<Pair<Short, Short>>((-1200).toShort() to 1200.toShort())
    val bandLevelRange: StateFlow<Pair<Short, Short>> = _bandLevelRange.asStateFlow()

    private val _activePresetName = MutableStateFlow("Balanced")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    private val _activeToneId = MutableStateFlow(DEFAULT_SIGNATURE_TONE_ID)
    val activeToneId: StateFlow<String> = _activeToneId.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow<Short>(0)
    val bassBoostStrength: StateFlow<Short> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow<Short>(0)
    val virtualizerStrength: StateFlow<Short> = _virtualizerStrength.asStateFlow()

    private val _headroomProtectionEnabled = MutableStateFlow(true)
    val headroomProtectionEnabled: StateFlow<Boolean> = _headroomProtectionEnabled.asStateFlow()

    private val _preampGainScalar = MutableStateFlow(1.0f)
    val preampGainScalar: StateFlow<Float> = _preampGainScalar.asStateFlow()

    private val _audioBalance = MutableStateFlow(0f) // -1.0f (Full Left) .. 0f (Center) .. +1.0f (Full Right)
    val audioBalance: StateFlow<Float> = _audioBalance.asStateFlow()

    private val _disableEffectsOnSpeaker = MutableStateFlow(false)
    val disableEffectsOnSpeaker: StateFlow<Boolean> = _disableEffectsOnSpeaker.asStateFlow()

    private var isBuiltInSpeakerActive: Boolean = false

    init {
        updateProcessingStatus()
    }

    private fun queryHardwareCapabilities(): HardwareCapabilities {
        return try {
            val descriptors = AudioEffect.queryEffects() ?: emptyArray()
            var hasEq = false
            var hasBb = false
            var hasVirt = false
            var hasLe = false
            var hasDp = false

            for (desc in descriptors) {
                when (desc.type) {
                    AudioEffect.EFFECT_TYPE_EQUALIZER -> hasEq = true
                    AudioEffect.EFFECT_TYPE_BASS_BOOST -> hasBb = true
                    AudioEffect.EFFECT_TYPE_VIRTUALIZER -> hasVirt = true
                    AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER -> hasLe = true
                    UUID.fromString("e0e6539b-1788-4cc1-ac18-02c0d57e17cb") -> hasDp = true
                }
            }
            HardwareCapabilities(
                hasEqualizer = hasEq,
                equalizerBandsCount = 5,
                hasBassBoost = hasBb,
                hasVirtualizer = hasVirt,
                hasLoudnessEnhancer = hasLe,
                hasDynamicsProcessing = hasDp
            )
        } catch (e: Exception) {
            HardwareCapabilities(queryError = e.message)
        }
    }

    private fun isFlatBypassPresetActive(): Boolean {
        val preset = _activePresetName.value
        return _isAbBypassed.value ||
            _activeToneId.value == FLAT_BYPASS_TONE_ID ||
            preset.equals("Flat / DSP Off", ignoreCase = true) ||
            preset.equals("Studio Reference", ignoreCase = true)
    }

    fun getHeadroomStatus(): HeadroomStatus {
        val isBypassed = isFlatBypassPresetActive()
        val maxGainMb = _bands.value.maxOfOrNull { it.levelMb.toInt() }?.coerceAtLeast(0) ?: 0
        val bassExtraDb = (_bassBoostStrength.value / 1000f) * 3.0f
        val maxGainDb = if (_isEnabled.value && !isBypassed) {
            (maxGainMb / 100f) + bassExtraDb
        } else {
            0f
        }
        val preampDb = if (_headroomProtectionEnabled.value && _isEnabled.value && !isBypassed) {
            -maxGainDb.coerceAtMost(9f)
        } else {
            0f
        }
        val isProtected = _headroomProtectionEnabled.value
        val hasClipRisk = maxGainDb > 0.5f && !isProtected && _isEnabled.value && !isBypassed
        return HeadroomStatus(
            maxPositiveGainDb = maxGainDb,
            preampAttenuationDb = preampDb,
            isProtected = isProtected,
            hasClipRisk = hasClipRisk
        )
    }

    private fun recalculatePreampScalar() {
        val status = getHeadroomStatus()
        val scalar = if (status.isProtected && status.preampAttenuationDb < 0f) {
            10.0.pow((status.preampAttenuationDb / 20.0)).toFloat().coerceIn(0.35f, 1.0f)
        } else {
            1.0f
        }
        _preampGainScalar.value = scalar
    }

    fun toggleHeadroomProtection(enabled: Boolean) {
        _headroomProtectionEnabled.value = enabled
        recalculatePreampScalar()
    }

    fun setAudioBalance(balance: Float) {
        _audioBalance.value = balance.coerceIn(-1f, 1f)
    }

    fun setAbBypass(bypassed: Boolean) {
        _isAbBypassed.value = bypassed
        val shouldEnable = isEffectivelyEnabled()
        try {
            equalizer?.enabled = shouldEnable
            bassBoost?.enabled = shouldEnable
            virtualizer?.enabled = shouldEnable && _virtualizerStrength.value > 0
        } catch (_: Exception) {
        }
        updateProcessingStatus()
    }

    fun toggleAbBypass(): Boolean {
        val next = !_isAbBypassed.value
        setAbBypass(next)
        return next
    }

    fun setSpeakerBypass(disableOnSpeaker: Boolean, isSpeakerOutput: Boolean = isBuiltInSpeakerActive) {
        _disableEffectsOnSpeaker.value = disableOnSpeaker
        isBuiltInSpeakerActive = isSpeakerOutput
        val shouldEnable = isEffectivelyEnabled()
        try {
            equalizer?.enabled = shouldEnable
            bassBoost?.enabled = shouldEnable
            virtualizer?.enabled = shouldEnable && _virtualizerStrength.value > 0
        } catch (_: Exception) {
        }
        updateProcessingStatus()
    }

    private fun isEffectivelyEnabled(): Boolean {
        val bypassedBySpeaker = _disableEffectsOnSpeaker.value && isBuiltInSpeakerActive
        return _isEnabled.value && !bypassedBySpeaker && !isFlatBypassPresetActive()
    }

    private fun updateProcessingStatus() {
        _processingStatus.value = when {
            !_isSupported.value -> ProcessingStatus.UNAVAILABLE
            !isEffectivelyEnabled() -> ProcessingStatus.OFF
            equalizer != null -> ProcessingStatus.DEVICE_EFFECT
            else -> ProcessingStatus.SOFTWARE
        }
        recalculatePreampScalar()
    }

    fun attachSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq

            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            _bandLevelRange.value = minLevel to maxLevel

            val numBands = eq.numberOfBands
            _hardwareCapabilities.value = _hardwareCapabilities.value.copy(
                hasEqualizer = true,
                equalizerBandsCount = numBands.toInt()
            )

            val previousBands = _bands.value
            val bandList = mutableListOf<EqBand>()
            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex) / 1000 // mHz to Hz
                val formatted = if (centerFreq >= 1000) "${centerFreq / 1000}k" else "$centerFreq"

                val priorLevel = previousBands.getOrNull(i)?.levelMb ?: eq.getBandLevel(bandIndex)
                val clamped = priorLevel.coerceIn(minLevel, maxLevel)
                try {
                    eq.setBandLevel(bandIndex, clamped)
                } catch (_: Exception) {
                }

                bandList.add(
                    EqBand(
                        index = bandIndex,
                        centerFreqHz = centerFreq,
                        levelMb = clamped,
                        formattedFreq = formatted
                    )
                )
            }
            _bands.value = bandList

            // Bass boost attachment
            try {
                val bb = BassBoost(0, audioSessionId)
                bassBoost = bb
                if (bb.strengthSupported) {
                    bb.setStrength(_bassBoostStrength.value)
                }
                bb.enabled = _isEnabled.value && _activePresetName.value != "Studio Reference"
            } catch (_: Exception) {
            }

            // Virtualizer attachment where supported
            try {
                val virt = Virtualizer(0, audioSessionId)
                virtualizer = virt
                if (virt.strengthSupported) {
                    virt.setStrength(_virtualizerStrength.value)
                }
                virt.enabled = _isEnabled.value && _activePresetName.value != "Studio Reference" && _virtualizerStrength.value > 0
            } catch (_: Exception) {
            }

            val shouldEnable = isEffectivelyEnabled()
            eq.enabled = shouldEnable
            _isSupported.value = true
            reapplyActiveEffects()
            updateProcessingStatus()
        } catch (e: Exception) {
            _isSupported.value = false
            updateProcessingStatus()
            release()
        }
    }

    /**
     * Re-applies the active mode curve or custom band levels to the attached audio session
     * without recreating or leaking Equalizer/BassBoost instances across track transitions.
     */
    fun reapplyActiveEffects() {
        val activeName = _activePresetName.value
        val isBuiltIn = BUILT_IN_MODES.any { it.name.equals(activeName, ignoreCase = true) }
        if (isBuiltIn && !activeName.equals("Custom", ignoreCase = true)) {
            applyPreset(activeName)
        } else {
            val eq = equalizer
            val range = _bandLevelRange.value
            val shouldEnable = isEffectivelyEnabled()
            try {
                eq?.enabled = shouldEnable
                bassBoost?.enabled = shouldEnable
            } catch (_: Exception) {
            }
            _bands.value.forEachIndexed { idx, band ->
                val clamped = band.levelMb.coerceIn(range.first, range.second)
                try {
                    if (eq != null && idx < eq.numberOfBands) {
                        eq.setBandLevel(idx.toShort(), clamped)
                    }
                } catch (_: Exception) {
                }
            }
            setBassBoost(_bassBoostStrength.value)
            updateProcessingStatus()
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            val shouldEnable = isEffectivelyEnabled()
            equalizer?.enabled = shouldEnable
            bassBoost?.enabled = shouldEnable
            virtualizer?.enabled = shouldEnable && _virtualizerStrength.value > 0
        } catch (_: Exception) {
        }
        updateProcessingStatus()
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        val range = _bandLevelRange.value
        val clampedLevel = levelMb.coerceIn(range.first, range.second)
        equalizer?.let { eq ->
            try {
                if (bandIndex < eq.numberOfBands) {
                    eq.setBandLevel(bandIndex, clampedLevel)
                }
            } catch (_: Exception) {
            }
        }
        _bands.value = _bands.value.map { band ->
            if (band.index == bandIndex) band.copy(levelMb = clampedLevel) else band
        }
        _activePresetName.value = "Custom"
        updateProcessingStatus()
    }

    fun stepBandLevel(bandIndex: Short, deltaDb: Int) {
        val current = _bands.value.firstOrNull { it.index == bandIndex }?.levelMb ?: 0
        val newLevel = (current + deltaDb * 100).toShort()
        setBandLevel(bandIndex, newLevel)
    }

    fun adjustAllBands(deltaDb: Int) {
        val deltaMb = deltaDb * 100
        _bands.value.forEach { band ->
            val newLevel = (band.levelMb + deltaMb).toShort()
            setBandLevel(band.index, newLevel)
        }
    }

    fun resetToFlat() {
        applyPreset("Balanced")
    }

    fun setBassBoost(strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        try {
            val bb = bassBoost
            if (bb != null && bb.strengthSupported) {
                bb.setStrength(clamped)
            }
        } catch (_: Exception) {
        }
        recalculatePreampScalar()
    }

    fun setVirtualizerStrength(strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        try {
            val virt = virtualizer
            if (virt != null && virt.strengthSupported) {
                virt.setStrength(clamped)
                virt.enabled = _isEnabled.value && _activePresetName.value != "Studio Reference" && clamped > 0
            }
        } catch (_: Exception) {
        }
    }

    fun applyCustomPreset(name: String, bandLevelsCsv: String, bassBoostStrength: Int) {
        _activePresetName.value = name
        val parsedLevels = bandLevelsCsv.split(",").mapNotNull { it.trim().toShortOrNull() }
        val range = _bandLevelRange.value
        val eq = equalizer

        if (_isEnabled.value) {
            try {
                eq?.enabled = true
                bassBoost?.enabled = true
            } catch (_: Exception) {
            }
        }

        _bands.value = _bands.value.mapIndexed { idx, band ->
            val target = parsedLevels.getOrElse(idx) { 0.toShort() }.coerceIn(range.first, range.second)
            try {
                if (eq != null && idx < eq.numberOfBands) {
                    eq.setBandLevel(idx.toShort(), target)
                }
            } catch (_: Exception) {
            }
            band.copy(levelMb = target)
        }
        setBassBoost(bassBoostStrength.toShort())
        updateProcessingStatus()
    }

    /**
     * Toggles or selects one of the 6 basic EQ modes.
     * - If the tapped mode is already active (and not "Balanced"), toggles it off back to "Balanced" (0 dB flat).
     * - If a different mode is tapped, enables EQ and replaces the curve cleanly without stacking gains.
     * Returns the resulting active mode name.
     */
    fun selectBasicMode(modeName: String, toggleIfAlreadyActive: Boolean = true): String {
        val canonical = BASIC_MODES.firstOrNull { it.name.equals(modeName, ignoreCase = true) }?.name ?: modeName
        val isAlreadyActive = _isEnabled.value && _activePresetName.value.equals(canonical, ignoreCase = true)
        return if (toggleIfAlreadyActive && isAlreadyActive && !canonical.equals("Balanced", ignoreCase = true)) {
            setEnabled(true)
            applyPreset("Balanced")
            "Balanced"
        } else {
            setEnabled(true)
            applyPreset(canonical)
            canonical
        }
    }

    /**
     * Applies one of the 6 functional Signature Tones (or Flat / DSP Off utility bypass).
     * Legacy "tone_studio_reference" is migrated to "tone_vesper_signature".
     * Tapping an already-active non-default tone toggles it back to "tone_vesper_signature".
     */
    fun applySignatureTone(toneId: String, toggleIfAlreadyActive: Boolean = true): String {
        val normalizedId = normalizeToneId(toneId)
        val target = if (normalizedId == FLAT_BYPASS_TONE_ID) {
            FLAT_BYPASS_PROFILE
        } else {
            SIGNATURE_TONES.firstOrNull { it.id == normalizedId } ?: SIGNATURE_TONES.first()
        }

        val isAlreadyActive = _isEnabled.value && !_isAbBypassed.value && _activeToneId.value == target.id
        val resolvedTone = if (toggleIfAlreadyActive && isAlreadyActive && target.id != DEFAULT_SIGNATURE_TONE_ID) {
            SIGNATURE_TONES.first()
        } else {
            target
        }

        _isAbBypassed.value = false
        _activeToneId.value = resolvedTone.id
        setEnabled(true)

        val eq = equalizer
        val numBands = eq?.numberOfBands ?: _bands.value.size.toShort()
        val range = _bandLevelRange.value
        val gains = if (resolvedTone.id == FLAT_BYPASS_TONE_ID) {
            List(numBands.toInt()) { 0.toShort() }
        } else {
            calculateCurve(numBands, resolvedTone.weights, range.first, range.second)
        }

        eq?.let {
            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val gain = gains.getOrElse(i) { 0.toShort() }
                try {
                    it.setBandLevel(bandIndex, gain)
                } catch (_: Exception) {
                }
            }
        }

        _bands.value = _bands.value.mapIndexed { index, band ->
            val level = gains.getOrElse(index) { 0.toShort() }
            band.copy(levelMb = level)
        }

        setBassBoost(resolvedTone.bassBoostStrength)
        setVirtualizerStrength(resolvedTone.virtualizerStrength)
        toggleHeadroomProtection(true)
        updateProcessingStatus()
        return resolvedTone.id
    }

    fun applyPreset(presetName: String) {
        val canonicalName = BUILT_IN_MODES.firstOrNull { it.name.equals(presetName, ignoreCase = true) }?.name ?: presetName
        _activePresetName.value = canonicalName
        _isAbBypassed.value = false
        val eq = equalizer
        val numBands = eq?.numberOfBands ?: _bands.value.size.toShort()
        val range = _bandLevelRange.value
        val maxGain = range.second
        val minGain = range.first

        if (canonicalName == "Studio Reference" || canonicalName == "Flat / DSP Off") {
            try {
                eq?.enabled = false
                bassBoost?.enabled = false
                virtualizer?.enabled = false
            } catch (_: Exception) {
            }
            _bassBoostStrength.value = 0
            _virtualizerStrength.value = 0
            _bands.value = _bands.value.map { it.copy(levelMb = 0) }
            updateProcessingStatus()
            return
        }

        if (_isEnabled.value) {
            try {
                eq?.enabled = true
                bassBoost?.enabled = true
            } catch (_: Exception) {
            }
        }

        val gains = when (canonicalName) {
            "Balanced" -> List(numBands.toInt()) { 0.toShort() }
            "VESPER Signature" -> calculateCurve(numBands, listOf(0.18f, 0.14f, 0.04f, 0.16f, 0.22f), minGain, maxGain)
            "Bass" -> calculateCurve(numBands, listOf(0.42f, 0.30f, 0.08f, 0.0f, -0.04f), minGain, maxGain)
            "Vocal" -> calculateCurve(numBands, listOf(-0.12f, 0.0f, 0.28f, 0.34f, 0.14f), minGain, maxGain)
            "Rock" -> calculateCurve(numBands, listOf(0.32f, 0.20f, -0.04f, 0.20f, 0.30f), minGain, maxGain)
            "Pop" -> calculateCurve(numBands, listOf(0.24f, 0.16f, 0.06f, 0.18f, 0.26f), minGain, maxGain)
            "Acoustic" -> calculateCurve(numBands, listOf(0.16f, 0.10f, 0.04f, 0.18f, 0.24f), minGain, maxGain)
            "Warm" -> calculateCurve(numBands, listOf(0.18f, 0.26f, 0.16f, 0.0f, -0.14f), minGain, maxGain)
            "Classical" -> calculateCurve(numBands, listOf(0.20f, 0.10f, -0.04f, 0.14f, 0.24f), minGain, maxGain)
            "Electronic" -> calculateCurve(numBands, listOf(0.40f, 0.26f, -0.06f, 0.16f, 0.34f), minGain, maxGain)
            "Cinematic" -> calculateCurve(numBands, listOf(0.36f, 0.24f, 0.08f, 0.20f, 0.32f), minGain, maxGain)
            else -> List(numBands.toInt()) { 0.toShort() }
        }

        eq?.let {
            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val gain = gains.getOrElse(i) { 0.toShort() }
                try {
                    it.setBandLevel(bandIndex, gain)
                } catch (_: Exception) {
                }
            }
        }

        when (canonicalName) {
            "VESPER Signature" -> setBassBoost(160)
            "Bass" -> setBassBoost(380)
            "Vocal" -> setBassBoost(60)
            "Rock" -> setBassBoost(260)
            "Pop" -> setBassBoost(200)
            "Acoustic" -> setBassBoost(120)
            "Warm" -> setBassBoost(180)
            "Cinematic" -> setBassBoost(300)
            "Electronic" -> setBassBoost(300)
            else -> setBassBoost(0)
        }

        _bands.value = _bands.value.mapIndexed { index, band ->
            val level = gains.getOrElse(index) { 0.toShort() }
            band.copy(levelMb = level)
        }
        updateProcessingStatus()
    }

    private fun calculateCurve(numBands: Short, weights: List<Float>, min: Short, max: Short): List<Short> {
        return (0 until numBands).map { i ->
            val ratio = if (weights.size == 1) 0f else (i.toFloat() / (numBands - 1).coerceAtLeast(1)) * (weights.size - 1)
            val index0 = ratio.toInt().coerceIn(0, weights.size - 1)
            val index1 = (index0 + 1).coerceIn(0, weights.size - 1)
            val frac = ratio - index0
            val weight = weights[index0] * (1 - frac) + weights[index1] * frac

            if (weight >= 0) {
                (weight * max).roundToInt().toShort()
            } else {
                ((-weight) * min).roundToInt().toShort()
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        try {
            virtualizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
        updateProcessingStatus()
    }
}
