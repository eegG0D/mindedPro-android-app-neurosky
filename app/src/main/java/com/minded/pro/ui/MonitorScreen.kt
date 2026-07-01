package com.minded.pro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minded.pro.headset.HeadsetState
import com.minded.pro.interpret.BandReading
import com.minded.pro.interpret.calmWord
import com.minded.pro.interpret.focusWord
import com.minded.pro.session.SessionFile
import com.minded.pro.session.shareSession
import com.minded.pro.ui.components.ContactRing
import com.minded.pro.ui.components.MetricGauge
import com.minded.pro.ui.components.WaveformChart
import com.minded.pro.ui.theme.AttentionAccent
import com.minded.pro.ui.theme.BlinkAccent
import com.minded.pro.ui.theme.MeditationAccent
import com.minded.pro.ui.theme.MutedText
import com.minded.pro.ui.theme.ProGold
import com.minded.pro.ui.theme.SignalAccent
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * The single screen of Minded Pro: a verbal dashboard that turns one
 * headset's neural data into plain language, with a session recorder below.
 */
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MonitorContent(
        state = state,
        onToggleRecording = viewModel::toggleRecording,
        modifier = modifier,
    )
}

@Composable
private fun MonitorContent(
    state: MonitorUiState,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Header(
            headset = state.headset,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WordCard(state)
            MindStateCard(state)
            GaugeRow(state)
            BandReadingsCard(state)
            SignalCard(state)
            WaveformCard(state)
            Spacer(Modifier.height(4.dp))
        }
        RecordingBar(
            state = state,
            onToggleRecording = onToggleRecording,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        )
    }
}

// --- Header --------------------------------------------------------------

@Composable
private fun Header(headset: HeadsetState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Minded",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProGold,
                )
            }
            Text(
                text = "Verbal neural monitor",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
            )
        }
        StatusPill(headset)
    }
}

@Composable
private fun StatusPill(headset: HeadsetState) {
    val accent = when (headset) {
        HeadsetState.Streaming -> MeditationAccent
        HeadsetState.Searching, HeadsetState.Dropped -> AttentionAccent
        HeadsetState.Idle -> MutedText
        else -> BlinkAccent
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = headset.caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// --- Shared card scaffolding --------------------------------------------

@Composable
private fun MindedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.5.sp,
        color = MutedText,
    )
}

// --- Cards ---------------------------------------------------------------

/** The signature card — the headset's raw stream rendered as a single word. */
@Composable
private fun WordCard(state: MonitorUiState) {
    MindedCard(Modifier.fillMaxWidth()) {
        SectionLabel("EEG speaks")
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.rawWord,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = ProGold,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "spoken from the raw EEG stream",
            style = MaterialTheme.typography.labelMedium,
            color = MutedText,
        )
    }
}

/** The band standing out the most, named and described in words. */
@Composable
private fun MindStateCard(state: MonitorUiState) {
    val dominant = state.dominantBand
    MindedCard(Modifier.fillMaxWidth()) {
        SectionLabel("Right now")
        Spacer(Modifier.height(12.dp))
        if (dominant == null) {
            Text(
                text = "Waiting for a reading…",
                style = MaterialTheme.typography.titleMedium,
                color = MutedText,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dominant.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ProGold,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${dominant.name} band leading",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText,
                    )
                    Text(
                        text = dominant.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun GaugeRow(state: MonitorUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VerbalGaugeCard(
            title = "Attention",
            value = state.attention,
            word = focusWord(state.attention),
            accent = AttentionAccent,
            modifier = Modifier.weight(1f),
        )
        VerbalGaugeCard(
            title = "Meditation",
            value = state.meditation,
            word = calmWord(state.meditation),
            accent = MeditationAccent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VerbalGaugeCard(
    title: String,
    value: Int,
    word: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    MindedCard(modifier) {
        SectionLabel(title)
        Spacer(Modifier.height(10.dp))
        MetricGauge(
            value = value,
            accent = accent,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = word,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
        )
    }
}

/** Every band, each translated from a number into a state phrase. */
@Composable
private fun BandReadingsCard(state: MonitorUiState) {
    MindedCard(Modifier.fillMaxWidth()) {
        SectionLabel("Band readings")
        Spacer(Modifier.height(6.dp))
        state.bandReadings.forEachIndexed { index, reading ->
            BandRow(reading)
            if (index < state.bandReadings.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun BandRow(reading: BandReading) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = reading.symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ProGold,
            modifier = Modifier.width(40.dp),
        )
        Text(
            text = reading.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SignalCard(state: MonitorUiState) {
    val percent = state.contactPercent
    val accent = when {
        percent >= 75 -> MeditationAccent
        percent >= 40 -> AttentionAccent
        else -> BlinkAccent
    }
    val verdict = when {
        percent >= 75 -> "Clean contact"
        percent >= 40 -> "Adjust the headband"
        else -> "No skin contact"
    }
    MindedCard(Modifier.fillMaxWidth()) {
        SectionLabel("Signal")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactRing(percent = percent, accent = accent)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun WaveformCard(state: MonitorUiState) {
    MindedCard(Modifier.fillMaxWidth()) {
        SectionLabel("Raw EEG")
        Spacer(Modifier.height(14.dp))
        WaveformChart(
            samples = state.waveform,
            accent = SignalAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
        )
    }
}

// --- Recording bar -------------------------------------------------------

@Composable
private fun RecordingBar(
    state: MonitorUiState,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp)) {
            val finished = state.lastSession
            if (finished != null && !state.recording) {
                ShareRow(
                    session = finished,
                    onShare = { shareSession(context, finished.file) },
                )
                Spacer(Modifier.height(12.dp))
            }
            RecordButton(state = state, onClick = onToggleRecording)
        }
    }
}

@Composable
private fun RecordButton(state: MonitorUiState, onClick: () -> Unit) {
    val recording = state.recording
    val elapsed by produceState(
        initialValue = 0L,
        key1 = recording,
        key2 = state.sessionStartedAt,
    ) {
        while (recording) {
            value = System.currentTimeMillis() - state.sessionStartedAt
            delay(500)
        }
    }
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (recording) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            imageVector = if (recording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
            contentDescription = null,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (recording) {
                "Stop  ·  ${formatElapsed(elapsed)}  ·  ${state.recordedRows} rows"
            } else {
                "Start recording"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ShareRow(session: SessionFile, onShare: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Last session saved",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${session.rowCount} rows · ${session.file.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
            )
        }
        TextButton(onClick = onShare) {
            Icon(Icons.Rounded.IosShare, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Share")
        }
    }
}

// --- Permission gate -----------------------------------------------------

/** Shown until the BLUETOOTH_CONNECT permission has been granted. */
@Composable
fun PermissionPrompt(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Bluetooth,
            contentDescription = null,
            tint = SignalAccent,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Bluetooth permission needed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Minded Pro streams data from your EEG headset over Bluetooth. " +
                "Pair the headset in system settings, then allow the connection here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onGrant,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.wrapContentWidth(),
        ) {
            Text("Grant permission")
        }
    }
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
