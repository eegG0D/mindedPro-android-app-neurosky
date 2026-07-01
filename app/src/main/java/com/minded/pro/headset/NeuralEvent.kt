package com.minded.pro.headset

/**
 * A single decoded reading produced by a NeuroSky ThinkGear headset.
 *
 * The headset emits these at different rates: [RawSample] arrives ~512 times a
 * second, [Attention] / [Meditation] / [SignalQuality] once a second, [Spectrum]
 * once a second, and [Blink] only when an eye-blink is detected.
 */
sealed interface NeuralEvent {

    /**
     * Sensor contact quality, where `0` means a clean skin contact and `200`
     * means the sensor is not touching the head at all.
     */
    data class SignalQuality(val noise: Int) : NeuralEvent

    /** Focused-attention index on a 0..100 scale. */
    data class Attention(val level: Int) : NeuralEvent

    /** Calm-meditation index on a 0..100 scale. */
    data class Meditation(val level: Int) : NeuralEvent

    /** Strength of a detected eye blink, 1..255. */
    data class Blink(val strength: Int) : NeuralEvent

    /** One raw EEG amplitude sample, a signed value roughly in -2048..2047. */
    data class RawSample(val amplitude: Int) : NeuralEvent

    /** The eight band powers, refreshed once per second. */
    data class Spectrum(val bands: BandPowers) : NeuralEvent
}

/**
 * The eight EEG band powers reported by the headset's ASIC, in arbitrary
 * device units. Values span several orders of magnitude.
 */
data class BandPowers(
    val delta: Int,
    val theta: Int,
    val lowAlpha: Int,
    val highAlpha: Int,
    val lowBeta: Int,
    val highBeta: Int,
    val lowGamma: Int,
    val midGamma: Int,
) {
    companion object {
        val ZERO = BandPowers(0, 0, 0, 0, 0, 0, 0, 0)
    }
}

/**
 * Converts a raw amplitude sample to microvolts.
 *
 * This follows the ThinkGear front-end's fixed analog path: a 1.8 V ADC
 * reference, 12-bit resolution (4096 steps) and a gain of 2000.
 */
fun rawToMicrovolts(amplitude: Int): Double =
    amplitude * 1.8 / 4096.0 / 2000.0 * 1_000_000.0
