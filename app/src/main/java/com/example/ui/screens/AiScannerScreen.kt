package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.ScannedFoodItem
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import com.example.viewmodel.ScanUiState
import com.example.widgets.CircularMacroRingCard
import com.example.widgets.FloatingFoodTagBubble
import com.example.widgets.HealthScoreBarCard
import com.example.widgets.ScannerViewfinderOverlay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScannerScreen(
    scanUiState: ScanUiState,
    remainingScansToday: Int,
    isPremium: Boolean,
    onAnalyzeBitmap: (Bitmap, String) -> Unit,
    onUpdateScannedItem: (Int, String, Double) -> Unit,
    onConfirmMealToDiary: (String) -> Unit,
    onResetScan: () -> Unit,
    onGrantBonusScan: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customMealHint by remember { mutableStateOf("") }
    var selectedTargetMeal by remember { mutableStateOf("Breakfast") }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var selectedScannerMode by remember { mutableStateOf("Scan Food") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            previewBitmap = bmp
            permissionDeniedMessage = null
            onAnalyzeBitmap(bmp, customMealHint)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMessage = null
            cameraLauncher.launch(null)
        } else {
            permissionDeniedMessage = "Camera permission was denied. You can still upload a food photo from your Gallery or scan the sample plate below."
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                previewBitmap = bmp
                permissionDeniedMessage = null
                onAnalyzeBitmap(bmp, customMealHint)
            } catch (e: Exception) {
                permissionDeniedMessage = "Failed to load selected image: ${e.localizedMessage}"
            }
        }
    }

    fun triggerSampleThaliScan() {
        val sampleBmp = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.img_hero_nutrition_bowl_1793693566593
        )
        if (sampleBmp != null) {
            previewBitmap = sampleBmp
            onAnalyzeBitmap(
                sampleBmp,
                customMealHint.ifBlank { "Indian & Bengali Thali with Rice, Dal, Fish Curry, Roti, Mixed Sabzi, and Salad" }
            )
        }
    }

    fun triggerCameraCapture() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_scanner_screen"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Immersive Food Viewfinder Card with Corner Brackets, Laser Line, Floating Tags & Shutter Bar (Images 3, 4, 5, 7)
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Food Image Background
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "Scanned Meal Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                            contentDescription = "Food Scanner Viewfinder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Dark translucent vignette at top & bottom for legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.24f))
                    )

                    // 4 White Rounded Corner Brackets + Glowing Green Laser Line
                    ScannerViewfinderOverlay(
                        showLaserLine = scanUiState !is ScanUiState.Success,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 54.dp, horizontal = 10.dp)
                    )

                    // Top Overlay Bar (Back Button, "Food Scanner" Title, Close/Reset Button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onResetScan() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Reset Scan",
                                    tint = DarkCharcoalText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (scanUiState is ScanUiState.Success) "Food Details" else "Food Scanner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPremium) "PRO • $remainingScansToday scans left" else "FREE • $remainingScansToday/5 scans",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onResetScan() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Scan",
                                    tint = DarkCharcoalText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Floating Detected Food Tag Bubbles Over Plate (Matching Uploaded Image 5 Center)
                    val floatingTags = when (scanUiState) {
                        is ScanUiState.Success -> scanUiState.result.foodItems.take(4).map {
                            "${it.foodName} • ${it.quantityG.roundToInt()}g"
                        }
                        else -> listOf("Basmati Rice", "Fish Curry", "Salad", "Dal")
                    }

                    if (floatingTags.isNotEmpty()) {
                        FloatingFoodTagBubble(
                            text = floatingTags[0],
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 34.dp, y = 96.dp)
                        )
                    }
                    if (floatingTags.size > 1) {
                        FloatingFoodTagBubble(
                            text = floatingTags[1],
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-34).dp, y = 118.dp)
                        )
                    }
                    if (floatingTags.size > 2) {
                        FloatingFoodTagBubble(
                            text = floatingTags[2],
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 30.dp, y = 24.dp)
                        )
                    }
                    if (floatingTags.size > 3) {
                        FloatingFoodTagBubble(
                            text = floatingTags[3],
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-30).dp, y = 36.dp)
                        )
                    }

                    // Bottom Scanner Mode Cards + Shutter Bar (Matching Uploaded Image 4 Right)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Alignment hint pill (Image 7 Left)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "Please align food with the scanner",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        // 4 Mode Selector Pills: Scan Food, Sample Plate, Food Label, Library (Image 4 Right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScannerModeTile(
                                title = "Scan Food",
                                icon = Icons.Default.Restaurant,
                                selected = selectedScannerMode == "Scan Food",
                                onClick = {
                                    selectedScannerMode = "Scan Food"
                                    triggerCameraCapture()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ScannerModeTile(
                                title = "Sample Plate",
                                icon = Icons.Default.AutoAwesome,
                                selected = selectedScannerMode == "Sample Plate",
                                onClick = {
                                    selectedScannerMode = "Sample Plate"
                                    triggerSampleThaliScan()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_sample_thali_button")
                            )
                            ScannerModeTile(
                                title = "Food Label",
                                icon = Icons.Default.QrCodeScanner,
                                selected = selectedScannerMode == "Food Label",
                                onClick = {
                                    selectedScannerMode = "Food Label"
                                    triggerCameraCapture()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ScannerModeTile(
                                title = "Library",
                                icon = Icons.Default.PhotoLibrary,
                                selected = selectedScannerMode == "Library",
                                onClick = {
                                    selectedScannerMode = "Library"
                                    galleryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_gallery_button")
                            )
                        }

                        // Camera Shutter Row (Gallery Thumbnail Left, Big White Shutter Center, Sample/Flip Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Gallery Thumbnail Circle
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                                contentDescription = "Pick from Gallery",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        galleryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            )

                            // Center: Large White Shutter Button with Inner Ring (Image 4 Right)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color.White, CircleShape)
                                    .padding(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { triggerCameraCapture() }
                                    .testTag("capture_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "Capture Meal Photo",
                                    tint = DarkCharcoalText,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Right: Dark Circle Quick Sample Scan / Refresh Button
                            Surface(
                                shape = CircleShape,
                                color = DarkCharcoalText,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable { triggerSampleThaliScan() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Analyze Sample Meal",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Optional Cooking Context / Dish Hint Input
        item {
            OutlinedTextField(
                value = customMealHint,
                onValueChange = { customMealHint = it },
                label = { Text("Optional context (e.g. 'Home cooked with 1 tsp mustard oil')") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_hint_input")
            )
        }

        if (permissionDeniedMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = permissionDeniedMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 3. Scan State Handling: Idle / Analyzing / Error / Food Details Result Sheet
        when (scanUiState) {
            is ScanUiState.Idle -> {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "SMART SCANNER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = PastelGreenCard
                                ) {
                                    Text(
                                        text = "READY TO SCAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FreshGreenPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Tap Shutter or Sample Plate",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Snap a photo of Rice, Roti, Dal, Fish Curry (Macher Jhol), Biryani, Momo, or any multi-item plate to get instant portion weights and macro rings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            is ScanUiState.Analyzing -> {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = FreshGreenPrimary)
                            Text(
                                text = "Analyzing your meal composition...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Identifying dishes, estimating gram portions, and computing macro rings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan Issue",
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
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (previewBitmap != null) {
                                    Button(onClick = { onAnalyzeBitmap(previewBitmap!!, customMealHint) }) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Scan")
                                    }
                                }
                                if (remainingScansToday <= 0) {
                                    OutlinedButton(onClick = onGrantBonusScan) {
                                        Text("Unlock +1 Bonus Scan")
                                    }
                                }
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
                val macroTotal = (pGrams + cGrams + fGrams).coerceAtLeast(1.0)

                // Compute a realistic 1..10 health score based on protein/fiber balance
                val computedHealthScore = (7 + (if (pGrams >= 12) 1 else 0) + (if (res.totalNutrition.fiberG >= 3) 1 else 0))
                    .coerceIn(6, 10)

                // Food Details Sheet Card (Matching Uploaded Images 3 Right, 4 Left, 7 Right)
                item {
                    Card(
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Drag handle pill
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            // Category & Weight Badges ("AI MEAL" | "350 G" - Image 4 Left)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = "MEAL SCAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = "$totalWeight G",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Dish Title ("Mandarin Orange" / "Rice and Fish Curry" - Image 4 Left)
                            Text(
                                text = res.mealName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )

                            // Soft Green Total Calorie Pill Bar with Flame Icon (Image 3 Right & Image 4 Left)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PastelGreenCard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total $totalKcal kcal",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkCharcoalText
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = Color(0xFFF97316),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3 Circular Macro Ring Cards (Protein, Fat, Carbs - Image 4 Left & Image 3 Right)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularMacroRingCard(
                                    label = "Protein",
                                    gramsText = "${pGrams.roundToInt()}g",
                                    subLabel = "${((pGrams / macroTotal) * 100).roundToInt()}%",
                                    progress = (pGrams / macroTotal).toFloat().coerceIn(0.15f, 1f),
                                    ringColor = Color(0xFF84CC16),
                                    cardColor = PastelGreenCard.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1f)
                                )
                                CircularMacroRingCard(
                                    label = "Fat",
                                    gramsText = "${fGrams.roundToInt()}g",
                                    subLabel = "${((fGrams / macroTotal) * 100).roundToInt()}%",
                                    progress = (fGrams / macroTotal).toFloat().coerceIn(0.15f, 1f),
                                    ringColor = Color(0xFF0EA5E9),
                                    cardColor = PastelBlueCard.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1f)
                                )
                                CircularMacroRingCard(
                                    label = "Carbs",
                                    gramsText = "${cGrams.roundToInt()}g",
                                    subLabel = "${((cGrams / macroTotal) * 100).roundToInt()}%",
                                    progress = (cGrams / macroTotal).toFloat().coerceIn(0.15f, 1f),
                                    ringColor = Color(0xFFF97316),
                                    cardColor = PastelPeachCard.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // High Health Score Card (Image 3 Right)
                            HealthScoreBarCard(scoreOutOfTen = computedHealthScore)

                            // Ingredients Identified Pill Tags (Image 7 Right)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Ingredients Identified (${res.foodItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    res.foodItems.forEachIndexed { idx, item ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier.clickable { editingItemIndex = idx }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${item.foodName} (${item.quantityG.roundToInt()}g)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit item",
                                                    modifier = Modifier.size(13.dp),
                                                    tint = FreshGreenPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Meal Selection Row ("Add Meal to Food Diary" - Image 7 Right)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Add Meal to Food Diary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { meal ->
                                        FilterChip(
                                            selected = selectedTargetMeal == meal,
                                            onClick = { selectedTargetMeal = meal },
                                            label = { Text(meal) },
                                            shape = RoundedCornerShape(50),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LimeAccent,
                                                selectedLabelColor = DarkCharcoalText
                                            )
                                        )
                                    }
                                }
                            }

                            // Dual Bottom Pill Buttons: "Update Details" & "Add Meal" (Images 3 Right & 4 Left)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (res.foodItems.isNotEmpty()) {
                                            editingItemIndex = 0
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                ) {
                                    Text("Update Details", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onConfirmMealToDiary(selectedTargetMeal) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkCharcoalText,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(54.dp)
                                        .testTag("confirm_scanned_meal_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Meal", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Itemized Nutrition Breakdown List (Matching Image 5 Center & Image 10 Right)
                itemsIndexed(res.foodItems) { index, item ->
                    ScannedFoodItemCard(
                        item = item,
                        onEditClick = { editingItemIndex = index }
                    )
                }
            }
        }
    }

    // Inline Edit Dialog for Scanned Food Item
    val currentSuccess = scanUiState as? ScanUiState.Success
    val editIdx = editingItemIndex
    if (currentSuccess != null && editIdx != null && editIdx in currentSuccess.result.foodItems.indices) {
        val targetItem = currentSuccess.result.foodItems[editIdx]
        var editedName by remember(targetItem) { mutableStateOf(targetItem.foodName) }
        var editedGrams by remember(targetItem) { mutableStateOf(targetItem.quantityG.roundToInt().toString()) }

        AlertDialog(
            onDismissRequest = { editingItemIndex = null },
            title = { Text("Adjust Food & Portion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Food Name") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedGrams,
                        onValueChange = { editedGrams = it },
                        label = { Text("Portion Quantity (grams)") },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Nutrition values will automatically scale proportionally to the updated gram weight.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Recalculate")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItemIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScannerModeTile(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color.White else Color.White.copy(alpha = 0.22f),
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) DarkCharcoalText else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) DarkCharcoalText else Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScannedFoodItemCard(
    item: ScannedFoodItem,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                contentDescription = item.foodName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${item.quantityG.roundToInt()}g • Confidence ${(item.confidence * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "P: ${item.proteinG}g  •  C: ${item.carbohydratesG}g  •  F: ${item.fatG}g  •  Fiber: ${item.fiberG}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = FreshGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.caloriesKcal} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit food name or portion",
                        tint = FreshGreenPrimary
                    )
                }
            }
        }
    }
}
