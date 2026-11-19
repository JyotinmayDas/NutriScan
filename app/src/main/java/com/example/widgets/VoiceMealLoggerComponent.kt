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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.ScannedFoodItem
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.ProteinColor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Structured representation of a Gemini-parsed natural language voice meal description.
 */
data class VoiceParsedMealResult(
    val mealTitle: String,
    val suggestedMealType: String,
    val items: List<ScannedFoodItem>,
    val analysisNotes: String
)

/**
 * Gemini 3.8 Flash structured-JSON parser for converting spoken natural-language food descriptions
 * into structured calorie and macronutrient entries.
 */
object VoiceMealGeminiParser {

    private val voiceMealResponseSchema = Schema.obj(
        mapOf(
            "meal_title" to Schema.string("Concise title summarizing the spoken meal"),
            "suggested_meal_type" to Schema.enumeration(
                listOf("Breakfast", "Lunch", "Dinner", "Snacks"),
                "Best matching meal category based on the spoken description or food types"
            ),
            "food_items" to Schema.array(
                Schema.obj(
                    mapOf(
                        "food_name" to Schema.string("Standardized food item name"),
                        "quantity_g" to Schema.double("Estimated portion weight in grams"),
                        "calories_kcal" to Schema.integer("Estimated calories in kcal for this portion"),
                        "protein_g" to Schema.double("Estimated protein in grams"),
                        "carbohydrates_g" to Schema.double("Estimated total carbohydrates in grams"),
                        "fat_g" to Schema.double("Estimated total fat in grams"),
                        "fiber_g" to Schema.double("Estimated dietary fiber in grams"),
                        "sugar_g" to Schema.double("Estimated total sugar in grams"),
                        "confidence" to Schema.double("Confidence score between 0.0 and 1.0"),
                        "assumptions" to Schema.string("Brief note on portion conversion (e.g., 2 medium rotis ≈ 80g)")
                    )
                )
            ),
            "analysis_notes" to Schema.string("Friendly nutritional summary and coaching note for the parsed voice log")
        )
    )

    private val voiceParserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = voiceMealResponseSchema
            }
        )
    }

    suspend fun parseSpokenFoodDescription(
        spokenText: String,
        preferredLanguage: String = "en"
    ): Result<VoiceParsedMealResult> {
        val cleaned = spokenText.trim()
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please speak or enter a food description first."))
        }

        return try {
            val prompt = """
                You are an expert clinical nutritionist and South Asian & global food parser for NutriScan.
                The user logged their food intake using voice-to-text:
                "$cleaned"
                
                Tasks:
                1. Identify every distinct food or beverage mentioned in the user's natural-language description (supports English, Bengali, Hindi, or mixed phrases such as roti, bhat, dal, dim, chicken curry, oatmeal, coffee, etc.).
                2. Convert household measurements (e.g., "2 rotis", "1 bowl dal", "1 cup rice", "2 boiled eggs", "1 glass milk", "1 plate biryani") into realistic gram weights (quantity_g).
                3. Calculate accurate calories (calories_kcal), protein (protein_g), carbohydrates (carbohydrates_g), fat (fat_g), fiber (fiber_g), and sugar (sugar_g) for the specified portion size.
                4. Infer the suggested_meal_type ("Breakfast", "Lunch", "Dinner", or "Snacks") if mentioned (e.g., "for breakfast", "at lunch"), otherwise choose the most appropriate category.
                5. Provide a concise meal_title and helpful analysis_notes in language code '$preferredLanguage'.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                voiceParserModel.generateContent(prompt)
            }

            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isEmpty()) {
                return Result.failure(IllegalStateException("Gemini returned an empty response. Please try again."))
            }

            val parsed = parseJsonToResult(rawJson)
            if (parsed.items.isEmpty()) {
                return Result.failure(IllegalStateException("Could not detect specific food items from the description. Try mentioning food names and portions."))
            }
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Gemini nutrition parsing timed out. Please check your connection and try again.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Voice nutrition parsing failed: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    private fun parseJsonToResult(rawJson: String): VoiceParsedMealResult {
        val cleaned = rawJson
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = JSONObject(cleaned)
        val mealTitle = root.optString("meal_title", "Voice Logged Meal").ifBlank { "Voice Logged Meal" }
        val suggestedMealType = root.optString("suggested_meal_type", "Lunch").let {
            if (it in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) it else "Lunch"
        }
        val notes = root.optString("analysis_notes", "Calculated from your spoken meal description.")
        val itemsArray = root.optJSONArray("food_items") ?: JSONArray()

        val parsedItems = mutableListOf<ScannedFoodItem>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue
            val name = obj.optString("food_name", "").trim()
            if (name.isEmpty()) continue
            val qtyG = obj.optDouble("quantity_g", 100.0).coerceIn(5.0, 2500.0)
            val protein = obj.optDouble("protein_g", 0.0).coerceAtLeast(0.0)
            val carbs = obj.optDouble("carbohydrates_g", 0.0).coerceAtLeast(0.0)
            val fat = obj.optDouble("fat_g", 0.0).coerceAtLeast(0.0)
            val fiber = obj.optDouble("fiber_g", 0.0).coerceAtLeast(0.0)
            val sugar = obj.optDouble("sugar_g", 0.0).coerceAtLeast(0.0)
            val rawKcal = obj.optInt("calories_kcal", 0)
            val macroKcal = ((protein * 4.0) + (carbs * 4.0) + (fat * 9.0)).roundToInt()
            val finalKcal = if (rawKcal <= 0) macroKcal.coerceAtLeast(10) else rawKcal

            parsedItems.add(
                ScannedFoodItem(
                    foodName = name,
                    quantityG = qtyG,
                    caloriesKcal = finalKcal,
                    proteinG = (protein * 10.0).roundToInt() / 10.0,
                    carbohydratesG = (carbs * 10.0).roundToInt() / 10.0,
                    fatG = (fat * 10.0).roundToInt() / 10.0,
                    fiberG = (fiber * 10.0).roundToInt() / 10.0,
                    sugarG = (sugar * 10.0).roundToInt() / 10.0,
                    confidence = obj.optDouble("confidence", 0.9).coerceIn(0.1, 1.0),
                    assumptions = obj.optString("assumptions", "Estimated standard serving")
                )
            )
        }

        return VoiceParsedMealResult(
            mealTitle = mealTitle,
            suggestedMealType = suggestedMealType,
            items = parsedItems,
            analysisNotes = notes
        )
    }
}

/**
 * Prominent Dashboard Voice-to-Text Food Logger Card & Microphone Trigger Button.
 */
@Composable
fun DashboardVoiceMealLoggerBar(
    onOpenVoiceLoggerModal: (autoStartListening: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        FreshGreenPrimary.copy(alpha = 0.45f),
                        Color(0xFF0EA5E9).copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenVoiceLoggerModal(false) }
            .testTag("dashboard_voice_logger_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FreshGreenPrimary.copy(alpha = 0.14f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Voice Food Logging",
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Voice Meal Logger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FreshGreenPrimary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Hands-Free",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FreshGreenPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Speak your meal to log calories & macros instantly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = { onOpenVoiceLoggerModal(true) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshGreenPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("dashboard_voice_log_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Speak Meal to Log",
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Speak",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Full interactive Voice-to-Text + Gemini AI Nutritional Parser Modal Dialog.
 * Supports:
 * 1. Live on-device SpeechRecognizer with RECORD_AUDIO runtime permission and animated waveform.
 * 2. System RecognizerIntent fallback for any Android environment.
 * 3. Editable natural-language transcript box + sample spoken meal chips (English / Bengali / Hindi).
 * 4. Gemini 3.8 Flash structured-JSON calorie & macronutrient breakdown with portion adjustments.
 * 5. One-tap logging to the user's daily diary (+XP reward).
 */
@Composable
fun VoiceMealLoggerModalDialog(
    selectedDateKey: String,
    preferredLanguage: String = "en",
    autoStartListeningOnOpen: Boolean = false,
    onDismiss: () -> Unit,
    onLogParsedEntries: (List<FoodDiaryEntry>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var spokenTranscript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var rmsDbLevel by remember { mutableFloatStateOf(0f) }
    var speechStatusMessage by remember {
        mutableStateOf("Tap the microphone and describe your meal naturally")
    }
    var isAnalyzingWithGemini by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var parsedMealTitle by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("Lunch") }
    var analysisNotes by remember { mutableStateOf("") }
    val parsedItems = remember { mutableStateListOf<ScannedFoodItem>() }
    var hasAutoStarted by remember { mutableStateOf(false) }

    // System RecognizerIntent fallback launcher
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
            if (recognizedText.isNotEmpty()) {
                spokenTranscript = if (spokenTranscript.isBlank()) {
                    recognizedText
                } else {
                    "$spokenTranscript, $recognizedText"
                }
                speechStatusMessage = "Voice captured! Calculating calories & macros..."
                errorMessage = null
                coroutineScope.launch {
                    isAnalyzingWithGemini = true
                    val res = VoiceMealGeminiParser.parseSpokenFoodDescription(
                        spokenText = spokenTranscript,
                        preferredLanguage = preferredLanguage
                    )
                    isAnalyzingWithGemini = false
                    res.onSuccess { parsed ->
                        parsedMealTitle = parsed.mealTitle
                        selectedMealType = parsed.suggestedMealType
                        analysisNotes = parsed.analysisNotes
                        parsedItems.clear()
                        parsedItems.addAll(parsed.items)
                        speechStatusMessage = "Parsed ${parsed.items.size} food item(s) from your voice input"
                    }.onFailure { err ->
                        errorMessage = err.localizedMessage ?: "Failed to parse voice description."
                    }
                }
            }
        } else {
            speechStatusMessage = "Voice capture cancelled or unavailable. You can also tap a voice sample or type below."
        }
    }

    // Manage on-device SpeechRecognizer instance cleanly
    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun launchSystemSpeechIntent(ctx: Context) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your meal (e.g., '2 rotis, 1 bowl dal, and 1 boiled egg for lunch')"
                )
            }
            systemSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            errorMessage = "Speech recognition service is not installed on this device. Please use the text box or quick voice phrases below."
        }
    }

    fun triggerGeminiParse(textToParse: String) {
        if (textToParse.isBlank()) {
            errorMessage = "Please speak or enter a meal description first."
            return
        }
        errorMessage = null
        coroutineScope.launch {
            isAnalyzingWithGemini = true
            speechStatusMessage = "Calculating calories & macronutrients..."
            val result = VoiceMealGeminiParser.parseSpokenFoodDescription(
                spokenText = textToParse,
                preferredLanguage = preferredLanguage
            )
            isAnalyzingWithGemini = false
            result.onSuccess { parsed ->
                parsedMealTitle = parsed.mealTitle
                selectedMealType = parsed.suggestedMealType
                analysisNotes = parsed.analysisNotes
                parsedItems.clear()
                parsedItems.addAll(parsed.items)
                speechStatusMessage = "Parsed ${parsed.items.size} item(s) • Ready to log!"
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Could not parse nutrition data."
            }
        }
    }

    fun startVoiceRecognition() {
        errorMessage = null
        if (speechRecognizer != null) {
            try {
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                isListening = true
                speechStatusMessage = "Listening... Speak your meal & portion sizes now"
                speechRecognizer.startListening(recognizerIntent)
            } catch (e: Exception) {
                launchSystemSpeechIntent(context)
            }
        } else {
            launchSystemSpeechIntent(context)
        }
    }

    fun stopVoiceRecognition() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        isListening = false
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                speechStatusMessage = "Listening... Speak your meal clearly"
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsDbLevel = rmsdB.coerceIn(0f, 12f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                rmsDbLevel = 0f
            }

            override fun onError(error: Int) {
                isListening = false
                rmsDbLevel = 0f
                // If on-device recognizer fails due to emulator/service lack, offer or fallback cleanly
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    errorMessage = "Microphone permission is required for voice logging."
                } else if (spokenTranscript.isNotBlank()) {
                    speechStatusMessage = "Voice input ready. Tap 'Analyze Meal' below."
                } else {
                    speechStatusMessage = "Could not hear speech clearly. Tap the Mic button again or select a sample phrase."
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                rmsDbLevel = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim().orEmpty()
                if (recognized.isNotEmpty()) {
                    spokenTranscript = recognized
                    triggerGeminiParse(recognized)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    spokenTranscript = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    // Runtime RECORD_AUDIO Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            errorMessage = "Microphone permission was declined. You can still type or tap sample phrases below."
        }
    }

    fun requestMicAndListen() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            startVoiceRecognition()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-start listening if user tapped the direct "Speak" mic button on dashboard
    DisposableEffect(autoStartListeningOnOpen) {
        if (autoStartListeningOnOpen && !hasAutoStarted) {
            hasAutoStarted = true
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                startVoiceRecognition()
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        onDispose { }
    }

    val sampleVoicePhrases = remember {
        listOf(
            "I had 2 whole wheat rotis, 1 bowl masoor dal, and 150g grilled chicken breast for lunch",
            "1 cup cooked white rice, fish curry 120g, and cucumber salad for dinner",
            "2 boiled eggs, 1 bowl oatmeal with milk, and 1 banana for breakfast",
            "1 cup masala chai and 1 samosa for evening snacks"
        )
    }

    Dialog(
        onDismissRequest = {
            stopVoiceRecognition()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("voice_meal_logger_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = FreshGreenPrimary.copy(alpha = 0.14f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Voice Meal Logger",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Speak naturally to calculate calories & macros",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            stopVoiceRecognition()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_voice_logger_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Voice Logger"
                        )
                    }
                }

                // Sleek Animated Microphone Listening Stage
                val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (isListening) 1.24f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(680, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_pulse_scale"
                )
                val waveFactor by infiniteTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(420, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "wave_bars"
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isListening) {
                        FreshGreenPrimary.copy(alpha = 0.11f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isListening) FreshGreenPrimary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            if (isListening) {
                                Box(
                                    modifier = Modifier
                                        .size(94.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(FreshGreenPrimary.copy(alpha = 0.15f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(78.dp)
                                        .scale(1f + (pulseScale - 1f) * 0.55f)
                                        .clip(CircleShape)
                                        .background(FreshGreenPrimary.copy(alpha = 0.25f))
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isListening) Color(0xFFEF4444) else FreshGreenPrimary,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isListening) {
                                            stopVoiceRecognition()
                                        } else {
                                            requestMicAndListen()
                                        }
                                    }
                                    .testTag("voice_modal_mic_toggle_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                        contentDescription = if (isListening) "Stop Listening" else "Start Voice Input",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        if (isListening) {
                            val rmsBoost = (rmsDbLevel / 12f).coerceIn(0.15f, 1f)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(24.dp)
                            ) {
                                listOf(0.45f, 0.85f, 1.0f, 0.7f, 0.95f, 0.5f).forEachIndexed { idx, ratio ->
                                    val barHeight = (8 + (16 * ratio * (0.4f + 0.6f * waveFactor) * (0.5f + 0.5f * rmsBoost))).dp
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(if (idx % 2 == 0) barHeight else (barHeight * 0.8f))
                                            .clip(RoundedCornerShape(50))
                                            .background(FreshGreenPrimary)
                                    )
                                }
                            }
                        }

                        Text(
                            text = speechStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isListening) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { launchSystemSpeechIntent(context) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MicNone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "System Voice Prompt",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Natural Language Transcript Input Box
                OutlinedTextField(
                    value = spokenTranscript,
                    onValueChange = { spokenTranscript = it },
                    label = { Text("Meal Description") },
                    placeholder = {
                        Text("e.g., 2 rotis, 1 bowl masoor dal, and 1 boiled egg for lunch")
                    },
                    trailingIcon = {
                        if (spokenTranscript.isNotEmpty()) {
                            IconButton(
                                onClick = { spokenTranscript = "" },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear meal description"
                                )
                            }
                        }
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_transcript_input_field")
                )

                // Quick Sample Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick sample phrases",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleVoicePhrases.forEachIndexed { idx, phrase ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        spokenTranscript = phrase
                                        triggerGeminiParse(phrase)
                                    }
                                    .testTag("voice_sample_chip_$idx")
                            ) {
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Instant Database Matches for spoken text (allows 1-tap logging of verified foods)
                val instantDbMatches = remember(spokenTranscript) {
                    if (spokenTranscript.isNotBlank()) {
                        FoodDatabaseCatalog.search(spokenTranscript).take(3)
                    } else {
                        emptyList()
                    }
                }

                if (instantDbMatches.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Verified Database Matches",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "1-Tap Log",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        instantDbMatches.forEach { dbItem ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    FreshGreenPrimary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${dbItem.nameEn} (${dbItem.nameBn})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${dbItem.servingSizeG.roundToInt()}g • ${dbItem.calories} kcal • P:${dbItem.protein.roundToInt()}g C:${dbItem.carbohydrates.roundToInt()}g F:${dbItem.fat.roundToInt()}g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            val entry = FoodDiaryEntry(
                                                id = UUID.randomUUID().toString(),
                                                foodName = dbItem.nameEn,
                                                quantity = dbItem.servingSizeG,
                                                calories = dbItem.calories,
                                                protein = dbItem.protein,
                                                carbohydrates = dbItem.carbohydrates,
                                                fat = dbItem.fat,
                                                fiber = dbItem.fiber,
                                                sugar = dbItem.sugar,
                                                mealType = selectedMealType,
                                                dateKey = selectedDateKey,
                                                source = "Voice Search",
                                                notes = "Logged via Voice Search • ${dbItem.category}"
                                            )
                                            stopVoiceRecognition()
                                            onLogParsedEntries(listOf(entry))
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FreshGreenPrimary,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("quick_log_db_match_${dbItem.id}")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Log", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Analyze Meal Action Button
                Button(
                    onClick = { triggerGeminiParse(spokenTranscript) },
                    enabled = !isAnalyzingWithGemini && spokenTranscript.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("parse_voice_with_gemini_button")
                ) {
                    if (isAnalyzingWithGemini) {
                        CircularProgressIndicator(
                            color = LimeAccent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing Meal...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyze Meal Nutrition",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Error Banner if any
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Structured Nutrition Results
                if (parsedItems.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = parsedMealTitle.ifBlank { "Parsed Meal Breakdown" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FreshGreenPrimary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${parsedItems.sumOf { it.caloriesKcal }} kcal total",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { mealCat ->
                                val selected = selectedMealType == mealCat
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) FreshGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedMealType = mealCat }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = mealCat,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val totalKcal = parsedItems.sumOf { it.caloriesKcal }
                    val totalProtein = parsedItems.sumOf { it.proteinG }
                    val totalCarbs = parsedItems.sumOf { it.carbohydratesG }
                    val totalFat = parsedItems.sumOf { it.fatG }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = FreshGreenPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, FreshGreenPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VoiceMacroStatPill("Calories", "$totalKcal kcal", FreshGreenPrimary)
                            VoiceMacroStatPill("Protein", "${totalProtein.roundToInt()}g", ProteinColor)
                            VoiceMacroStatPill("Carbs", "${totalCarbs.roundToInt()}g", CarbsColor)
                            VoiceMacroStatPill("Fat", "${totalFat.roundToInt()}g", FatColor)
                        }
                    }

                    parsedItems.forEachIndexed { index, item ->
                        VoiceParsedFoodItemRow(
                            item = item,
                            onScalePortion = { factor ->
                                val currentGrams = item.quantityG.coerceAtLeast(10.0)
                                val newGrams = (currentGrams * factor).coerceIn(10.0, 2000.0)
                                val ratio = newGrams / currentGrams
                                parsedItems[index] = item.copy(
                                    quantityG = (newGrams * 10.0).roundToInt() / 10.0,
                                    caloriesKcal = (item.caloriesKcal * ratio).roundToInt().coerceAtLeast(5),
                                    proteinG = ((item.proteinG * ratio) * 10.0).roundToInt() / 10.0,
                                    carbohydratesG = ((item.carbohydratesG * ratio) * 10.0).roundToInt() / 10.0,
                                    fatG = ((item.fatG * ratio) * 10.0).roundToInt() / 10.0,
                                    fiberG = ((item.fiberG * ratio) * 10.0).roundToInt() / 10.0,
                                    sugarG = ((item.sugarG * ratio) * 10.0).roundToInt() / 10.0
                                )
                            },
                            onRemove = {
                                parsedItems.removeAt(index)
                            }
                        )
                    }

                    if (analysisNotes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nutrition Note: $analysisNotes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Confirm & Log to Food Diary Button
                    Button(
                        onClick = {
                            val entries = parsedItems.map { scanned ->
                                FoodDiaryEntry(
                                    id = UUID.randomUUID().toString(),
                                    dateKey = selectedDateKey,
                                    mealType = selectedMealType,
                                    foodName = scanned.foodName,
                                    quantity = scanned.quantityG,
                                    calories = scanned.caloriesKcal,
                                    protein = scanned.proteinG,
                                    carbohydrates = scanned.carbohydratesG,
                                    fat = scanned.fatG,
                                    fiber = scanned.fiberG,
                                    sugar = scanned.sugarG,
                                    source = "Voice Log",
                                    confidence = scanned.confidence,
                                    notes = "Voice Logged • ${scanned.assumptions}"
                                )
                            }
                            stopVoiceRecognition()
                            onLogParsedEntries(entries)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FreshGreenPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_log_voice_meal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log ${parsedItems.size} Item(s) to $selectedMealType (+${parsedItems.size * 25} XP)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceMacroStatPill(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoiceParsedFoodItemRow(
    item: ScannedFoodItem,
    onScalePortion: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = FreshGreenPrimary.copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.RestaurantMenu,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.foodName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.assumptions.isNotBlank()) {
                            Text(
                                text = item.assumptions,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.caloriesKcal} kcal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove Item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P: ${item.proteinG}g • C: ${item.carbohydratesG}g • F: ${item.fatG}g • Fiber: ${item.fiberG}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Portion Adjuster (-20% / +20%) with accessible 48dp touch area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onScalePortion(0.8) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease Portion",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${item.quantityG.roundToInt()}g",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { onScalePortion(1.25) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase Portion",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
