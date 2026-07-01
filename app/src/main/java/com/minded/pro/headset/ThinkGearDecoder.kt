package com.minded.pro.headset

/**
 * Decodes the NeuroSky ThinkGear serial stream into [NeuralEvent]s.
 *
 * ThinkGear is a public hardware wire format. A frame looks like:
 *
 * ```
 *   0xAA 0xAA  <length>  <payload bytes...>  <checksum>
 * ```
 *
 * The payload is a back-to-back list of rows. Each row is an optional run of
 * `0x55` extended-code markers, then a one-byte code, then — only when the code
 * is `>= 0x80` — a one-byte value length, then the value bytes. The trailing
 * checksum is the low byte of the bit-inverted sum of every payload byte.
 *
 * Bytes arrive in arbitrary chunks from the Bluetooth socket, so this decoder
 * is fed one byte at a time and keeps its position in a small state machine.
 */
class ThinkGearDecoder(private val sink: (NeuralEvent) -> Unit) {

    private enum class Phase { AWAIT_SYNC_1, AWAIT_SYNC_2, AWAIT_LENGTH, READ_PAYLOAD, READ_CHECKSUM }

    private var phase = Phase.AWAIT_SYNC_1
    private var frameLength = 0
    private var filled = 0
    private val frame = ByteArray(MAX_FRAME)

    /** Feeds the first [count] bytes of [chunk] through the decoder. */
    fun feed(chunk: ByteArray, count: Int) {
        for (i in 0 until count) {
            absorb(chunk[i].toInt() and 0xFF)
        }
    }

    private fun absorb(byte: Int) {
        when (phase) {
            Phase.AWAIT_SYNC_1 ->
                if (byte == SYNC) phase = Phase.AWAIT_SYNC_2

            Phase.AWAIT_SYNC_2 ->
                phase = if (byte == SYNC) Phase.AWAIT_LENGTH else Phase.AWAIT_SYNC_1

            Phase.AWAIT_LENGTH -> when {
                byte == SYNC -> Unit                       // still inside the sync run
                byte > MAX_FRAME -> phase = Phase.AWAIT_SYNC_1   // implausible length
                else -> {
                    frameLength = byte
                    filled = 0
                    phase = if (byte == 0) Phase.READ_CHECKSUM else Phase.READ_PAYLOAD
                }
            }

            Phase.READ_PAYLOAD -> {
                frame[filled++] = byte.toByte()
                if (filled == frameLength) phase = Phase.READ_CHECKSUM
            }

            Phase.READ_CHECKSUM -> {
                if (checksum() == byte) emitRows()
                phase = Phase.AWAIT_SYNC_1
            }
        }
    }

    private fun checksum(): Int {
        var sum = 0
        for (i in 0 until frameLength) sum += frame[i].toInt() and 0xFF
        return sum.inv() and 0xFF
    }

    /** Walks the validated payload and emits one event per recognised row. */
    private fun emitRows() {
        var i = 0
        while (i < frameLength) {
            while (i < frameLength && unsigned(i) == EXTENDED_CODE) i++
            if (i >= frameLength) return

            val code = unsigned(i++)
            val valueLength = if (code >= 0x80) {
                if (i >= frameLength) return
                unsigned(i++)
            } else {
                1
            }
            if (i + valueLength > frameLength) return        // truncated row

            translate(code, i, valueLength)
            i += valueLength
        }
    }

    private fun translate(code: Int, at: Int, length: Int) {
        when (code) {
            CODE_POOR_SIGNAL -> sink(NeuralEvent.SignalQuality(unsigned(at)))
            CODE_ATTENTION -> sink(NeuralEvent.Attention(unsigned(at)))
            CODE_MEDITATION -> sink(NeuralEvent.Meditation(unsigned(at)))
            CODE_BLINK -> sink(NeuralEvent.Blink(unsigned(at)))

            CODE_RAW_WAVE -> if (length >= 2) {
                var sample = (unsigned(at) shl 8) or unsigned(at + 1)
                if (sample >= 0x8000) sample -= 0x10000      // sign-extend 16-bit
                sink(NeuralEvent.RawSample(sample))
            }

            CODE_BAND_POWERS -> if (length >= 24) {
                val band = IntArray(8) { k ->
                    val p = at + k * 3
                    (unsigned(p) shl 16) or (unsigned(p + 1) shl 8) or unsigned(p + 2)
                }
                sink(
                    NeuralEvent.Spectrum(
                        BandPowers(
                            delta = band[0], theta = band[1],
                            lowAlpha = band[2], highAlpha = band[3],
                            lowBeta = band[4], highBeta = band[5],
                            lowGamma = band[6], midGamma = band[7],
                        ),
                    ),
                )
            }
        }
    }

    private fun unsigned(index: Int): Int = frame[index].toInt() and 0xFF

    private companion object {
        const val SYNC = 0xAA
        const val EXTENDED_CODE = 0x55
        const val MAX_FRAME = 169

        const val CODE_POOR_SIGNAL = 0x02
        const val CODE_ATTENTION = 0x04
        const val CODE_MEDITATION = 0x05
        const val CODE_BLINK = 0x16
        const val CODE_RAW_WAVE = 0x80
        const val CODE_BAND_POWERS = 0x83
    }
}
