package com.example.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDiaryEntry
import com.example.model.todayDateKey
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Redesigned Searchable Scanned Food History View listing past scanned food entries (and logged meals)
 * with full calorie and macronutrient breakdowns, dark-mode-safe cards, meal/category filters,
 * sort options, and 1-tap "Log Again" support.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannedFoodHistoryView(
    allEntries: List<FoodDiaryEntry>,
    onReLogEntry: (FoodDiaryEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onSaveAsRecipe: ((FoodDiaryEntry) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Scanned Only") }
    var selectedSort by remember { mutableStateOf("Newest") }

    val filteredHistory = remember(allEntries, searchQuery, selectedFilter, selectedSort) {
        val q = searchQuery.trim().lowercase()
        val baseList = allEntries.filter { entry ->
            val isScanned = entry.source.contains("Scan", ignoreCase = true) ||
                entry.source.contains("Barcode", ignoreCase = true) ||
                entry.source.contains("Voice", ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Scanned Only" -> isScanned || allEntries.none {
                    it.source.contains("Scan", ignoreCase = true) ||
                        it.source.contains("Barcode", ignoreCase = true) ||
                        it.source.contains("Voice", ignoreCase = true)
                }
                "All Logged" -> true
                "Breakfast", "Lunch", "Dinner", "Snacks" -> entry.mealType.equals(selectedFilter, ignoreCase = true)
                "High Protein" -> entry.protein >= 15.0
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                entry.foodName.lowercase().contains(q) ||
                entry.mealType.lowercase().contains(q) ||
                entry.dateKey.lowercase().contains(q) ||
                entry.notes.lowercase().contains(q) ||
                entry.source.lowercase().contains(q)
            matchesFilter && matchesQuery
        }

        when (selectedSort) {
            "Highest kcal" -> baseList.sortedByDescending { it.calories }
            "Highest Protein" -> baseList.sortedByDescending { it.protein }
            else -> baseList.sortedWith(
                compareByDescending<FoodDiaryEntry> { it.dateKey }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }
    }

    val totalScansCount = filteredHistory.size
    val avgCalories = if (filteredHistory.isNotEmpty()) {
        (filteredHistory.sumOf { it.calories }.toDouble() / filteredHistory.size).roundToInt()
    } else 0
    val totalProteinG = filteredHistory.sumOf { it.protein }.roundToInt()
    val totalCarbsG = filteredHistory.sumOf { it.carbohydrates }.roundToInt()
    val totalFatG = filteredHistory.sumOf { it.fat }.roundToInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .testTag("scanned_food_history_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Sleek Pill Search Bar
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Scanned History",
                    tint = FreshGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search scanned dishes, dates or notes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                            .fillMaxWidth()
                            .testTag("scan_history_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Filter & Sort Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "Scanned Only",
                "All Logged",
                "High Protein",
                "Breakfast",
                "Lunch",
                "Dinner",
                "Snacks"
            ).forEach { filterName ->
                val selected = selectedFilter == filterName
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filterName },
                    label = {
                        Text(
                            text = filterName,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FreshGreenPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("scan_history_filter_${filterName.lowercase().replace(" ", "_")}")
                )
            }

            listOf("Newest", "Highest kcal", "Highest Protein").forEach { sortMode ->
                val selected = selectedSort == sortMode
                FilterChip(
                    selected = selected,
                    onClick = { selectedSort = sortMode },
                    label = {
                        Text(
                            text = "Sort: $sortMode",
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 3. Dark-Mode-Safe Summary Hero Strip
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                HistoryStatMiniTile(
                    label = "Entries",
                    value = "$totalScansCount",
                    modifier = Modifier.weight(0.8f)
                )
                HistoryStatMiniTile(
                    label = "Avg kcal",
                    value = "$avgCalories",
                    modifier = Modifier.weight(0.95f)
                )
                HistoryStatMiniTile(
                    label = "Total Macros",
                    value = "${totalProteinG}P • ${totalCarbsG}C • ${totalFatG}F",
                    modifier = Modifier.weight(1.65f)
                )
            }
        }

        // 4. Searchable List of Past Scanned Entries or Empty State
        if (filteredHistory.isEmpty()) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "No entries match \"$searchQuery\""
                        } else {
                            "No Scanned Food History Yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Scan a meal with the Food Scanner or log a packaged barcode item to build your searchable macro history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("scan_history_list"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredHistory,
                    key = { it.id.ifBlank { "${it.foodName}_${it.dateKey}_${it.calories}_${it.hashCode()}" } }
                ) { entry ->
                    ScannedFoodHistoryItemCard(
                        entry = entry,
                        onReLog = {
                            onReLogEntry(
                                entry.copy(
                                    id = "",
                                    dateKey = todayDateKey(),
                                    source = if (entry.source.contains("Scan", ignoreCase = true)) "AI Scan" else entry.source
                                )
                            )
                        },
                        onDelete = {
                            if (entry.id.isNotBlank()) {
                                onDeleteEntry(entry.id)
                            }
                        },
                        onSaveAsRecipe = onSaveAsRecipe?.let { callback ->
                            { callback(entry) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryStatMiniTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannedFoodHistoryItemCard(
    entry: FoodDiaryEntry,
    onReLog: () -> Unit,
    onDelete: () -> Unit,
    onSaveAsRecipe: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pKcal = (entry.protein * 4.0).toFloat().coerceAtLeast(0f)
    val cKcal = (entry.carbohydrates * 4.0).toFloat().coerceAtLeast(0f)
    val fKcal = (entry.fat * 9.0).toFloat().coerceAtLeast(0f)
    val totalMacroKcal = (pKcal + cKcal + fKcal).coerceAtLeast(1f)

    val pShare = (pKcal / totalMacroKcal).coerceIn(0.05f, 1f)
    val cShare = (cKcal / totalMacroKcal).coerceIn(0.05f, 1f)
    val fShare = (fKcal / totalMacroKcal).coerceIn(0.05f, 1f)

    val confidencePct = (entry.confidence * 100).roundToInt().coerceIn(50, 100)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("scan_history_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Food Photo + Title + Date/Meal Metadata + Calorie Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        id = FoodDatabaseCatalog.foodImageResFor(entry.foodName, entry.mealType)
                    ),
                    contentDescription = entry.foodName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = entry.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${entry.dateKey} • ${entry.mealType} • ${entry.quantity.roundToInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entry.source} • $confidencePct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Dark-Mode-Safe Calorie Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${entry.calories}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshGreenPrimary
                        )
                    }
                }
            }

            // Proportional Macronutrient Split Bar (Protein | Carbs | Fat)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(pShare)
                            .fillMaxHeight()
                            .background(ProteinColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(cShare)
                            .fillMaxHeight()
                            .background(CarbsColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(fShare)
                            .fillMaxHeight()
                            .background(FatColor)
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MacroBreakdownPill(
                        label = "Protein",
                        grams = entry.protein,
                        kcal = (entry.protein * 4).roundToInt(),
                        color = ProteinColor
                    )
                    MacroBreakdownPill(
                        label = "Carbs",
                        grams = entry.carbohydrates,
                        kcal = (entry.carbohydrates * 4).roundToInt(),
                        color = CarbsColor
                    )
                    MacroBreakdownPill(
                        label = "Fat",
                        grams = entry.fat,
                        kcal = (entry.fat * 9).roundToInt(),
                        color = FatColor
                    )
                    if (entry.fiber > 0.0) {
                        MacroBreakdownPill(
                            label = "Fiber",
                            grams = entry.fiber,
                            kcal = null,
                            color = FreshGreenPrimary
                        )
                    }
                    if (entry.sugar > 0.0) {
                        MacroBreakdownPill(
                            label = "Sugar",
                            grams = entry.sugar,
                            kcal = null,
                            color = Color(0xFF9333EA)
                        )
                    }
                }
            }

            if (entry.notes.isNotBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bottom Action Row: 1-Tap "Log Again to Today" & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LimeAccent,
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onReLog() }
                        .testTag("relog_scanned_entry_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Log Again Today",
                            tint = DarkCharcoalText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Log Again Today",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSaveAsRecipe != null) {
                        IconButton(
                            onClick = onSaveAsRecipe,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("save_history_entry_recipe_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdd,
                                contentDescription = "Save to My Recipes",
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (entry.id.isNotBlank()) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete from History",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBreakdownPill(
    label: String,
    grams: Double,
    kcal: Int?,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (kcal != null) {
                    "$label ${(grams * 10).roundToInt() / 10.0}g ($kcal kcal)"
                } else {
                    "$label ${(grams * 10).roundToInt() / 10.0}g"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
