package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.model.ScannedFoodItem
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.ProteinColor
import com.example.viewmodel.ScanUiState
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScannerScreen(
    scanUiState: ScanUiState,
    onAnalyzeBitmap: (Bitmap, String) -> Unit,
    onAnalyzeText: (String) -> Unit,
    onUpdateScannedItem: (Int, String, Double) -> Unit,
    onUpdatePortionMultiplier: (Float) -> Unit = {},
    onConfirmMealToDiary: (String) -> Unit,
    onResetScan: () -> Unit,
    onSaveScannedComboToRecipes: (title: String, mealType: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var mealDescription by rememberSaveable { mutableStateOf("") }
    var selectedTargetMeal by rememberSaveable { mutableStateOf("Lunch") }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    var showSaveComboDialog by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFilePath?.let { File(it) }
        if (success && file != null && file.exists() && file.length() > 0L) {
            localErrorMessage = null
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    decodeSampledFileBitmap(file, maxDim = 1024)
                }
                if (decoded != null) {
                    previewBitmap = decoded
                    onAnalyzeBitmap(decoded, mealDescription.trim())
                } else {
                    localErrorMessage = "Could not read captured photo. Please try again."
                }
            }
        } else if (success) {
            localErrorMessage = "Captured photo could not be saved. Please try again or pick from Gallery."
        }
    }

    fun launchFullResCamera() {
        try {
            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val photoFile = File(cameraDir, "meal_${System.currentTimeMillis()}.jpg")
            pendingCameraFilePath = photoFile.absolutePath
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            localErrorMessage = "Camera unavailable: ${e.localizedMessage ?: "try Gallery instead"}"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            localErrorMessage = null
            launchFullResCamera()
        } else {
            localErrorMessage = "Camera permission denied. You can choose a photo from Gallery or type your meal below."
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            localErrorMessage = null
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    decodeSampledUriBitmap(context, uri, maxDim = 1024)
                }
                if (decoded != null) {
                    previewBitmap = decoded
                    onAnalyzeBitmap(decoded, mealDescription.trim())
                } else {
                    localErrorMessage = "Could not load selected image. Please try another photo."
                }
            }
        }
    }

    fun triggerCameraCapture() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            launchFullResCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun resetEverything() {
        previewBitmap = null
        mealDescription = ""
        localErrorMessage = null
        onResetScan()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_scanner_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (scanUiState is ScanUiState.Success) "Scan Result" else "Scan Food",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Take a photo, pick from gallery, or type your meal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (previewBitmap != null || scanUiState !is ScanUiState.Idle) {
                    IconButton(
                        onClick = { resetEverything() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scanner_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear Scan",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. Clean Photo & Text Capture Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Photo Preview Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { triggerCameraCapture() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Selected meal photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = FreshGreenPrimary.copy(alpha = 0.14f),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = FreshGreenPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Tap to photograph your meal",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Good lighting improves calorie accuracy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Primary Action Buttons: Camera & Gallery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { triggerCameraCapture() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshGreenPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("capture_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Camera", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("upload_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gallery", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Text Meal Description Input + Instant Estimate Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = mealDescription,
                            onValueChange = { mealDescription = it },
                            placeholder = { Text("Or type meal (e.g. 2 rotis, dal)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (mealDescription.isNotBlank()) {
                                        onAnalyzeText(mealDescription)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_hint_input")
                        )

                        FilledTonalButton(
                            onClick = {
                                val bmp = previewBitmap
                                if (bmp != null) {
                                    onAnalyzeBitmap(bmp, mealDescription.trim())
                                } else {
                                    onAnalyzeText(mealDescription)
                                }
                            },
                            enabled = mealDescription.isNotBlank() && scanUiState !is ScanUiState.Analyzing,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("scanner_text_estimate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Estimate meal calories",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Local Camera/Gallery Error Banner
        if (localErrorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = localErrorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 3. Scan State (Analyzing / Error / Success)
        when (scanUiState) {
            is ScanUiState.Idle -> Unit

            is ScanUiState.Analyzing -> {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FreshGreenPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(30.dp)
                            )
                            Column {
                                Text(
                                    text = "Calculating calories...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Identifying food items and portion sizes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            is ScanUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = scanUiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = {
                                    val bmp = previewBitmap
                                    if (bmp != null) {
                                        onAnalyzeBitmap(bmp, mealDescription.trim())
                                    } else if (mealDescription.isNotBlank()) {
                                        onAnalyzeText(mealDescription)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("scanner_retry_button")
                            ) {
                                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            is ScanUiState.Success -> {
                val res = scanUiState.result
                val totalWeight = res.totalNutrition.totalWeightG.roundToInt()
                val totalKcal = res.totalNutrition.caloriesKcal
                val pGrams = res.totalNutrition.proteinG
                val cGrams = res.totalNutrition.carbohydratesG
                val fGrams = res.totalNutrition.fatG

                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = res.mealName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$totalWeight g estimated portion",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = FreshGreenPrimary.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$totalKcal kcal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // 3-Macro Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SimpleMacroStatBox(
                                    label = "Protein",
                                    grams = pGrams,
                                    accent = ProteinColor,
                                    modifier = Modifier.weight(1f)
                                )
                                SimpleMacroStatBox(
                                    label = "Carbs",
                                    grams = cGrams,
                                    accent = CarbsColor,
                                    modifier = Modifier.weight(1f)
                                )
                                SimpleMacroStatBox(
                                    label = "Fat",
                                    grams = fGrams,
                                    accent = FatColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Portion Size Slider (0.5x – 2.5x)
                            PortionEstimatorSection(
                                portionMultiplier = scanUiState.portionMultiplier,
                                baseCaloriesKcal = scanUiState.baseResult.totalNutrition.caloriesKcal,
                                adjustedCaloriesKcal = totalKcal,
                                adjustedWeightG = totalWeight,
                                onMultiplierChange = onUpdatePortionMultiplier
                            )

                            // Meal Slot Selector
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { meal ->
                                    FilterChip(
                                        selected = selectedTargetMeal == meal,
                                        onClick = { selectedTargetMeal = meal },
                                        label = { Text(meal) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FreshGreenPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            // Primary Save Actions
                            Button(
                                onClick = { onConfirmMealToDiary(selectedTargetMeal) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FreshGreenPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("confirm_scanned_meal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to $selectedTargetMeal", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showSaveComboDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_scanned_combo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Save to My Recipes",
                                    fontWeight = FontWeight.Bold,
                                    color = FreshGreenPrimary
                                )
                            }
                        }
                    }
                }

                // Detected Items List
                itemsIndexed(res.foodItems) { index, item ->
                    ScannedFoodItemCard(
                        item = item,
                        onEditClick = { editingItemIndex = index }
                    )
                }
            }
        }
    }

    // Inline Edit Dialog
    val currentSuccess = scanUiState as? ScanUiState.Success
    val editIdx = editingItemIndex
    if (currentSuccess != null && editIdx != null && editIdx in currentSuccess.result.foodItems.indices) {
        val targetItem = currentSuccess.result.foodItems[editIdx]
        var editedName by remember(targetItem) { mutableStateOf(targetItem.foodName) }
        var editedGrams by remember(targetItem) { mutableStateOf(targetItem.quantityG.roundToInt().toString()) }

        AlertDialog(
            onDismissRequest = { editingItemIndex = null },
            title = { Text("Adjust Portion", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Food Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedGrams,
                        onValueChange = { editedGrams = it },
                        label = { Text("Weight (grams)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = editedGrams.toDoubleOrNull() ?: targetItem.quantityG
                        onUpdateScannedItem(editIdx, editedName, g)
                        editingItemIndex = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItemIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Scanned Combo to My Recipes Dialog
    if (showSaveComboDialog && currentSuccess != null) {
        var comboTitle by remember(currentSuccess.result.mealName) {
            mutableStateOf(currentSuccess.result.mealName)
        }
        AlertDialog(
            onDismissRequest = { showSaveComboDialog = false },
            title = { Text("Save to My Recipes", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = comboTitle,
                    onValueChange = { comboTitle = it },
                    label = { Text("Recipe Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_scanned_combo_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveScannedComboToRecipes(
                            comboTitle.ifBlank { currentSuccess.result.mealName },
                            selectedTargetMeal
                        )
                        showSaveComboDialog = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                    modifier = Modifier.testTag("confirm_save_scanned_combo_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveComboDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SimpleMacroStatBox(
    label: String,
    grams: Double,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${grams.roundToInt()}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PortionEstimatorSection(
    portionMultiplier: Float,
    baseCaloriesKcal: Int,
    adjustedCaloriesKcal: Int,
    adjustedWeightG: Int,
    onMultiplierChange: (Float) -> Unit
) {
    val presets = listOf(0.5f to "0.5x", 1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x")

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portion_estimator_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Portion Size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${"%.2f".format(portionMultiplier).trimEnd('0').trimEnd('.')}x • $adjustedCaloriesKcal kcal ($adjustedWeightG g)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FreshGreenPrimary
                )
            }

            Slider(
                value = portionMultiplier.coerceIn(0.5f, 2.5f),
                onValueChange = onMultiplierChange,
                valueRange = 0.5f..2.5f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = FreshGreenPrimary,
                    activeTrackColor = FreshGreenPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portion_estimator_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (mult, label) ->
                    val isSelected = abs(portionMultiplier - mult) < 0.03f
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMultiplierChange(mult) },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FreshGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("portion_preset_${label.replace(".", "_")}")
                    )
                }
            }

            if (abs(portionMultiplier - 1.0f) >= 0.03f) {
                Text(
                    text = "Base 1x estimate: $baseCaloriesKcal kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScannedFoodItemCard(
    item: ScannedFoodItem,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.foodName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.quantityG.roundToInt()}g • P ${item.proteinG.roundToInt()}g • C ${item.carbohydratesG.roundToInt()}g • F ${item.fatG.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.caloriesKcal} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FreshGreenPrimary
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit ${item.foodName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun decodeSampledFileBitmap(file: File, maxDim: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
        val exifOrientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        rotateBitmapIfNeeded(decoded, exifOrientation)
    } catch (_: Exception) {
        null
    }
}

private fun decodeSampledUriBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width.coerceAtLeast(1)
                val h = info.size.height.coerceAtLeast(1)
                if (w > maxDim || h > maxDim) {
                    val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= maxDim || h / 2 >= maxDim) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated != bitmap) {
        bitmap.recycle()
    }
    return rotated
}
