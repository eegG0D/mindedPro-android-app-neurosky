package com.minded.pro.session

import android.content.Context
import android.util.Log
import com.minded.pro.headset.BandPowers
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One row of headset data captured at a single instant.
 *
 * The raw EEG and the eight bands are recorded purely as words: [rawWord] is
 * the amplitude's word from `eeg_map2.csv` and [bandStates] holds the full
 * `describe_wave` sentence for each band — no numeric duplicate is written.
 */
data class SessionSample(
    val signalNoise: Int,
    val amplitude: Int,
    val rawWord: String,
    val attention: Int,
    val meditation: Int,
    val blink: Int,
    val bands: BandPowers,
    /** Full describe_wave sentences for the eight bands, in delta..midGamma order. */
    val bandStates: List<String>,
)

/** A finished recording on disk, with the number of rows it holds. */
data class SessionFile(val file: File, val rowCount: Int)

/**
 * Streams [SessionSample]s to a timestamped CSV file under the app's external
 * files directory. One recorder instance is reused across many sessions:
 * [begin] opens a fresh file, [append] adds a row, [finish] closes it.
 *
 * Recording is best-effort — a failed write is logged and dropped rather than
 * crashing the capture.
 */
class SessionRecorder {

    private var writer: BufferedWriter? = null
    private var target: File? = null

    /** Rows written to the file currently open (or last closed). */
    var rowCount: Int = 0
        private set

    /** True while a file is open for writing. */
    val isOpen: Boolean
        get() = writer != null

    /** Opens a new CSV file; returns false if it could not be created. */
    fun begin(context: Context): Boolean {
        finishQuietly()
        return try {
            val folder = File(context.getExternalFilesDir(null), "sessions").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val file = File(folder, "minded-pro-$stamp.csv")
            BufferedWriter(FileWriter(file)).also {
                it.write(HEADER)
                it.newLine()
                writer = it
            }
            target = file
            rowCount = 0
            true
        } catch (e: IOException) {
            Log.e(TAG, "Could not open session file", e)
            writer = null
            target = null
            false
        }
    }

    /** Appends one row, stamped with the current wall-clock time. */
    fun append(sample: SessionSample) {
        val out = writer ?: return
        val states = sample.bandStates
        val row = buildString {
            append(System.currentTimeMillis()).append(',')
            append(sample.signalNoise).append(',')
            append(sample.rawWord).append(',')
            append(sample.attention).append(',')
            append(sample.meditation).append(',')
            append(sample.blink).append(',')
            for (index in 0 until 8) {
                append(states.getOrElse(index) { "" })
                if (index < 7) append(',')
            }
        }
        try {
            out.write(row)
            out.newLine()
            rowCount++
        } catch (e: IOException) {
            Log.e(TAG, "Dropped a session row", e)
        }
    }

    /** Flushes and closes the file, returning a handle to it. */
    fun finish(): SessionFile? {
        val file = target
        finishQuietly()
        return file?.let { SessionFile(it, rowCount) }
    }

    private fun finishQuietly() {
        writer?.let { runCatching { it.flush(); it.close() } }
        writer = null
    }

    private companion object {
        const val TAG = "SessionRecorder"
        const val HEADER =
            "timestamp_ms,signal_noise,raw_word,attention,meditation,blink," +
                "delta_state,theta_state,low_alpha_state,high_alpha_state," +
                "low_beta_state,high_beta_state,low_gamma_state,mid_gamma_state"
    }
}
