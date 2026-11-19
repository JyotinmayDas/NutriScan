package com.example.widgets

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import java.util.Locale

/**
 * Controller to handle device microphone audio capture, runtime permissions,
 * and speech-to-text recognition for search bars and input fields.
 */
class VoiceInputController(
    val isListening: Boolean,
    val rmsDbLevel: Float,
    val statusMessage: String,
    val startListening: () -> Unit,
    val stopListening: () -> Unit
)

/**
 * Remembers and manages SpeechRecognizer lifecycle and runtime microphone permission
 * to provide live audio transcription for food search and logging.
 */
@Composable
fun rememberVoiceInputController(
    onSpokenText: (String) -> Unit
): VoiceInputController {
    val context = LocalContext.current

    var isListening by remember { mutableStateOf(false) }
    var rmsDbLevel by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf("Tap mic to speak") }

    // Fallback System Speech Intent Launcher
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            if (text.isNotBlank()) {
                onSpokenText(text)
                statusMessage = "Heard: $text"
            }
        } else {
            statusMessage = "Voice search cancelled"
        }
    }

    fun launchSystemIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a food item (e.g., Rice, Dal, Eggs, Chicken)")
            }
            systemSpeechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice recognition service not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun startListeningInternal() {
        if (speechRecognizer != null) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                isListening = true
                statusMessage = "Listening... Speak food name"
                speechRecognizer.startListening(intent)
            } catch (_: Exception) {
                launchSystemIntent()
            }
        } else {
            launchSystemIntent()
        }
    }

    fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusMessage = "Listening... Speak now"
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsDbLevel = rmsdB.coerceIn(0f, 10f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                rmsDbLevel = 0f
            }

            override fun onError(error: Int) {
                isListening = false
                rmsDbLevel = 0f
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    statusMessage = "Microphone permission required"
                } else if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    statusMessage = "No speech detected"
                } else {
                    // Fall back to system dialog if on-device listener encountered an issue
                    launchSystemIntent()
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                rmsDbLevel = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim().orEmpty()
                if (recognized.isNotBlank()) {
                    onSpokenText(recognized)
                    statusMessage = "Heard: $recognized"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    onSpokenText(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningInternal()
        } else {
            Toast.makeText(context, "Microphone permission is required to search food with voice", Toast.LENGTH_SHORT).show()
        }
    }

    val requestAndStart: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListeningInternal()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    return remember(isListening, rmsDbLevel, statusMessage) {
        VoiceInputController(
            isListening = isListening,
            rmsDbLevel = rmsDbLevel,
            statusMessage = statusMessage,
            startListening = requestAndStart,
            stopListening = ::stopListeningInternal
        )
    }
}

/**
 * Animated Microphone / Waveform Action Button designed for Search Bars.
 */
@Composable
fun VoiceSearchIconButton(
    controller: VoiceInputController,
    modifier: Modifier = Modifier,
    testTag: String = "voice_search_mic_button"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (controller.isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    IconButton(
        onClick = {
            if (controller.isListening) {
                controller.stopListening()
            } else {
                controller.startListening()
            }
        },
        modifier = modifier
            .size(48.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (controller.isListening) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(FreshGreenPrimary.copy(alpha = 0.22f))
                        .border(1.dp, LimeAccent, CircleShape)
                )
            }
            Icon(
                imageVector = if (controller.isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                contentDescription = if (controller.isListening) "Listening via microphone" else "Input food via voice",
                tint = if (controller.isListening) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
