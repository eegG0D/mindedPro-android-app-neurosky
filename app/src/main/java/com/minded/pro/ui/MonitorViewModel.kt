package com.minded.pro.ui

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minded.pro.headset.BandPowers
import com.minded.pro.headset.HeadsetLink
import com.minded.pro.headset.HeadsetState
import com.minded.pro.headset.NeuralEvent
import com.minded.pro.interpret.BandInterpreter
import com.minded.pro.interpret.BandReading
import com.minded.pro.interpret.RawLexicon
import com.minded.pro.session.SessionFile
import com.minded.pro.session.SessionRecorder
import com.minded.pro.session.SessionSample
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the monitor screen draws, as one immutable snapshot. */
data class MonitorUiState(
    val headset: HeadsetState = HeadsetState.Idle,
    val signalNoise: Int = 200,
    val attention: Int = 0,
    val meditation: Int = 0,
    val blinkStrength: Int = 0,
    val rawWord: String = "—",
    val bands: BandPowers = BandPowers.ZERO,
    val bandReadings: List<BandReading> = BandInterpreter.interpret(BandPowers.ZERO),
    val waveform: List<Int> = emptyList(),
    val recording: Boolean = false,
    val recordedRows: Int = 0,
    val sessionStartedAt: Long = 0L,
    val lastSession: SessionFile? = null,
) {
    /** Sensor contact as a 0..100 percentage (100 = clean contact). */
    val contactPercent: Int
        get() = 100 - (signalNoise.coerceIn(0, 200) * 100 / 200)

    /** True once the headset is delivering live data. */
    val isLive: Boolean
        get() = headset == HeadsetState.Streaming

    /** The band currently standing out the most. */
    val dominantBand: BandReading?
        get() = BandInterpreter.dominant(bandReadings)
}

/**
 * Drives the Minded Pro monitor: owns the [HeadsetLink], folds incoming
 * [NeuralEvent]s into [MonitorUiState], turns numbers into words through the
 * interpretation layer, and manages session recording.
 *
 * Raw samples arrive ~512 times a second; to keep recomposition cheap the
 * waveform and word are published every [WAVE_PUBLISH_EVERY] samples, while
 * every sample is still written to an active recording.
 */
class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    private val link = HeadsetLink(
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter,
    )
    private val recorder = SessionRecorder()

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private val waveform = ArrayDeque<Int>()
    private var rawTick = 0
    private var started = false
    private var blinkClear: Job? = null

    init {
        // Load the amplitude→word table (eeg_map2.csv) once, before any sample.
        RawLexicon.load(app)
    }

    /** Called once the BLUETOOTH_CONNECT permission has been granted. */
    fun onConnectAllowed() {
        if (started) return
        started = true
        link.start(viewModelScope)
        viewModelScope.launch {
            link.state.collect { state -> _uiState.update { it.copy(headset = state) } }
        }
        viewModelScope.launch {
            link.events.collect(::reduce)
        }
    }

    private fun reduce(event: NeuralEvent) {
        when (event) {
            is NeuralEvent.SignalQuality ->
                _uiState.update { it.copy(signalNoise = event.noise) }

            is NeuralEvent.Attention ->
                _uiState.update { it.copy(attention = event.level) }

            is NeuralEvent.Meditation ->
                _uiState.update { it.copy(meditation = event.level) }

            is NeuralEvent.Spectrum ->
                _uiState.update {
                    it.copy(
                        bands = event.bands,
                        bandReadings = BandInterpreter.interpret(event.bands),
                    )
                }

            is NeuralEvent.Blink -> onBlink(event.strength)

            is NeuralEvent.RawSample -> onRawSample(event.amplitude)
        }
    }

    private fun onBlink(strength: Int) {
        _uiState.update { it.copy(blinkStrength = strength) }
        blinkClear?.cancel()
        blinkClear = viewModelScope.launch {
            delay(BLINK_HOLD_MS)
            _uiState.update { it.copy(blinkStrength = 0) }
        }
    }

    private fun onRawSample(amplitude: Int) {
        waveform.addLast(amplitude)
        while (waveform.size > WAVE_CAPACITY) waveform.removeFirst()

        val state = _uiState.value
        if (state.recording) {
            recorder.append(
                SessionSample(
                    signalNoise = state.signalNoise,
                    amplitude = amplitude,
                    rawWord = RawLexicon.wordFor(amplitude),
                    attention = state.attention,
                    meditation = state.meditation,
                    blink = state.blinkStrength,
                    bands = state.bands,
                    bandStates = state.bandReadings.map { it.text },
                ),
            )
        }

        if (++rawTick % WAVE_PUBLISH_EVERY == 0) {
            _uiState.update {
                it.copy(
                    rawWord = RawLexicon.wordFor(amplitude),
                    waveform = waveform.toList(),
                    recordedRows = recorder.rowCount,
                )
            }
        }
    }

    /** Starts a recording if idle, otherwise stops and finalises the file. */
    fun toggleRecording() {
        val state = _uiState.value
        if (state.recording) {
            val file = recorder.finish()
            _uiState.update {
                it.copy(
                    recording = false,
                    recordedRows = file?.rowCount ?: it.recordedRows,
                    lastSession = file,
                )
            }
        } else if (recorder.begin(getApplication())) {
            _uiState.update {
                it.copy(
                    recording = true,
                    recordedRows = 0,
                    sessionStartedAt = System.currentTimeMillis(),
                    lastSession = null,
                )
            }
        }
    }

    override fun onCleared() {
        recorder.finish()
        link.shutdown()
    }

    private companion object {
        /** Raw samples kept for the live waveform (~0.5 s at 512 Hz). */
        const val WAVE_CAPACITY = 240

        /** Publish the waveform / word to the UI once per this many samples. */
        const val WAVE_PUBLISH_EVERY = 6

        /** How long a blink stays lit on screen. */
        const val BLINK_HOLD_MS = 650L
    }
}
