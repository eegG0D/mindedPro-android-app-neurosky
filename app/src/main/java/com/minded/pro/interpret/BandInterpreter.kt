package com.minded.pro.interpret

import com.minded.pro.headset.BandPowers

/** Three levels a band's power can fall into, from faint to dominant. */
enum class BandTier { Low, Medium, High }

/**
 * A single band translated from a number into one plain-language line.
 *
 * The reading is purely verbal — [text] is the full `describe_wave` sentence
 * and the original numeric power is not carried, so nothing downstream can
 * surface it.
 */
data class BandReading(
    val symbol: String,
    val name: String,
    /** The full sentence, e.g. "Low delta: Mentally alert or stressed". */
    val text: String,
    /** Power relative to this band's "Medium" ceiling — used only to rank bands. */
    val intensity: Float,
)

/**
 * Turns the eight numeric EEG band powers into plain-language readings.
 *
 * Each band is classified into **three** levels — Low, Medium and High — using
 * the per-band thresholds from the `describe_wave` reference script: a value at
 * or below the low cut-off is Low, at or below the high cut-off is Medium, and
 * anything above is High. Each level maps to its full `describe_wave` sentence.
 */
object BandInterpreter {

    private class Profile(
        val symbol: String,
        val name: String,
        /** `[low, high]` cut-offs; `value <= low` is Low, `value <= high` is Medium. */
        val ceilings: IntArray,
        /** `describe_wave` sentences indexed by [BandTier.ordinal]: Low, Medium, High. */
        val descriptions: Array<String>,
    )

    // Profiles in the headset's reporting order: delta .. midGamma.
    private val profiles = listOf(
        Profile(
            "δ", "Delta", intArrayOf(100_000, 5_000_000),
            arrayOf(
                "Low delta: Mentally alert or stressed",
                "Medium delta: Deep relaxation or light sleep",
                "High delta: Deep sleep or unconsciousness",
            ),
        ),
        Profile(
            "θ", "Theta", intArrayOf(100_000, 5_000_000),
            arrayOf(
                "Low theta: Distracted or anxious state",
                "Medium theta: Meditation or creative thinking",
                "High theta: Dream-like states or deep meditation",
            ),
        ),
        Profile(
            "α−", "Low Alpha", intArrayOf(50_000, 2_000_000),
            arrayOf(
                "Low alphaLow: Mentally tense or agitated",
                "Medium alphaLow: Calm and alert",
                "High alphaLow: Deep relaxation or passive awareness",
            ),
        ),
        Profile(
            "α+", "High Alpha", intArrayOf(50_000, 2_000_000),
            arrayOf(
                "Low alphaHigh: Lack of mental coordination",
                "Medium alphaHigh: Coordinated relaxation and focus",
                "High alphaHigh: Flow state or peak creativity",
            ),
        ),
        Profile(
            "β−", "Low Beta", intArrayOf(30_000, 1_000_000),
            arrayOf(
                "Low betaLow: Daydreaming or disengaged",
                "Medium betaLow: Focused thinking and attention",
                "High betaLow: Intense focus or anxiety",
            ),
        ),
        Profile(
            "β+", "High Beta", intArrayOf(30_000, 1_000_000),
            arrayOf(
                "Low betaHigh: Relaxed cognitive state",
                "Medium betaHigh: Alertness and logical thinking",
                "High betaHigh: Hyper-alert or stressed mind",
            ),
        ),
        Profile(
            "γ−", "Low Gamma", intArrayOf(10_000, 500_000),
            arrayOf(
                "Low gammaLow: Low cognitive load",
                "Medium gammaLow: Moderate learning or memory use",
                "High gammaLow: High-level information processing",
            ),
        ),
        Profile(
            "γ+", "Mid Gamma", intArrayOf(10_000, 500_000),
            arrayOf(
                "Low gammaMid: Minimal sensory integration",
                "Medium gammaMid: Cognitive engagement",
                "High gammaMid: Heightened consciousness or insight",
            ),
        ),
    )

    /** Translates [bands] into eight verbal readings, in delta..midGamma order. */
    fun interpret(bands: BandPowers): List<BandReading> {
        val values = intArrayOf(
            bands.delta, bands.theta,
            bands.lowAlpha, bands.highAlpha,
            bands.lowBeta, bands.highBeta,
            bands.lowGamma, bands.midGamma,
        )
        return profiles.mapIndexed { index, profile ->
            val value = values[index]
            val tier = tierOf(value, profile.ceilings)
            BandReading(
                symbol = profile.symbol,
                name = profile.name,
                text = profile.descriptions[tier.ordinal],
                intensity = value.toFloat() / profile.ceilings[1],
            )
        }
    }

    /** The band currently standing out the most, used as the headline state. */
    fun dominant(readings: List<BandReading>): BandReading? =
        readings.maxByOrNull { it.intensity }

    private fun tierOf(value: Int, ceilings: IntArray): BandTier = when {
        value <= ceilings[0] -> BandTier.Low
        value <= ceilings[1] -> BandTier.Medium
        else -> BandTier.High
    }
}

/** A one-word verdict for the 0..100 attention index. */
fun focusWord(level: Int): String = when {
    level < 20 -> "Unfocused"
    level < 45 -> "Drifting"
    level < 70 -> "Engaged"
    level < 88 -> "Focused"
    else -> "Locked in"
}

/** A one-word verdict for the 0..100 meditation index. */
fun calmWord(level: Int): String = when {
    level < 20 -> "Restless"
    level < 45 -> "Unsettled"
    level < 70 -> "Easing"
    level < 88 -> "Calm"
    else -> "Deeply calm"
}
