package com.example.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDatabaseItem
import com.example.model.SavedRecipe
import com.example.model.SavedRecipeItem
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Full "My Recipes" section for managing favorite scanned food combinations
 * and custom meal plans with search, category filters, builder dialog, and 1-tap diary logging.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyRecipesSection(
    savedRecipes: List<SavedRecipe>,
    language: String,
    onSaveRecipe: (SavedRecipe) -> Unit,
    onDeleteRecipe: (String) -> Unit,
    onToggleFavorite: (SavedRecipe) -> Unit,
    onLogRecipeToDiary: (SavedRecipe, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showBuilderDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<SavedRecipe?>(null) }
    var recipePendingSlotLog by remember { mutableStateOf<SavedRecipe?>(null) }

    val filteredRecipes = remember(savedRecipes, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        savedRecipes.filter { recipe ->
            val matchesFilter = when (selectedFilter) {
                "Favorites" -> recipe.isFavorite
                "Scanned Combos" -> recipe.category == SavedRecipe.CATEGORY_SCANNED_COMBO
                "Meal Plans" -> recipe.category == SavedRecipe.CATEGORY_CUSTOM_MEAL_PLAN
                "Favorite Meals" -> recipe.category == SavedRecipe.CATEGORY_FAVORITE_MEAL
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                recipe.title.lowercase().contains(q) ||
                recipe.notes.lowercase().contains(q) ||
                recipe.mealType.lowercase().contains(q) ||
                recipe.items.any { it.foodName.lowercase().contains(q) }
            matchesFilter && matchesQuery
        }
    }

    val favoritesCount = remember(savedRecipes) { savedRecipes.count { it.isFavorite } }
    val totalLogsCount = remember(savedRecipes) { savedRecipes.sumOf { it.usageCount } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_recipes_section"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Header Card with Create CTA
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = FreshGreenPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = FreshGreenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "My Recipes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Favorite scanned combos & custom meal plans",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                editingRecipe = null
                                showBuilderDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshGreenPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("create_custom_recipe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Summary Stats Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecipeSummaryPill(
                            label = "Saved",
                            value = "${savedRecipes.size}",
                            accent = FreshGreenPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        RecipeSummaryPill(
                            label = "Favorites",
                            value = "$favoritesCount",
                            accent = Color(0xFFEC4899),
                            modifier = Modifier.weight(1f)
                        )
                        RecipeSummaryPill(
                            label = "Quick Logs",
                            value = "$totalLogsCount×",
                            accent = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Search Bar & Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = FreshGreenPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear recipe search"
                                )
                            }
                        }
                    },
                    placeholder = { Text("Search saved recipes, dishes or notes...") },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_recipes_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "All",
                        "Favorites",
                        "Scanned Combos",
                        "Meal Plans",
                        "Favorite Meals"
                    ).forEach { filterLabel ->
                        val selected = selectedFilter == filterLabel
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filterLabel },
                            label = {
                                Text(
                                    text = filterLabel,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag(
                                "my_recipes_filter_${filterLabel.lowercase().replace(" ", "_")}"
                            )
                        )
                    }
                }
            }
        }

        // 3. Empty State or Saved Recipes List
        if (filteredRecipes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_recipes_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FreshGreenPrimary.copy(alpha = 0.14f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdded,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = if (savedRecipes.isEmpty()) {
                                "No Saved Recipes Yet"
                            } else {
                                "No Matching Recipes"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (savedRecipes.isEmpty()) {
                                "Save multi-item meals right after scanning, pin your favorite Daily Diary combos, or build custom meal plans for 1-tap logging."
                            } else {
                                "Try clearing your search or switching category filters."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                if (savedRecipes.isEmpty()) {
                                    editingRecipe = null
                                    showBuilderDialog = true
                                } else {
                                    searchQuery = ""
                                    selectedFilter = "All"
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshGreenPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("my_recipes_empty_cta_button")
                        ) {
                            Icon(
                                imageVector = if (savedRecipes.isEmpty()) Icons.Rounded.Add else Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (savedRecipes.isEmpty()) "Create Recipe or Meal Plan" else "Reset Filters",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredRecipes,
                key = { it.id.ifBlank { "${it.title}_${it.totalCalories}" } }
            ) { recipe ->
                SavedRecipeCard(
                    recipe = recipe,
                    onToggleFavorite = { onToggleFavorite(recipe) },
                    onQuickLog = { onLogRecipeToDiary(recipe, null) },
                    onPickSlotLog = { recipePendingSlotLog = recipe },
                    onEdit = {
                        editingRecipe = recipe
                        showBuilderDialog = true
                    },
                    onDelete = {
                        if (recipe.id.isNotBlank()) {
                            onDeleteRecipe(recipe.id)
                        }
                    }
                )
            }
        }
    }

    if (showBuilderDialog) {
        CreateOrEditSavedRecipeDialog(
            initialRecipe = editingRecipe,
            language = language,
            onDismiss = {
                showBuilderDialog = false
                editingRecipe = null
            },
            onSave = { built ->
                onSaveRecipe(built)
                showBuilderDialog = false
                editingRecipe = null
            }
        )
    }

    val pendingSlotRecipe = recipePendingSlotLog
    if (pendingSlotRecipe != null) {
        AlertDialog(
            onDismissRequest = { recipePendingSlotLog = null },
            title = {
                Text(
                    text = "Log '${pendingSlotRecipe.title}'",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose which meal slot to log all ${pendingSlotRecipe.items.size.coerceAtLeast(1)} item(s) into:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { slot ->
                        FilledTonalButton(
                            onClick = {
                                onLogRecipeToDiary(pendingSlotRecipe, slot)
                                recipePendingSlotLog = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Add to $slot", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { recipePendingSlotLog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedRecipeCard(
    recipe: SavedRecipe,
    onToggleFavorite: () -> Unit,
    onQuickLog: () -> Unit,
    onPickSlotLog: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedItems by remember { mutableStateOf(false) }

    val (catColor, catIcon) = when (recipe.category) {
        SavedRecipe.CATEGORY_CUSTOM_MEAL_PLAN -> Color(0xFF6366F1) to Icons.Rounded.CalendarMonth
        SavedRecipe.CATEGORY_SCANNED_COMBO -> FreshGreenPrimary to Icons.Rounded.CameraAlt
        else -> Color(0xFFF59E0B) to Icons.Rounded.RestaurantMenu
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_recipe_card_${recipe.id.ifBlank { recipe.title }}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Badges & Favorite/Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = catColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = recipe.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = recipe.mealType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    if (recipe.usageCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = LimeAccent.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "Logged ${recipe.usageCount}×",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_favorite_recipe_${recipe.id}")
                    ) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (recipe.isFavorite) "Remove favorite" else "Mark favorite",
                            tint = if (recipe.isFavorite) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_saved_recipe_${recipe.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit recipe",
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_saved_recipe_${recipe.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete recipe",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Title & Calorie Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${recipe.items.size.coerceAtLeast(1)} item(s) • ${recipe.totalWeightG.roundToInt()}g total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FreshGreenPrimary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${recipe.totalCalories} kcal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Macro Breakdown Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecipeMacroMiniTile("Protein", "${recipe.totalProteinG.roundToInt()}g", ProteinColor, Modifier.weight(1f))
                RecipeMacroMiniTile("Carbs", "${recipe.totalCarbsG.roundToInt()}g", CarbsColor, Modifier.weight(1f))
                RecipeMacroMiniTile("Fat", "${recipe.totalFatG.roundToInt()}g", FatColor, Modifier.weight(1f))
                RecipeMacroMiniTile("Fiber", "${recipe.totalFiberG.roundToInt()}g", FreshGreenPrimary, Modifier.weight(1f))
            }

            // Items Preview + Expand Toggle
            if (recipe.items.isNotEmpty()) {
                val visibleItems = if (expandedItems) recipe.items else recipe.items.take(2)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleItems.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.foodName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.mealSlot} • ${item.quantityG.roundToInt()}g • P:${item.proteinG.roundToInt()}g C:${item.carbohydratesG.roundToInt()}g F:${item.fatG.roundToInt()}g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.caloriesKcal} kcal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshGreenPrimary
                                )
                            }
                        }

                        if (recipe.items.size > 2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { expandedItems = !expandedItems }
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (expandedItems) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (expandedItems) {
                                        "Show less"
                                    } else {
                                        "Show all ${recipe.items.size} items"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (recipe.notes.isNotBlank()) {
                Text(
                    text = recipe.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Footer: 1-Tap Quick Log & Choose Meal Slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickLog,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("quick_log_recipe_${recipe.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAddCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (recipe.mealType == "Full Day") {
                            "Log Full Day"
                        } else {
                            "Log to ${recipe.mealType}"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPickSlotLog,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("pick_slot_log_recipe_${recipe.id}")
                ) {
                    Text(
                        text = "Pick Slot",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeSummaryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.11f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

@Composable
private fun RecipeMacroMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.11f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Interactive Modal Dialog for creating or editing a custom recipe, food combination,
 * or custom meal plan with quick-pick from FoodDatabaseCatalog or custom items.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditSavedRecipeDialog(
    initialRecipe: SavedRecipe?,
    language: String,
    onDismiss: () -> Unit,
    onSave: (SavedRecipe) -> Unit
) {
    var title by remember(initialRecipe) { mutableStateOf(initialRecipe?.title.orEmpty()) }
    var category by remember(initialRecipe) {
        mutableStateOf(initialRecipe?.category ?: SavedRecipe.CATEGORY_CUSTOM_MEAL_PLAN)
    }
    var mealType by remember(initialRecipe) {
        mutableStateOf(initialRecipe?.mealType ?: "Lunch")
    }
    var notes by remember(initialRecipe) { mutableStateOf(initialRecipe?.notes.orEmpty()) }

    val itemsList = remember(initialRecipe) {
        mutableStateListOf<SavedRecipeItem>().apply {
            initialRecipe?.items?.let { addAll(it) }
        }
    }

    var showDbPicker by remember { mutableStateOf(false) }
    var dbSearchQuery by remember { mutableStateOf("") }
    var showCustomItemForm by remember { mutableStateOf(itemsList.isEmpty()) }

    var customItemName by remember { mutableStateOf("") }
    var customItemGrams by remember { mutableStateOf("150") }
    var customItemKcal by remember { mutableStateOf("250") }
    var customItemProtein by remember { mutableStateOf("12") }
    var customItemCarbs by remember { mutableStateOf("30") }
    var customItemFat by remember { mutableStateOf("8") }
    var customItemSlot by remember { mutableStateOf(if (mealType == "Full Day") "Lunch" else mealType) }

    val previewTotals = remember(itemsList.toList()) {
        SavedRecipe(items = itemsList.toList()).withRecalculatedTotals()
    }

    val matchingCatalogItems = remember(dbSearchQuery) {
        FoodDatabaseCatalog.search(dbSearchQuery, "All").take(12)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRecipe == null) "Create Recipe / Meal Plan" else "Edit Saved Recipe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Recipe or Meal Plan Name") },
                    placeholder = { Text("e.g., High-Protein Dal Bhat Combo") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_builder_title_input")
                )

                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SavedRecipe.ALLOWED_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text(
                    text = "Default Meal Slot",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SavedRecipe.ALLOWED_MEAL_TYPES.forEach { slot ->
                        FilterChip(
                            selected = mealType == slot,
                            onClick = {
                                mealType = slot
                                if (slot != "Full Day") customItemSlot = slot
                            },
                            label = { Text(slot) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkCharcoalText,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Live Totals Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = FreshGreenPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${itemsList.size} item(s) • ${previewTotals.totalWeightG.roundToInt()}g",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${previewTotals.totalCalories} kcal • P:${previewTotals.totalProteinG.roundToInt()}g C:${previewTotals.totalCarbsG.roundToInt()}g F:${previewTotals.totalFatG.roundToInt()}g",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshGreenPrimary
                        )
                    }
                }

                // Added Items List
                itemsList.forEachIndexed { idx, item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.foodName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.mealSlot} • ${item.quantityG.roundToInt()}g • ${item.caloriesKcal} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { itemsList.removeAt(idx) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove item",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Quick Pick from Food Database vs Custom Item Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showDbPicker = !showDbPicker
                            if (showDbPicker) showCustomItemForm = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("recipe_builder_pick_db_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick from Recipe's", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            showCustomItemForm = !showCustomItemForm
                            if (showCustomItemForm) showDbPicker = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("recipe_builder_custom_item_toggle")
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Dish", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                AnimatedVisibility(visible = showDbPicker) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = dbSearchQuery,
                                onValueChange = { dbSearchQuery = it },
                                label = { Text("Search Indian, South Indian, Nepali foods...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            matchingCatalogItems.forEach { dbItem: FoodDatabaseItem ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            itemsList.add(
                                                SavedRecipeItem(
                                                    foodName = dbItem.localizedName(language),
                                                    quantityG = dbItem.servingSizeG,
                                                    caloriesKcal = dbItem.calories,
                                                    proteinG = dbItem.protein,
                                                    carbohydratesG = dbItem.carbohydrates,
                                                    fatG = dbItem.fat,
                                                    fiberG = dbItem.fiber,
                                                    mealSlot = if (mealType == "Full Day") customItemSlot else mealType
                                                )
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dbItem.localizedName(language),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${dbItem.servingSizeG.roundToInt()}g • ${dbItem.calories} kcal • P:${dbItem.protein}g",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add ${dbItem.nameEn}",
                                            tint = FreshGreenPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showCustomItemForm) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customItemName,
                                onValueChange = { customItemName = it },
                                label = { Text("Dish / Ingredient Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recipe_builder_item_name_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customItemGrams,
                                    onValueChange = { customItemGrams = it },
                                    label = { Text("Grams") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customItemKcal,
                                    onValueChange = { customItemKcal = it },
                                    label = { Text("kcal") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customItemProtein,
                                    onValueChange = { customItemProtein = it },
                                    label = { Text("Pro (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customItemCarbs,
                                    onValueChange = { customItemCarbs = it },
                                    label = { Text("Carb (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customItemFat,
                                    onValueChange = { customItemFat = it },
                                    label = { Text("Fat (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (mealType == "Full Day") {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { s ->
                                        FilterChip(
                                            selected = customItemSlot == s,
                                            onClick = { customItemSlot = s },
                                            label = { Text(s) },
                                            shape = RoundedCornerShape(50)
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    if (customItemName.isNotBlank()) {
                                        itemsList.add(
                                            SavedRecipeItem(
                                                foodName = customItemName.trim(),
                                                quantityG = customItemGrams.toDoubleOrNull() ?: 150.0,
                                                caloriesKcal = customItemKcal.toIntOrNull() ?: 200,
                                                proteinG = customItemProtein.toDoubleOrNull() ?: 10.0,
                                                carbohydratesG = customItemCarbs.toDoubleOrNull() ?: 25.0,
                                                fatG = customItemFat.toDoubleOrNull() ?: 6.0,
                                                fiberG = 3.0,
                                                mealSlot = if (mealType == "Full Day") customItemSlot else mealType
                                            )
                                        )
                                        customItemName = ""
                                    }
                                },
                                enabled = customItemName.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("recipe_builder_add_item_button")
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Dish to Recipe", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Preparation Tips (Optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val finalItems = itemsList.toList().ifEmpty {
                                listOf(
                                    SavedRecipeItem(
                                        foodName = title.trim().ifBlank { "Custom Meal" },
                                        quantityG = customItemGrams.toDoubleOrNull() ?: 200.0,
                                        caloriesKcal = customItemKcal.toIntOrNull() ?: 300,
                                        proteinG = customItemProtein.toDoubleOrNull() ?: 15.0,
                                        carbohydratesG = customItemCarbs.toDoubleOrNull() ?: 35.0,
                                        fatG = customItemFat.toDoubleOrNull() ?: 8.0,
                                        fiberG = 4.0,
                                        mealSlot = if (mealType == "Full Day") "Lunch" else mealType
                                    )
                                )
                            }
                            val built = SavedRecipe(
                                id = initialRecipe?.id.orEmpty(),
                                userId = initialRecipe?.userId.orEmpty(),
                                title = title.trim().ifBlank { finalItems.first().foodName },
                                category = category,
                                mealType = mealType,
                                items = finalItems,
                                notes = notes.trim(),
                                isFavorite = initialRecipe?.isFavorite ?: true,
                                usageCount = initialRecipe?.usageCount ?: 0,
                                createdAt = initialRecipe?.createdAt
                            ).withRecalculatedTotals()
                            onSave(built)
                        },
                        enabled = title.isNotBlank() || itemsList.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FreshGreenPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("recipe_builder_save_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Recipe", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/**
 * Compact horizontal carousel shown on the Home Dashboard for instant 1-tap logging
 * of saved favorite scanned combos and custom meal plans.
 */
@Composable
fun MyRecipesHomeQuickAccessCard(
    savedRecipes: List<SavedRecipe>,
    onQuickLogRecipe: (SavedRecipe) -> Unit,
    onOpenMyRecipes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_my_recipes_quick_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FreshGreenPrimary.copy(alpha = 0.14f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "My Recipes & Combos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (savedRecipes.isEmpty()) {
                                "Save scanned combos & custom meal plans"
                            } else {
                                "${savedRecipes.size} saved for 1-tap logging"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onOpenMyRecipes,
                    modifier = Modifier.testTag("home_open_my_recipes_button")
                ) {
                    Text(
                        text = if (savedRecipes.isEmpty()) "Create" else "View All",
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary
                    )
                }
            }

            if (savedRecipes.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    FreshGreenPrimary.copy(alpha = 0.12f),
                                    LimeAccent.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .clickable { onOpenMyRecipes() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Build your personal quick-log cookbook",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to save custom meal plans or scanned food combinations.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = FreshGreenPrimary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Create Recipe",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = savedRecipes.take(8),
                        key = { it.id.ifBlank { it.title } }
                    ) { recipe ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .width(205.dp)
                                .clickable { onOpenMyRecipes() }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = FreshGreenPrimary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = recipe.mealType,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FreshGreenPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${recipe.totalCalories} kcal",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = recipe.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${recipe.items.size} item(s) • P:${recipe.totalProteinG.roundToInt()}g C:${recipe.totalCarbsG.roundToInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FilledTonalButton(
                                    onClick = { onQuickLogRecipe(recipe) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlaylistAddCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quick Log", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
