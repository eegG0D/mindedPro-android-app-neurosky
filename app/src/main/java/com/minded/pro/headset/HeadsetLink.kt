package com.minded.pro.headset

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** Lifecycle of the connection to a physical headset, surfaced to the UI. */
enum class HeadsetState(val caption: String) {
    Idle("Standby"),
    BluetoothOff("Turn on Bluetooth"),
    PermissionNeeded("Bluetooth permission needed"),
    NoHeadsetPaired("Pair a headset in Settings"),
    Searching("Searching for headset…"),
    NotReachable("Headset out of range"),
    Streaming("Streaming"),
    Dropped("Connection lost — retrying"),
}

/**
 * Owns the Bluetooth Classic (SPP) link to a NeuroSky ThinkGear headset.
 *
 * Once [start] is called the link keeps itself alive: it scans the bonded
 * devices, opens the first one that accepts an RFCOMM socket, streams decoded
 * [NeuralEvent]s, and — if the socket drops — waits briefly and tries again.
 * The caller observes [state] for connection status and [events] for data.
 */
class HeadsetLink(private val adapter: BluetoothAdapter?) {

    private val _state = MutableStateFlow(HeadsetState.Idle)
    val state: StateFlow<HeadsetState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<NeuralEvent>(extraBufferCapacity = 512)
    val events: SharedFlow<NeuralEvent> = _events.asSharedFlow()

    private var pump: Job? = null

    @Volatile
    private var liveSocket: BluetoothSocket? = null

    /** Begins (or resumes) the self-healing connection loop. */
    fun start(scope: CoroutineScope) {
        if (pump?.isActive == true) return
        pump = scope.launch(Dispatchers.IO) { runForever() }
    }

    /** Tears the link down; safe to call more than once. */
    fun shutdown() {
        pump?.cancel()
        pump = null
        closeSocket()
        _state.value = HeadsetState.Idle
    }

    private suspend fun runForever() {
        if (adapter == null) {
            _state.value = HeadsetState.BluetoothOff
            return
        }
        while (coroutineContext.isActive) {
            if (!adapter.isEnabled) {
                _state.value = HeadsetState.BluetoothOff
                delay(RETRY_DELAY_MS)
                continue
            }

            val candidates = bondedDevices() ?: run {
                _state.value = HeadsetState.PermissionNeeded
                return
            }
            if (candidates.isEmpty()) {
                _state.value = HeadsetState.NoHeadsetPaired
                delay(RETRY_DELAY_MS)
                continue
            }

            _state.value = HeadsetState.Searching
            val socket = openFirstReachable(candidates)
            if (socket == null) {
                _state.value = HeadsetState.NotReachable
                delay(RETRY_DELAY_MS)
                continue
            }

            liveSocket = socket
            _state.value = HeadsetState.Streaming
            pumpUntilClosed(socket)
            closeSocket()

            if (!coroutineContext.isActive) break
            _state.value = HeadsetState.Dropped
            delay(RETRY_DELAY_MS)
        }
    }

    @SuppressLint("MissingPermission") // BLUETOOTH_CONNECT is verified before start().
    private fun bondedDevices(): Set<BluetoothDevice>? = try {
        adapter?.bondedDevices ?: emptySet()
    } catch (e: SecurityException) {
        Log.w(TAG, "Bluetooth permission missing", e)
        null
    }

    @SuppressLint("MissingPermission")
    private fun openFirstReachable(devices: Set<BluetoothDevice>): BluetoothSocket? {
        try {
            adapter?.cancelDiscovery()
        } catch (_: SecurityException) {
        }
        for (device in devices) {
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                Log.i(TAG, "Linked to ${device.address}")
                return socket
            } catch (e: IOException) {
                Log.d(TAG, "Skipping ${device.address}: ${e.message}")
                socket?.let { runCatching { it.close() } }
            } catch (e: SecurityException) {
                Log.w(TAG, "Bluetooth permission missing during connect", e)
                socket?.let { runCatching { it.close() } }
                return null
            }
        }
        return null
    }

    /** Reads bytes off the socket and feeds the decoder until the link ends. */
    private suspend fun pumpUntilClosed(socket: BluetoothSocket) {
        val decoder = ThinkGearDecoder { _events.tryEmit(it) }
        val buffer = ByteArray(1024)
        try {
            val input = socket.inputStream
            while (coroutineContext.isActive) {
                val read = input.read(buffer)
                if (read < 0) break                  // peer closed the stream
                decoder.feed(buffer, read)
            }
        } catch (e: IOException) {
            Log.d(TAG, "Stream ended: ${e.message}")
        }
    }

    private fun closeSocket() {
        liveSocket?.let { runCatching { it.close() } }
        liveSocket = null
    }

    private companion object {
        const val TAG = "HeadsetLink"
        const val RETRY_DELAY_MS = 2000L

        /** Standard Serial Port Profile UUID — every ThinkGear headset uses it. */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
