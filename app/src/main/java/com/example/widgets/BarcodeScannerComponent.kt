package com.example.widgets

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.PackagedBarcodeService
import com.example.data.PackagedFoodProduct
import com.example.model.FoodDiaryEntry
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.ProteinColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Redesigned Mobile Camera Barcode & Packaged Food Scanner Component.
 * Combines a sleek CameraX viewfinder, camera snapshot / gallery label scanning,
 * Open Food Facts API v2 lookup, and verified packaged food quick-scan cards.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackagedBarcodeScannerCard(
    onLogPackagedFood: (FoodDiaryEntry) -> Unit,
    defaultMealType: String = "Snacks",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showLiveCameraPreview by remember { mutableStateOf(false) }
    var barcodeQuery by remember { mutableStateOf("") }
    var selectedProduct by remember {
        mutableStateOf<PackagedFoodProduct?>(PackagedBarcodeService.catalog.firstOrNull())
    }
    var portionGramsInput by remember(selectedProduct) {
        mutableStateOf((selectedProduct?.servingSizeGrams ?: 100.0).roundToInt().toString())
    }
    var selectedMealSlot by remember { mutableStateOf(defaultMealType) }
    var isScanningBarcode by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    fun selectPackagedProduct(product: PackagedFoodProduct, message: String? = null) {
        selectedProduct = product
        portionGramsInput = product.servingSizeGrams.roundToInt().toString()
        isErrorStatus = false
        statusMessage = message ?: "Loaded ${product.brand} ${product.productName} (EAN: ${product.barcode})"
    }

    fun analyzeCameraBitmapForBarcode(bmp: Bitmap) {
        isScanningBarcode = true
        statusMessage = "Reading barcode & nutrition label…"
        isErrorStatus = false
        coroutineScope.launch {
            PackagedBarcodeService.analyzePackagedCameraFrame(bmp)
                .onSuccess { prod ->
                    isScanningBarcode = false
                    selectPackagedProduct(
                        prod,
                        "Scanned: ${prod.brand} ${prod.productName} (${prod.barcode})"
                    )
                }
                .onFailure { err ->
                    isScanningBarcode = false
                    isErrorStatus = true
                    statusMessage = "Could not read barcode: ${err.localizedMessage ?: "Try entering digits below."}"
                }
        }
    }

    val takeBarcodePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            analyzeCameraBitmapForBarcode(bmp)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showLiveCameraPreview = true
            isErrorStatus = false
            statusMessage = "Camera active! Align barcode inside the brackets or tap Capture."
        } else {
            isErrorStatus = true
            statusMessage = "Camera permission declined. You can pick a barcode image from Gallery or enter digits below."
        }
    }

    val galleryBarcodeLauncher = rememberLauncherForActivityResult(
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
                analyzeCameraBitmapForBarcode(bmp)
            } catch (e: Exception) {
                isErrorStatus = true
                statusMessage = "Could not decode image: ${e.localizedMessage}"
            }
        }
    }

    fun handleBarcodeLookup() {
        val q = barcodeQuery.trim()
        if (q.isEmpty()) {
            isErrorStatus = true
            statusMessage = "Enter a barcode number (e.g. 8901262010016) or product name first."
            return
        }
        val localMatch = PackagedBarcodeService.findInLocalCatalog(q)
        if (localMatch != null) {
            selectPackagedProduct(
                localMatch,
                "Found ${localMatch.brand} ${localMatch.productName} (${localMatch.barcode})"
            )
            return
        }

        isScanningBarcode = true
        isErrorStatus = false
        statusMessage = "Searching barcode $q in Open Food Facts…"
        coroutineScope.launch {
            PackagedBarcodeService.lookupByBarcode(q)
                .onSuccess { prod ->
                    isScanningBarcode = false
                    selectPackagedProduct(
                        prod,
                        "Loaded ${prod.brand} ${prod.productName} from ${prod.sourceLabel}"
                    )
                }
                .onFailure { err ->
                    isScanningBarcode = false
                    isErrorStatus = true
                    statusMessage = err.localizedMessage ?: "Barcode not found."
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("barcode_scanner_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Camera Barcode Viewfinder Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A1D13))
                .border(
                    1.5.dp,
                    FreshGreenPrimary.copy(alpha = 0.65f),
                    RoundedCornerShape(24.dp)
                )
        ) {
            if (hasCameraPermission && showLiveCameraPreview) {
                LiveCameraPreviewSurface(modifier = Modifier.fillMaxSize())
            }

            BarcodeReticleCanvasOverlay(modifier = Modifier.fillMaxSize())

            // Top Format Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = if (hasCameraPermission && showLiveCameraPreview) {
                        "CAMERA ACTIVE • Align Barcode in Brackets"
                    } else {
                        "EAN-13 • UPC-A • Nutrition Label Scanner"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Bottom Floating Action Buttons inside Viewfinder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            showLiveCameraPreview = true
                            takeBarcodePhotoLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = DarkCharcoalText
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("camera_scan_barcode_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasCameraPermission) "Capture" else "Open Camera",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        galleryBarcodeLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.94f),
                        contentColor = DarkCharcoalText
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("gallery_scan_barcode_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // 2. Barcode Lookup Input & Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = barcodeQuery,
                onValueChange = { barcodeQuery = it },
                placeholder = { Text("Enter barcode or brand…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = FreshGreenPrimary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .testTag("barcode_number_input")
            )

            Button(
                onClick = { handleBarcodeLookup() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("barcode_lookup_button")
            ) {
                Text("Lookup", fontWeight = FontWeight.ExtraBold)
            }
        }

        // 3. Quick-Scan Popular Packaged Foods Carousel
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Popular Packaged Foods",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PackagedBarcodeService.catalog.forEach { item ->
                    val isSelected = selectedProduct?.barcode == item.barcode
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) FreshGreenPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                barcodeQuery = item.barcode
                                selectPackagedProduct(item)
                            }
                            .testTag("sample_barcode_chip_${item.barcode}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${item.brand} • ${item.productName.take(22)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "${item.barcode} • ${item.caloriesForGrams(item.servingSizeGrams)} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Status Banner
        if (isScanningBarcode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = FreshGreenPrimary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusMessage ?: "Scanning barcode…",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (statusMessage != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isErrorStatus) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isErrorStatus) MaterialTheme.colorScheme.error else FreshGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isErrorStatus) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }

        // 5. Scanned Product Nutrition & Logging Card
        selectedProduct?.let { product ->
            val currentGrams = (portionGramsInput.toDoubleOrNull() ?: product.servingSizeGrams)
                .coerceIn(5.0, 2500.0)
            val calcKcal = product.caloriesForGrams(currentGrams)
            val calcProtein = product.proteinForGrams(currentGrams)
            val calcCarbs = product.carbsForGrams(currentGrams)
            val calcFat = product.fatForGrams(currentGrams)
            val calcFiber = product.fiberForGrams(currentGrams)
            val calcSugar = product.sugarForGrams(currentGrams)

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, FreshGreenPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanned_barcode_product_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${product.brand.uppercase()} • ${product.category}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshGreenPrimary
                            )
                            Text(
                                text = product.productName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "EAN: ${product.barcode} • ${product.sourceLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        NutriScoreBadge(grade = product.nutriScore)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PackagedMacroTile("Calories", "$calcKcal", "kcal", FreshGreenPrimary, Modifier.weight(1.1f))
                        PackagedMacroTile("Protein", "$calcProtein", "g", ProteinColor, Modifier.weight(1f))
                        PackagedMacroTile("Carbs", "$calcCarbs", "g", CarbsColor, Modifier.weight(1f))
                        PackagedMacroTile("Fat", "$calcFat", "g", FatColor, Modifier.weight(1f))
                    }

                    Text(
                        text = "Fiber ${calcFiber}g • Sugar ${calcSugar}g • Serving: ${product.servingDescription}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    val baseServingGrams = product.servingSizeGrams.coerceAtLeast(1.0)
                    val currentMultiplier = (currentGrams / baseServingGrams).toFloat().coerceIn(0.25f, 3.0f)
                    val formattedMult = ((currentMultiplier * 100f).roundToInt() / 100f).toString().trimEnd('0').trimEnd('.')

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Portion Estimator (${currentGrams.roundToInt()}g)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formattedMult}x • $calcKcal kcal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshGreenPrimary
                        )
                    }

                    Slider(
                        value = currentMultiplier,
                        onValueChange = { mult ->
                            val snapped = ((mult.coerceIn(0.25f, 3.0f) * 20f).roundToInt() / 20f)
                            val newGrams = (baseServingGrams * snapped).roundToInt().coerceIn(5, 2500)
                            portionGramsInput = newGrams.toString()
                        },
                        valueRange = 0.25f..3.0f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = FreshGreenPrimary,
                            activeTrackColor = FreshGreenPrimary,
                            inactiveTrackColor = FreshGreenPrimary.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barcode_portion_slider")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val stdG = product.servingSizeGrams
                        listOf(
                            "0.5x (${(stdG * 0.5).roundToInt()}g)" to (stdG * 0.5).roundToInt(),
                            "1x (${stdG.roundToInt()}g)" to stdG.roundToInt(),
                            "2x (${(stdG * 2.0).roundToInt()}g)" to (stdG * 2.0).roundToInt(),
                            "100g" to 100
                        ).forEach { (label, gVal) ->
                            val selected = currentGrams.roundToInt() == gVal
                            FilterChip(
                                selected = selected,
                                onClick = { portionGramsInput = gVal.toString() },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FreshGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = portionGramsInput,
                        onValueChange = { portionGramsInput = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Custom Grams / ml") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barcode_portion_grams_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { meal ->
                            val isSelected = selectedMealSlot == meal
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMealSlot = meal },
                                label = {
                                    Text(
                                        text = meal,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FreshGreenPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val entry = product.toFoodDiaryEntry(
                                portionGrams = currentGrams,
                                mealType = selectedMealSlot
                            )
                            onLogPackagedFood(entry)
                            isErrorStatus = false
                            statusMessage = "Added ${product.brand} ${product.productName} ($calcKcal kcal) to $selectedMealSlot!"
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FreshGreenPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_barcode_food_to_diary_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to $selectedMealSlot ($calcKcal kcal)",
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
private fun LiveCameraPreviewSurface(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            } catch (_: Exception) {
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun BarcodeReticleCanvasOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "barcode_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barcode_laser_y"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val reticleW = w * 0.74f
        val reticleH = h * 0.46f
        val left = (w - reticleW) / 2f
        val top = (h - reticleH) / 2f - 8.dp.toPx()

        val barWidths = listOf(3f, 1.5f, 4.5f, 2f, 1.5f, 5f, 2f, 3f, 1.5f, 4f, 2f, 3.5f, 1.5f, 4f, 2.5f)
        var cursorX = left + 22.dp.toPx()
        val maxBarX = left + reticleW - 22.dp.toPx()
        var barIdx = 0
        while (cursorX < maxBarX) {
            val bw = barWidths[barIdx % barWidths.size].dp.toPx() * 0.65f
            if (cursorX + bw < maxBarX) {
                drawRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(cursorX, top + 14.dp.toPx()),
                    size = Size(bw, reticleH - 28.dp.toPx())
                )
            }
            cursorX += bw + 4.dp.toPx()
            barIdx++
        }

        drawRoundRect(
            color = Color.White.copy(alpha = 0.82f),
            topLeft = Offset(left, top),
            size = Size(reticleW, reticleH),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        val laserY = top + (reticleH * laserProgress)
        drawLine(
            color = Color(0xFF4ADE80),
            start = Offset(left + 12.dp.toPx(), laserY),
            end = Offset(left + reticleW - 12.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun NutriScoreBadge(grade: String) {
    val cleanGrade = grade.uppercase().take(1).ifBlank { "B" }
    val bgColor = when (cleanGrade) {
        "A" -> Color(0xFF15803D)
        "B" -> Color(0xFF65A30D)
        "C" -> Color(0xFFEAB308)
        "D" -> Color(0xFFF97316)
        else -> Color(0xFFDC2626)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NUTRI-SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = cleanGrade,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PackagedMacroTile(
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
fun BarcodeScannerModalDialog(
    defaultMealType: String = "Snacks",
    onLogPackagedFood: (FoodDiaryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Barcode Scanner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Scan packaged foods or lookup EAN-13",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Barcode Scanner"
                        )
                    }
                }

                PackagedBarcodeScannerCard(
                    defaultMealType = defaultMealType,
                    onLogPackagedFood = onLogPackagedFood
                )
            }
        }
    }
}
