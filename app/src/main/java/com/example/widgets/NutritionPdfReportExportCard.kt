package com.example.widgets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.NutritionReportExportConfig
import com.example.core.export.NutritionReportPdfExporter
import com.example.core.export.ReportPeriodFilter
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Clinical & Health Professional PDF Report Exporter Card.
 * Allows users to preview their nutritional summary & meal history across 7/14/30 days or All History,
 * customize report sections & doctor notes, and export/share a formatted multi-page PDF document.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionPdfReportExportCard(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord> = emptyList(),
    weightRecords: List<WeightRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var config by remember { mutableStateOf(NutritionReportExportConfig()) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }

    val summary = remember(profile, diaryEntries, waterRecords, weightRecords, config) {
        NutritionReportPdfExporter.buildReportSummary(
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            config = config
        )
    }

    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val fileToCopy = pendingSaveFile
        if (targetUri != null && fileToCopy != null) {
            NutritionReportPdfExporter.writePdfFileToUri(context, fileToCopy, targetUri)
                .onSuccess {
                    statusMessage = "Saved clinical PDF report (${fileToCopy.name}) to your device!"
                }
                .onFailure { err ->
                    statusMessage = "Could not save PDF: ${err.localizedMessage ?: "Unknown error"}"
                }
        }
        pendingSaveFile = null
    }

    fun handleSharePdfWithDoctor() {
        NutritionReportPdfExporter.generatePdfFile(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            config = config
        ).onSuccess { pdfFile ->
            statusMessage = "Generated ${pdfFile.name} (${summary.totalMealsLogged} meals). Opening Share sheet…"
            NutritionReportPdfExporter.sharePdfReportWithHealthProfessional(
                context = context,
                pdfFile = pdfFile,
                profile = profile,
                summary = summary
            ).onFailure { err ->
                statusMessage = "PDF generated (${pdfFile.name}), but sharing failed: ${err.localizedMessage}"
            }
        }.onFailure { err ->
            statusMessage = "Failed to generate PDF report: ${err.localizedMessage ?: "Error"}"
        }
    }

    fun handleSavePdfToDevice() {
        NutritionReportPdfExporter.generatePdfFile(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            config = config
        ).onSuccess { pdfFile ->
            pendingSaveFile = pdfFile
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val safeName = profile.name.replace(Regex("[^A-Za-z0-9]"), "_").take(16).ifBlank { "Report" }
            createPdfDocumentLauncher.launch("NutriScan_Clinical_Report_${safeName}_$dateStamp.pdf")
        }.onFailure { err ->
            statusMessage = "Failed to generate PDF report: ${err.localizedMessage ?: "Error"}"
        }
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("nutrition_pdf_export_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header Row
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
                        shape = RoundedCornerShape(14.dp),
                        color = PastelGreenCard,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Export Clinical PDF Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Share nutritional summaries & meal history with doctors or dietitians",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(38.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = { showCustomizeDialog = true },
                        modifier = Modifier.testTag("customize_pdf_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize PDF Report Sections",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Report Time Period Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriodFilter.entries.forEach { period ->
                    val selected = config.period == period
                    FilterChip(
                        selected = selected,
                        onClick = {
                            config = config.copy(period = period)
                            statusMessage = null
                        },
                        label = {
                            Text(
                                text = period.label,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FreshGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("pdf_period_chip_${period.name.lowercase(Locale.US)}")
                    )
                }
            }

            // Live Clinical Report Summary Preview Box
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PastelGreenCard.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        FreshGreenPrimary.copy(alpha = 0.28f),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clinical Summary Preview (${summary.periodLabel})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White
                        ) {
                            Text(
                                text = "${summary.totalMealsLogged} meals • ${summary.activeDaysCount}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FreshGreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 4 Key Clinical Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClinicalMetricMiniPill(
                            label = "Avg Calories",
                            value = "${summary.avgCaloriesKcal} kcal",
                            sub = "${summary.calorieAdherencePct}% of goal",
                            modifier = Modifier.weight(1f)
                        )
                        ClinicalMetricMiniPill(
                            label = "Avg Protein",
                            value = "${summary.avgProteinG.roundToInt()} g",
                            sub = "${summary.proteinAdherencePct}% of goal",
                            modifier = Modifier.weight(1f)
                        )
                        ClinicalMetricMiniPill(
                            label = "Avg Carbs/Fat",
                            value = "${summary.avgCarbsG.roundToInt()}g / ${summary.avgFatG.roundToInt()}g",
                            sub = "Fiber ${summary.avgFiberG.roundToInt()}g",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Includes: Patient BMI (${summary.bmiValue} • ${summary.bmiCategory}), Daily Macro Adherence Bars, ${summary.dailyRows.size} Daily Summaries & ${summary.filteredEntries.size} Meal Logs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkCharcoalText.copy(alpha = 0.8f)
                    )
                }
            }

            // Status Banner when PDF is generated/saved
            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PastelBlueCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                    }
                }
            }

            // Primary Action Buttons: Share PDF with Health Professional + Save PDF to Device
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { handleSharePdfWithDoctor() },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCharcoalText,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(50.dp)
                        .testTag("share_nutrition_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share PDF with Doctor",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = { handleSavePdfToDevice() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(50.dp)
                        .testTag("save_nutrition_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save PDF",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary
                    )
                }
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizePdfReportDialog(
            initialConfig = config,
            onDismiss = { showCustomizeDialog = false },
            onApply = { updated ->
                config = updated
                showCustomizeDialog = false
                statusMessage = "Updated PDF report configuration (${updated.period.label})."
            }
        )
    }
}

@Composable
private fun ClinicalMetricMiniPill(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.9f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkCharcoalText.copy(alpha = 0.65f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkCharcoalText
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = FreshGreenPrimary
            )
        }
    }
}

@Composable
private fun CustomizePdfReportDialog(
    initialConfig: NutritionReportExportConfig,
    onDismiss: () -> Unit,
    onApply: (NutritionReportExportConfig) -> Unit
) {
    var includeProfile by remember(initialConfig) { mutableStateOf(initialConfig.includeProfileAndBmi) }
    var includeDailyTable by remember(initialConfig) { mutableStateOf(initialConfig.includeDailySummaryTable) }
    var includeDetailedMeals by remember(initialConfig) { mutableStateOf(initialConfig.includeDetailedMealHistory) }
    var includeHydrationWeight by remember(initialConfig) { mutableStateOf(initialConfig.includeHydrationAndWeight) }
    var notesText by remember(initialConfig) { mutableStateOf(initialConfig.clinicianNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = FreshGreenPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customize Clinical PDF Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select the sections to include in the exported PDF for your healthcare provider or nutritionist:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PdfSectionToggleRow(
                    title = "Patient Profile & BMI Biometrics",
                    subtitle = "Age, height, current/target weight, BMI category & dietary style",
                    checked = includeProfile,
                    onCheckedChange = { includeProfile = it }
                )

                PdfSectionToggleRow(
                    title = "Day-by-Day Calorie & Macro Summary",
                    subtitle = "Daily totals for calories, protein, carbs, fat, fiber & water",
                    checked = includeDailyTable,
                    onCheckedChange = { includeDailyTable = it }
                )

                PdfSectionToggleRow(
                    title = "Detailed Meal & Scanned Food History",
                    subtitle = "Individual food logs with portion weights and macro breakdown",
                    checked = includeDetailedMeals,
                    onCheckedChange = { includeDetailedMeals = it }
                )

                PdfSectionToggleRow(
                    title = "Body Weight Trend Logs",
                    subtitle = "Include historical weight check-ins and notes",
                    checked = includeHydrationWeight,
                    onCheckedChange = { includeHydrationWeight = it }
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it.take(125) },
                    label = { Text("Notes for Doctor / Dietitian (Optional)") },
                    placeholder = { Text("e.g., Focusing on post-meal glucose control and higher protein breakfasts") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_clinician_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        initialConfig.copy(
                            includeProfileAndBmi = includeProfile,
                            includeDailySummaryTable = includeDailyTable,
                            includeDetailedMealHistory = includeDetailedMeals,
                            includeHydrationAndWeight = includeHydrationWeight,
                            clinicianNotes = notesText.trim()
                        )
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = DarkCharcoalText
                )
            ) {
                Text("Apply Settings", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PdfSectionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Modal Dialog wrapper so users can open the Clinical PDF Export flow from any screen
 * (such as the Food Diary & Scan History header).
 */
@Composable
fun NutritionPdfReportExportDialog(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord> = emptyList(),
    weightRecords: List<WeightRecord> = emptyList(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Nutritional Report (PDF)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                NutritionPdfReportExportCard(
                    profile = profile,
                    diaryEntries = diaryEntries,
                    waterRecords = waterRecords,
                    weightRecords = weightRecords
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCharcoalText,
                    contentColor = Color.White
                )
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
