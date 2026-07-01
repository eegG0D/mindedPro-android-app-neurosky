package com.minded.pro.interpret

import android.content.Context

/**
 * Turns a raw EEG amplitude sample into a single word by looking it up in the
 * bundled `eeg_map2.csv` table.
 *
 * Each row of the CSV is `amplitude,word,index` — column A is the raw EEG
 * amplitude and column B is the word it speaks. The table covers a contiguous
 * window of amplitudes; any sample outside that window is folded back into
 * range so the same amplitude always speaks the same word.
 */
object RawLexicon {

    private const val ASSET_NAME = "eeg_map2.csv"

    /** Words from column B, indexed by `amplitude - minAmplitude`. */
    private var words: Array<String> = emptyArray()

    /** Lowest amplitude (column A) present in the table. */
    private var minAmplitude = 0

    /** Number of distinct words loaded from the table. */
    val size: Int get() = words.size

    /** True once [load] has populated the table from assets. */
    val isLoaded: Boolean get() = words.isNotEmpty()

    /**
     * Loads `eeg_map2.csv` from the app assets. Safe to call repeatedly — the
     * table is parsed only once.
     */
    fun load(context: Context) {
        if (isLoaded) return
        val rows = ArrayList<Pair<Int, String>>(6_000)
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            reader.forEachLine { line ->
                val parts = line.split(',')
                if (parts.size >= 2) {
                    val amplitude = parts[0].trim().toIntOrNull()
                    val word = parts[1].trim()
                    if (amplitude != null && word.isNotEmpty()) {
                        rows.add(amplitude to word)
                    }
                }
            }
        }
        if (rows.isEmpty()) return
        rows.sortBy { it.first }
        minAmplitude = rows.first().first
        words = Array(rows.size) { rows[it].second }
    }

    /** Returns the word for [amplitude]; every amplitude maps deterministically. */
    fun wordFor(amplitude: Int): String {
        val span = words.size
        if (span == 0) return "—"
        val index = (((amplitude - minAmplitude) % span) + span) % span
        return words[index]
    }
}
