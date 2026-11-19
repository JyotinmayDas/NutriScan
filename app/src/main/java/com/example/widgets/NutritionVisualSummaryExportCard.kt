package com.example.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.export.NutritionVisualReportData
import com.example.core.export.NutritionVisualReportExporter
import com.example.core.export.VisualReportPeriod
import com.example.core.export.VisualReportTheme
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.todayDateKey
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.FatColor
import com.example.ui.theme.FiberColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.ProteinColor

/**
 * Interactive card allowing users to switch between Daily and Weekly (7-Day) nutrition history,
 * preview a generated visual summary graphic, save it as a PNG image, or share it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionVisualSummaryExportCard(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord> = emptyList(),
    anchorDateKey: String = todayDateKey(),
    initialPeriod: VisualReportPeriod = VisualReportPeriod.DAILY,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPeriod by remember(initialPeriod) { mutableStateOf(initialPeriod) }
    var selectedTheme by remember { mutableStateOf(VisualReportTheme.FRESH_LIGHT) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRenderedBitmapDialog by remember { mutableStateOf(false) }

    val reportData = remember(profile, diaryEntries, waterRecords, selectedPeriod, anchorDateKey) {
        NutritionVisualReportExporter.buildVisualReportData(
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            period = selectedPeriod,
            anchorDateKey = anchorDateKey
        )
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("nutrition_visual_summary_export_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row
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
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Visual Nutrition Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Export daily or weekly nutrition history as a shareable image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showRenderedBitmapDialog = true }
                        .testTag("preview_visual_report_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Preview Full Visual Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Daily vs Weekly Segmented Toggle
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    VisualReportPeriod.entries.forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) FreshGreenPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selectedPeriod = period
                                    statusMessage = null
                                }
                                .testTag("visual_report_period_${period.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Visual Style Theme Selector Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualReportTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) FreshGreenPrimary else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedTheme = theme }
                            .testTag("visual_report_theme_${theme.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.bgStartHex))
                                    .border(1.dp, Color(theme.accentHex), CircleShape)
                            )
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // 4. Live Visual Summary Report Graphic Preview
            VisualNutritionReportLivePreview(
                data = reportData,
                theme = selectedTheme,
                onClick = { showRenderedBitmapDialog = true }
            )

            // 5. Status Feedback Banner
            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 6. Export Actions Row: Save Image (PNG) + Share Visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        NutritionVisualReportExporter.saveVisualReportToGallery(
                            context = context,
                            data = reportData,
                            theme = selectedTheme
                        ).onSuccess { savedPath ->
                            statusMessage = "Saved visual summary to $savedPath"
                        }.onFailure { err ->
                            statusMessage = "Could not save visual report: ${err.localizedMessage ?: "Error"}"
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_visual_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Image",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        NutritionVisualReportExporter.shareVisualReport(
                            context = context,
                            data = reportData,
                            theme = selectedTheme
                        ).onSuccess { file ->
                            statusMessage = "Generated ${file.name}. Opening Share sheet…"
                        }.onFailure { err ->
                            statusMessage = "Could not share visual report: ${err.localizedMessage ?: "Error"}"
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(50.dp)
                        .testTag("share_visual_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Visual",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showRenderedBitmapDialog) {
        val renderedBitmap = remember(reportData, selectedTheme) {
            NutritionVisualReportExporter.renderVisualReportBitmap(reportData, selectedTheme)
        }
        AlertDialog(
            onDismissRequest = { showRenderedBitmapDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = FreshGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generated Summary Image",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = "Generated ${reportData.period.label} Nutrition Summary Graphic",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1080f / 1440f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    )
                    Text(
                        text = "High-resolution 1080×1440 PNG ready to save or share.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        NutritionVisualReportExporter.shareVisualReport(
                            context = context,
                            data = reportData,
                            theme = selectedTheme
                        ).onSuccess { file ->
                            statusMessage = "Generated ${file.name}. Opening Share sheet…"
                            showRenderedBitmapDialog = false
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PNG", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenderedBitmapDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Live Compose preview mirroring the generated 1080x1440 PNG Visual Nutrition Summary card.
 */
@Composable
fun VisualNutritionReportLivePreview(
    data: NutritionVisualReportData,
    theme: VisualReportTheme,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(theme.bgStartHex), Color(theme.bgEndHex))
    )
    val cardColor = Color(theme.cardHex)
    val subCardColor = Color(theme.subCardHex)
    val primaryText = Color(theme.primaryTextHex)
    val secondaryText = Color(theme.secondaryTextHex)
    val accent = Color(theme.accentHex)
    val accentSoft = Color(theme.accentSoftHex)
    val border = Color(theme.borderHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bgBrush)
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("visual_report_live_preview")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, border, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentSoft
                    ) {
                        Text(
                            text = data.period.badgeTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent
                    ) {
                        Text(
                            text = "NutriScan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = data.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = data.dateRangeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }

                // Hero Area: Daily Ring vs Weekly 7-Day Bar Chart
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = subCardColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (data.period == VisualReportPeriod.DAILY) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(92.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeW = 10.dp.toPx()
                                    drawArc(
                                        color = border,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    val sweep = 360f * (data.calorieProgressPct / 100f).coerceIn(0.03f, 1f)
                                    drawArc(
                                        color = accent,
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${data.caloriesDisplay}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryText
                                    )
                                    Text(
                                        text = "/ ${data.calorieGoal}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryText
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PreviewCompactMetricRow(
                                    label = "Goal Progress",
                                    value = "${data.calorieProgressPct}%",
                                    cardColor = cardColor,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText
                                )
                                PreviewCompactMetricRow(
                                    label = "Meals Logged",
                                    value = "${data.totalMealsLogged} today",
                                    cardColor = cardColor,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText
                                )
                                PreviewCompactMetricRow(
                                    label = "Hydration",
                                    value = "${data.waterMl} / ${data.waterGoalMl} ml",
                                    cardColor = cardColor,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText
                                )
                            }
                        }
                    } else {
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
                                Text(
                                    text = "Avg ${data.caloriesDisplay} kcal/day",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                Text(
                                    text = "${data.activeDaysCount}/7 days • ${data.totalMealsLogged} meals",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }

                            val maxCal = maxOf(
                                data.calorieGoal * 1.25f,
                                (data.weeklyBars.maxOfOrNull { it.calories } ?: data.calorieGoal).toFloat(),
                                1000f
                            )
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                            ) {
                                val slotW = size.width / data.weeklyBars.size.coerceAtLeast(1)
                                val barW = (slotW * 0.46f).coerceAtMost(22.dp.toPx())
                                val goalY = size.height - ((data.calorieGoal / maxCal).coerceIn(0f, 1f) * size.height)

                                drawLine(
                                    color = Color(0xFFFF6200),
                                    start = Offset(0f, goalY),
                                    end = Offset(size.width, goalY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )

                                data.weeklyBars.forEachIndexed { idx, bar ->
                                    val cx = (idx + 0.5f) * slotW
                                    drawRoundRect(
                                        color = border.copy(alpha = 0.5f),
                                        topLeft = Offset(cx - barW / 2f, 0f),
                                        size = Size(barW, size.height),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                    val barH = if (bar.calories > 0) {
                                        ((bar.calories / maxCal).coerceIn(0.06f, 1f) * size.height)
                                    } else {
                                        4.dp.toPx()
                                    }
                                    drawRoundRect(
                                        color = if (bar.calories > 0) accent else border,
                                        topLeft = Offset(cx - barW / 2f, size.height - barH),
                                        size = Size(barW, barH),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                data.weeklyBars.forEach { bar ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = bar.dayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryText
                                        )
                                        Text(
                                            text = if (bar.calories > 0) "${bar.calories}" else "–",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = primaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Macro Progress Bars
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = subCardColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreviewMacroProgressRow("Protein", data.proteinG, data.proteinGoalG, data.proteinPct, ProteinColor, border, primaryText, secondaryText)
                        PreviewMacroProgressRow("Carbs", data.carbsG, data.carbsGoalG, data.carbsPct, CarbsColor, border, primaryText, secondaryText)
                        PreviewMacroProgressRow("Fat", data.fatG, data.fatGoalG, data.fatPct, FatColor, border, primaryText, secondaryText)
                        PreviewMacroProgressRow("Fiber", data.fiberG, data.fiberGoalG, data.fiberPct, FiberColor, border, primaryText, secondaryText)
                    }
                }

                // Meal Slot Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    data.mealBreakdown.forEach { slot ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = subCardColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = slot.mealType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${slot.calories}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                Text(
                                    text = "kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                        }
                    }
                }

                // Insight Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.insightText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCompactMetricRow(
    label: String,
    value: String,
    cardColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText
            )
        }
    }
}

@Composable
private fun PreviewMacroProgressRow(
    label: String,
    actualG: Int,
    goalG: Int,
    pct: Int,
    barColor: Color,
    trackColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )
            Text(
                text = "${actualG}g / ${goalG}g ($pct%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = secondaryText
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((pct / 100f).coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Modal Dialog wrapper for exporting Daily or Weekly Visual Summary Reports from quick-action buttons.
 */
@Composable
fun NutritionVisualSummaryExportDialog(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord> = emptyList(),
    anchorDateKey: String = todayDateKey(),
    initialPeriod: VisualReportPeriod = VisualReportPeriod.DAILY,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Visual Nutrition Summary",
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
                NutritionVisualSummaryExportCard(
                    profile = profile,
                    diaryEntries = diaryEntries,
                    waterRecords = waterRecords,
                    anchorDateKey = anchorDateKey,
                    initialPeriod = initialPeriod
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
