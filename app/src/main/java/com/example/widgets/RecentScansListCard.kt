package com.example.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FoodDiaryEntry
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import androidx.compose.material.icons.filled.GraphicEq
import com.example.ui.theme.ProteinColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Home Screen "Recent Scans" Component that fetches and displays the last 5 food items
 * from Cloud Firestore (`users/{userId}/foodDiary`) in a scrollable list.
 */
@Composable
fun RecentScansListCard(
    recentScansFromFirestore: List<FoodDiaryEntry>,
    allDiaryEntriesFallback: List<FoodDiaryEntry>,
    selectedDateKey: String,
    isCloudSynced: Boolean = true,
    isRefreshing: Boolean = false,
    onRefreshFromFirestore: () -> Unit = {},
    onReLogFoodItem: (FoodDiaryEntry) -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Layout mode: 0 = Vertical Scrollable List (default), 1 = Horizontal Scrollable Carousel
    var listLayoutMode by remember { mutableIntStateOf(0) }
    // Filter mode: 0 = All Recent / Search Results, 1 = AI & Barcode Scans
    var filterMode by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val voiceInputController = rememberVoiceInputController { spoken -> searchQuery = spoken }
    var inspectedEntry by remember { mutableStateOf<FoodDiaryEntry?>(null) }

    // Unified list of all past food entries from Firestore and fallback history
    val fullSourceList = remember(recentScansFromFirestore, allDiaryEntriesFallback) {
        val combined = LinkedHashMap<String, FoodDiaryEntry>()
        // Recent Firestore items first
        recentScansFromFirestore.forEach { entry ->
            val key = entry.id.ifBlank { "${entry.foodName}_${entry.dateKey}_${entry.mealType}_${entry.calories}" }
            combined[key] = entry
        }
        // Then complete diary history sorted chronologically
        allDiaryEntriesFallback.sortedWith(
            compareByDescending<FoodDiaryEntry> {
                it.createdAt?.seconds ?: it.updatedAt?.seconds ?: 0L
            }.thenByDescending { it.dateKey }
        ).forEach { entry ->
            val key = entry.id.ifBlank { "${entry.foodName}_${entry.dateKey}_${entry.mealType}_${entry.calories}" }
            if (!combined.containsKey(key)) {
                combined[key] = entry
            }
        }
        combined.values.toList()
    }

    val displayItems = remember(fullSourceList, filterMode, searchQuery) {
        val modeFiltered = if (filterMode == 1) {
            fullSourceList.filter {
                it.source.equals("AI Scan", ignoreCase = true) ||
                    it.source.contains("Scan", ignoreCase = true) ||
                    it.notes.contains("Voice", ignoreCase = true) ||
                    it.notes.contains("Barcode", ignoreCase = true)
            }
        } else {
            fullSourceList
        }

        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isEmpty()) {
            modeFiltered.take(5)
        } else {
            val terms = trimmedQuery.lowercase(Locale.ROOT).split("\\s+".toRegex()).filter { it.isNotBlank() }
            modeFiltered.filter { entry ->
                val combinedText = "${entry.foodName} ${entry.mealType} ${entry.notes} ${entry.source} ${entry.dateKey}".lowercase(Locale.ROOT)
                terms.all { term -> combinedText.contains(term) }
            }.take(25)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_scans_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row with Title, Firestore Last-5 Badge, Layout Toggle & Refresh Button
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
                        color = FreshGreenPrimary.copy(alpha = 0.14f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Recent Scans",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = FreshGreenPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.testTag("firestore_offline_persistence_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = FreshGreenPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isCloudSynced) "Firestore Cache • Offline Ready" else "Local • Last 5",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FreshGreenPrimary
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Last 5 food items cached with Firestore local persistence for 100% offline access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Layout Switcher & Firestore Refresh Icon Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { listLayoutMode = if (listLayoutMode == 0) 1 else 0 },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("recent_scans_layout_toggle")
                    ) {
                        Icon(
                            imageVector = if (listLayoutMode == 0) Icons.Default.ViewCarousel else Icons.Default.ViewAgenda,
                            contentDescription = "Switch Recent Scans View Mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRefreshFromFirestore,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("recent_scans_refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = FreshGreenPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Fetch Latest 5 Items from Firestore",
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Search Bar for Finding Past Food Entries by Keywords
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (searchQuery.isNotBlank()) FreshGreenPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recent_scans_search_container")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search recent scans",
                            tint = if (searchQuery.isNotBlank()) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(FreshGreenPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp)
                                .testTag("recent_scans_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search past scans (e.g. Oats, Egg, Coffee)...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("recent_scans_search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        VoiceSearchIconButton(
                            controller = voiceInputController,
                            testTag = "recent_scans_voice_search_mic_button"
                        )
                    }
                }

                // Voice Listening feedback banner
                AnimatedVisibility(visible = voiceInputController.isListening) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FreshGreenPrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LimeAccent),
                        modifier = Modifier.fillMaxWidth().testTag("recent_scans_voice_status_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Listening... Speak food name to search recent scans",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Keyword Suggestion Pills (Horizontally Scrollable)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    val quickPills = listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Protein", "Coffee", "Fruit")
                    items(quickPills.size) { idx ->
                        val pill = quickPills[idx]
                        val isSelected = searchQuery.equals(pill, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) FreshGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) FreshGreenPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    searchQuery = if (isSelected) "" else pill
                                }
                                .testTag("recent_scans_quick_pill_$pill")
                        ) {
                            Text(
                                text = pill,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Filter Chips ("All Recent" / "Search Results" vs "AI & Camera Scans") + "Scan Meal" Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allRecentLabel = if (searchQuery.isNotBlank()) {
                        "Found (${displayItems.size})"
                    } else {
                        "All Recent (${displayItems.size}/5)"
                    }

                    listOf(
                        0 to allRecentLabel,
                        1 to "AI & Barcode"
                    ).forEach { (idx, label) ->
                        val selected = filterMode == idx
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) DarkCharcoalText else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .clickable { filterMode = idx }
                                .testTag("recent_scans_filter_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = LimeAccent.copy(alpha = 0.45f),
                    modifier = Modifier
                        .clickable { onNavigateToScanner() }
                        .testTag("recent_scans_open_scanner_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = DarkCharcoalText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "New Scan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                    }
                }
            }

            // 4. Scrollable List of Past Food Items (or Search / Default Empty State)
            if (displayItems.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    RecentScansSearchEmptyState(
                        query = searchQuery,
                        onClearQuery = { searchQuery = "" },
                        onNavigateToScanner = onNavigateToScanner
                    )
                } else {
                    RecentScansEmptyState(onNavigateToScanner = onNavigateToScanner)
                }
            } else {
                if (listLayoutMode == 0) {
                    // Vertical Scrollable List (LazyColumn with bounded height so it scrolls smoothly inside the home screen)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 350.dp)
                            .testTag("recent_scans_scrollable_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        itemsIndexed(
                            items = displayItems,
                            key = { idx, item -> item.id.ifBlank { "${item.foodName}_$idx" } }
                        ) { index, entry ->
                            RecentScanVerticalItemCard(
                                index = index,
                                entry = entry,
                                onInspect = { inspectedEntry = entry },
                                onQuickReLog = {
                                    onReLogFoodItem(
                                        entry.copy(
                                            id = "",
                                            dateKey = selectedDateKey
                                        )
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // Horizontal Scrollable Carousel (LazyRow)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_scans_horizontal_list"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        itemsIndexed(
                            items = displayItems,
                            key = { idx, item -> item.id.ifBlank { "${item.foodName}_$idx" } }
                        ) { index, entry ->
                            RecentScanHorizontalCard(
                                index = index,
                                entry = entry,
                                onInspect = { inspectedEntry = entry },
                                onQuickReLog = {
                                    onReLogFoodItem(
                                        entry.copy(
                                            id = "",
                                            dateKey = selectedDateKey
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Inspect & Portion Adjustment Dialog before Re-logging
    inspectedEntry?.let { entry ->
        RecentScanInspectModalDialog(
            entry = entry,
            selectedDateKey = selectedDateKey,
            onDismiss = { inspectedEntry = null },
            onConfirmReLog = { customizedEntry ->
                onReLogFoodItem(customizedEntry)
                inspectedEntry = null
            }
        )
    }
}

@Composable
private fun RecentScanVerticalItemCard(
    index: Int,
    entry: FoodDiaryEntry,
    onInspect: () -> Unit,
    onQuickReLog: () -> Unit
) {
    val (sourceLabel, sourceIcon, sourceColor) = resolveScanSourceBadge(entry)
    val timeLabel = remember(entry.createdAt, entry.updatedAt, entry.dateKey) {
        formatEntryTimestamp(entry)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onInspect() }
            .testTag("recent_scan_item_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank + Source Icon Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = sourceColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = sourceIcon,
                            contentDescription = null,
                            tint = sourceColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.foodName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = sourceColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = sourceColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Calorie + Portion + Meal Type + Timestamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${entry.calories} kcal • ${entry.quantity.roundToInt()}g",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshGreenPrimary
                        )
                        Text(
                            text = "• ${entry.mealType} • $timeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Mini Macro Pills Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "P ${entry.protein.roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ProteinColor
                        )
                        Text(
                            text = "C ${entry.carbohydrates.roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CarbsColor
                        )
                        Text(
                            text = "F ${entry.fat.roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FatColor
                        )
                        if (entry.confidence in 0.01..0.99) {
                            Text(
                                text = "• ${(entry.confidence * 100).roundToInt()}% AI conf",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // One-tap Re-log Button
            FilledTonalButton(
                onClick = onQuickReLog,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("recent_scan_relog_button_$index")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log Again",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Log",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun RecentScanHorizontalCard(
    index: Int,
    entry: FoodDiaryEntry,
    onInspect: () -> Unit,
    onQuickReLog: () -> Unit
) {
    val (sourceLabel, sourceIcon, sourceColor) = resolveScanSourceBadge(entry)
    val timeLabel = remember(entry.createdAt, entry.updatedAt, entry.dateKey) {
        formatEntryTimestamp(entry)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .width(235.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onInspect() }
            .testTag("recent_scan_carousel_item_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = sourceColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = sourceIcon,
                            contentDescription = null,
                            tint = sourceColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sourceColor
                        )
                    }
                }

                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = entry.foodName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${entry.calories} kcal • ${entry.quantity.roundToInt()}g • ${entry.mealType}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P:${entry.protein.roundToInt()}g C:${entry.carbohydrates.roundToInt()}g F:${entry.fat.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = FreshGreenPrimary,
                    modifier = Modifier
                        .clickable { onQuickReLog() }
                        .testTag("recent_scan_carousel_relog_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Re-log",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Log",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentScansSearchEmptyState(
    query: String,
    onClearQuery: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_scans_search_empty_state")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = FreshGreenPrimary.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = "No Scans Matching \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "We couldn't find any past logged foods matching your keyword. Try checking for typos, searching another ingredient, or logging a new scan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onClearQuery,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("clear_recent_scans_search_button")
                ) {
                    Text("Clear Search", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onNavigateToScanner,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("scan_new_from_search_empty_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Meal", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun RecentScansEmptyState(
    onNavigateToScanner: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_scans_empty_state")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = FreshGreenPrimary.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = "No Recent Scans Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Scan a meal with your camera, barcode scanner, or voice logger. All items are saved to Firestore with local persistence so they remain accessible anytime, even offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onNavigateToScanner,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("empty_recent_scans_cta_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scan Your First Meal",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun RecentScanInspectModalDialog(
    entry: FoodDiaryEntry,
    selectedDateKey: String,
    onDismiss: () -> Unit,
    onConfirmReLog: (FoodDiaryEntry) -> Unit
) {
    var scaleMultiplier by remember { mutableDoubleStateOf(1.0) }
    var targetMealType by remember { mutableStateOf(entry.mealType) }

    val baseGrams = entry.quantity.coerceAtLeast(1.0)
    val scaledGrams = (baseGrams * scaleMultiplier).roundToInt().coerceIn(5, 5000)
    val scaledCalories = (entry.calories * scaleMultiplier).roundToInt().coerceAtLeast(0)
    val scaledProtein = ((entry.protein * scaleMultiplier) * 10.0).roundToInt() / 10.0
    val scaledCarbs = ((entry.carbohydrates * scaleMultiplier) * 10.0).roundToInt() / 10.0
    val scaledFat = ((entry.fat * scaleMultiplier) * 10.0).roundToInt() / 10.0
    val scaledFiber = ((entry.fiber * scaleMultiplier) * 10.0).roundToInt() / 10.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recent Scan Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = FreshGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = entry.foodName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (entry.notes.isNotBlank()) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Macro Summary
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = FreshGreenPrimary.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$scaledCalories kcal", fontWeight = FontWeight.ExtraBold, color = FreshGreenPrimary)
                            Text("Calories", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${scaledProtein}g", fontWeight = FontWeight.ExtraBold, color = ProteinColor)
                            Text("Protein", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${scaledCarbs}g", fontWeight = FontWeight.ExtraBold, color = CarbsColor)
                            Text("Carbs", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${scaledFat}g", fontWeight = FontWeight.ExtraBold, color = FatColor)
                            Text("Fat", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Portion Adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Adjust Portion Weight:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    scaleMultiplier = (scaleMultiplier - 0.25).coerceAtLeast(0.25)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Less")
                            }
                        }
                        Text(
                            text = "${scaledGrams}g",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    scaleMultiplier = (scaleMultiplier + 0.25).coerceAtMost(4.0)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "More")
                            }
                        }
                    }
                }

                // Target Meal Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { meal ->
                        val selected = targetMealType == meal
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) FreshGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { targetMealType = meal }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = meal,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Button(
                    onClick = {
                        onConfirmReLog(
                            entry.copy(
                                id = "",
                                dateKey = selectedDateKey,
                                mealType = targetMealType,
                                quantity = scaledGrams.toDouble(),
                                calories = scaledCalories,
                                protein = scaledProtein,
                                carbohydrates = scaledCarbs,
                                fat = scaledFat,
                                fiber = scaledFiber
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log to $targetMealType (+25 XP)",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private fun resolveScanSourceBadge(entry: FoodDiaryEntry): Triple<String, ImageVector, Color> {
    return when {
        entry.notes.contains("Voice", ignoreCase = true) -> {
            Triple("Voice AI", Icons.Default.Mic, Color(0xFF0EA5E9))
        }
        entry.notes.contains("Barcode", ignoreCase = true) -> {
            Triple("Barcode", Icons.Default.QrCodeScanner, Color(0xFF8B5CF6))
        }
        entry.source.contains("Scan", ignoreCase = true) -> {
            Triple("AI Scan", Icons.Default.CameraAlt, FreshGreenPrimary)
        }
        entry.source.equals("Database", ignoreCase = true) -> {
            Triple("Recipe's", Icons.Default.Restaurant, Color(0xFFF59E0B))
        }
        else -> {
            Triple(entry.source.ifBlank { "Logged" }, Icons.Default.AutoAwesome, FreshGreenPrimary)
        }
    }
}

private fun formatEntryTimestamp(entry: FoodDiaryEntry): String {
    val ts = entry.createdAt ?: entry.updatedAt
    if (ts != null) {
        val date = Date(ts.seconds * 1000L)
        val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        return timeFmt.format(date)
    }
    return entry.dateKey
}
