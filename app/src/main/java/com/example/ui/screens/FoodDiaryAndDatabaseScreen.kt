package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.MenuBook
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.SavedRecipe
import com.example.model.UserProfile
import com.example.ui.theme.FreshGreenPrimary
import com.example.widgets.DailyMacroPieChartCard
import com.example.widgets.MyRecipesSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private fun recentDateKeys(): List<Pair<String, String>> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelSdf = SimpleDateFormat("MMM d", Locale.US)
    return (0..4).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val key = sdf.format(cal.time)
        val label = when (daysAgo) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> labelSdf.format(cal.time)
        }
        key to label
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDiaryAndDatabaseScreen(
    language: String,
    selectedDateKey: String,
    diaryEntries: List<FoodDiaryEntry>,
    onSelectDate: (String) -> Unit,
    onAddDatabaseFood: (FoodDatabaseItem, Double, String) -> Unit,
    onAddManualFood: (FoodDiaryEntry) -> Unit,
    onUpdateDiaryEntry: (FoodDiaryEntry, String, Double) -> Unit,
    onDeleteDiaryEntry: (String) -> Unit,
    profile: UserProfile = UserProfile(language = language),
    @Suppress("UNUSED_PARAMETER") onSaveProfile: (UserProfile) -> Unit = {},
    savedRecipes: List<SavedRecipe> = emptyList(),
    initialSubTab: Int = 0,
    onInitialSubTabConsumed: () -> Unit = {},
    onSaveCustomRecipe: (SavedRecipe) -> Unit = {},
    onDeleteSavedRecipe: (String) -> Unit = {},
    onToggleSavedRecipeFavorite: (SavedRecipe) -> Unit = {},
    onLogSavedRecipe: (SavedRecipe, String?) -> Unit = { _, _ -> },
    onSaveDiaryGroupAsRecipe: (title: String, mealType: String, entries: List<FoodDiaryEntry>) -> Unit = { _, _, _ -> }
) {
    // 0 = Diary, 1 = My Recipes, 2 = Recipe's (Food Catalog)
    var activeSubTab by remember { mutableIntStateOf(initialSubTab.coerceIn(0, 2)) }
    var showManualDialog by remember { mutableStateOf(false) }
    var manualDefaultMeal by remember { mutableStateOf("Lunch") }
    var editingDiaryEntry by remember { mutableStateOf<FoodDiaryEntry?>(null) }
    var selectedDbFoodForAdd by remember { mutableStateOf<FoodDatabaseItem?>(null) }
    var selectedDbFoodForRecipe by remember { mutableStateOf<FoodDatabaseItem?>(null) }
    var mealGroupPendingSave by remember { mutableStateOf<Pair<String, List<FoodDiaryEntry>>?>(null) }

    LaunchedEffect(initialSubTab) {
        if (initialSubTab in 1..2) {
            activeSubTab = initialSubTab
            onInitialSubTabConsumed()
        }
    }

    val dateOptions = remember { recentDateKeys() }
    val dayEntries = remember(diaryEntries, selectedDateKey) {
        diaryEntries.filter { it.dateKey == selectedDateKey }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Clean 3-Way Segmented Switcher (Diary | My Recipes | Recipe's)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, "Diary", "tab_daily_diary"),
                    Triple(1, "My Recipes", "tab_my_recipes"),
                    Triple(2, "Recipe's", "tab_food_database")
                ).forEach { (tabIdx, title, tag) ->
                    val selected = activeSubTab == tabIdx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) FreshGreenPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeSubTab = tabIdx }
                            .testTag(tag)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        when (activeSubTab) {
            0 -> {
                val totalKcal = dayEntries.sumOf { it.calories }
                val totalP = dayEntries.sumOf { it.protein }
                val totalC = dayEntries.sumOf { it.carbohydrates }
                val totalF = dayEntries.sumOf { it.fat }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Date Strip + Manual Add Button
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dateOptions.forEach { (dateKey, label) ->
                                    val selected = selectedDateKey == dateKey
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) FreshGreenPrimary else MaterialTheme.colorScheme.surface,
                                        border = if (selected) null else BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectDate(dateKey) }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    manualDefaultMeal = "Lunch"
                                    showManualDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("add_manual_food_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Compact Daily Summary Strip
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Logged",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$totalKcal / ${profile.dailyCalorieGoal} kcal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "P ${totalP.roundToInt()}g • C ${totalC.roundToInt()}g • F ${totalF.roundToInt()}g",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshGreenPrimary
                                )
                            }
                        }
                    }

                    // Daily Macronutrient Split Pie Chart
                    item {
                        DailyMacroPieChartCard(
                            carbsG = totalC,
                            proteinG = totalP,
                            fatG = totalF,
                            targetCarbsG = profile.effectiveCarbsGoalG(),
                            targetProteinG = profile.effectiveProteinGoalG(),
                            targetFatG = profile.effectiveFatGoalG()
                        )
                    }

                    // 4 Clean Meal Cards
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { mealSection ->
                        val sectionEntries = dayEntries.filter { it.mealType == mealSection }
                        val sectionKcal = sectionEntries.sumOf { it.calories }
                        val (mealIcon, mealTint) = when (mealSection) {
                            "Breakfast" -> Icons.Rounded.FreeBreakfast to Color(0xFFF59E0B)
                            "Lunch" -> Icons.Rounded.LunchDining to FreshGreenPrimary
                            "Dinner" -> Icons.Rounded.DinnerDining to Color(0xFF6366F1)
                            else -> Icons.Rounded.Icecream to Color(0xFFEC4899)
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
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
                                                shape = RoundedCornerShape(12.dp),
                                                color = mealTint.copy(alpha = 0.14f),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = mealIcon,
                                                        contentDescription = null,
                                                        tint = mealTint,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = mealSection,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = if (sectionEntries.isEmpty()) "0 kcal" else "${sectionEntries.size} item(s) • $sectionKcal kcal",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (sectionEntries.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { mealGroupPendingSave = mealSection to sectionEntries },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("save_meal_section_recipe_$mealSection")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.BookmarkAdd,
                                                        contentDescription = "Save $mealSection to My Recipes",
                                                        tint = FreshGreenPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    manualDefaultMeal = mealSection
                                                    showManualDialog = true
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = FreshGreenPrimary.copy(alpha = 0.14f),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Add,
                                                            contentDescription = "Add food to $mealSection",
                                                            tint = FreshGreenPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (sectionEntries.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        sectionEntries.forEachIndexed { idx, entry ->
                                            if (idx > 0) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = entry.foodName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${entry.quantity.roundToInt()}g • P ${entry.protein.roundToInt()}g • C ${entry.carbohydrates.roundToInt()}g • F ${entry.fat.roundToInt()}g",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Text(
                                                    text = "${entry.calories} kcal",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = FreshGreenPrimary
                                                )

                                                IconButton(
                                                    onClick = { editingDiaryEntry = entry },
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = "Edit portion",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteDiaryEntry(entry.id) },
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Delete entry",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                MyRecipesSection(
                    savedRecipes = savedRecipes,
                    language = language,
                    onSaveRecipe = onSaveCustomRecipe,
                    onDeleteRecipe = onDeleteSavedRecipe,
                    onToggleFavorite = onToggleSavedRecipeFavorite,
                    onLogRecipeToDiary = onLogSavedRecipe
                )
            }

            else -> {
                // Clean Searchable Recipe's / Food Catalog Tab
                var searchQuery by remember { mutableStateOf("") }
                var selectedCategory by remember { mutableStateOf("All") }
                var recipesOnly by remember { mutableStateOf(false) }
                val filteredFoods = remember(searchQuery, selectedCategory, recipesOnly) {
                    FoodDatabaseCatalog.search(searchQuery, selectedCategory, recipesOnly = recipesOnly)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("food_db_search_clear_button")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        placeholder = { Text("Search food or recipes...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("food_db_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = recipesOnly,
                            onClick = { recipesOnly = !recipesOnly },
                            label = { Text("With Steps") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("food_db_recipes_chip")
                        )

                        FoodDatabaseCatalog.categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val displayLabel = cat.removeSuffix(" Foods")
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(displayLabel) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FreshGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredFoods, key = { it.id }) { food ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (food.hasRecipe) {
                                            selectedDbFoodForRecipe = food
                                        } else {
                                            selectedDbFoodForAdd = food
                                        }
                                    }
                                    .testTag("db_food_card_${food.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Image(
                                        painter = painterResource(
                                            id = FoodDatabaseCatalog.foodImageResFor(food.nameEn, food.category)
                                        ),
                                        contentDescription = food.localizedName(language),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = food.localizedName(language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${food.calories} kcal • ${food.servingSizeG.roundToInt()}g • P ${food.protein.roundToInt()}g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedDbFoodForAdd = food },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("add_db_food_${food.id}")
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = FreshGreenPrimary.copy(alpha = 0.14f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = "Add ${food.nameEn}",
                                                    tint = FreshGreenPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Recipe Detail Dialog
    val recipeFood = selectedDbFoodForRecipe
    if (recipeFood != null) {
        AlertDialog(
            onDismissRequest = { selectedDbFoodForRecipe = null },
            title = { Text(recipeFood.localizedName(language), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${recipeFood.calories} kcal • ${recipeFood.servingLabel} • P ${recipeFood.protein}g • C ${recipeFood.carbohydrates}g • F ${recipeFood.fat}g",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FreshGreenPrimary
                    )
                    if (recipeFood.ingredients.isNotEmpty()) {
                        Text("Ingredients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        recipeFood.ingredients.forEach { ing ->
                            Text("• $ing", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (recipeFood.recipeSteps.isNotEmpty()) {
                        Text("Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        recipeFood.recipeSteps.forEachIndexed { idx, step ->
                            Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDbFoodForAdd = recipeFood
                        selectedDbFoodForRecipe = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary)
                ) {
                    Text("Log to Diary", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDbFoodForRecipe = null }) { Text("Close") }
            }
        )
    }

    // Add from Catalog Dialog
    val dbFood = selectedDbFoodForAdd
    if (dbFood != null) {
        var portionG by remember(dbFood) { mutableStateOf(dbFood.servingSizeG.roundToInt().toString()) }
        var targetMeal by remember { mutableStateOf("Lunch") }

        AlertDialog(
            onDismissRequest = { selectedDbFoodForAdd = null },
            title = { Text(dbFood.localizedName(language), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${dbFood.calories} kcal per ${dbFood.servingSizeG.roundToInt()}g",
                        style = MaterialTheme.typography.labelMedium,
                        color = FreshGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = portionG,
                        onValueChange = { portionG = it },
                        label = { Text("Portion (grams)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { m ->
                            FilterChip(
                                selected = targetMeal == m,
                                onClick = { targetMeal = m },
                                label = { Text(m) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = portionG.toDoubleOrNull() ?: dbFood.servingSizeG
                        onAddDatabaseFood(dbFood, g, targetMeal)
                        selectedDbFoodForAdd = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary)
                ) {
                    Text("Add to $targetMeal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDbFoodForAdd = null }) { Text("Cancel") }
            }
        )
    }

    // Edit Diary Entry Dialog
    val entryToEdit = editingDiaryEntry
    if (entryToEdit != null) {
        var newName by remember(entryToEdit) { mutableStateOf(entryToEdit.foodName) }
        var newGrams by remember(entryToEdit) { mutableStateOf(entryToEdit.quantity.roundToInt().toString()) }

        AlertDialog(
            onDismissRequest = { editingDiaryEntry = null },
            title = { Text("Edit Entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Food Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGrams,
                        onValueChange = { newGrams = it },
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
                        val g = newGrams.toDoubleOrNull() ?: entryToEdit.quantity
                        onUpdateDiaryEntry(entryToEdit, newName, g)
                        editingDiaryEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDiaryEntry = null }) { Text("Cancel") }
            }
        )
    }

    // Manual Add Food Dialog
    if (showManualDialog) {
        var foodName by remember { mutableStateOf("") }
        var gramsStr by remember { mutableStateOf("150") }
        var caloriesStr by remember { mutableStateOf("220") }
        var proteinStr by remember { mutableStateOf("8") }
        var carbsStr by remember { mutableStateOf("30") }
        var fatStr by remember { mutableStateOf("6") }
        var mealType by remember(manualDefaultMeal) { mutableStateOf(manualDefaultMeal) }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Add Food", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gramsStr,
                            onValueChange = { gramsStr = it },
                            label = { Text("Grams") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = caloriesStr,
                            onValueChange = { caloriesStr = it },
                            label = { Text("Calories") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proteinStr,
                            onValueChange = { proteinStr = it },
                            label = { Text("Protein") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carbsStr,
                            onValueChange = { carbsStr = it },
                            label = { Text("Carbs") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatStr,
                            onValueChange = { fatStr = it },
                            label = { Text("Fat") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { m ->
                            FilterChip(
                                selected = mealType == m,
                                onClick = { mealType = m },
                                label = { Text(m) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddManualFood(
                            FoodDiaryEntry(
                                foodName = foodName.ifBlank { "Custom Meal" },
                                quantity = gramsStr.toDoubleOrNull() ?: 150.0,
                                calories = caloriesStr.toIntOrNull() ?: 200,
                                protein = proteinStr.toDoubleOrNull() ?: 6.0,
                                carbohydrates = carbsStr.toDoubleOrNull() ?: 28.0,
                                fat = fatStr.toDoubleOrNull() ?: 5.0,
                                fiber = 2.0,
                                sugar = 2.0,
                                mealType = mealType
                            )
                        )
                        showManualDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Save Meal Group to My Recipes Dialog
    val pendingGroup = mealGroupPendingSave
    if (pendingGroup != null) {
        val (slotName, slotEntries) = pendingGroup
        var comboTitle by remember(slotName, slotEntries) {
            mutableStateOf(
                if (slotEntries.size == 1) slotEntries.first().foodName else "My $slotName Combo"
            )
        }
        AlertDialog(
            onDismissRequest = { mealGroupPendingSave = null },
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
                        .testTag("save_meal_group_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveDiaryGroupAsRecipe(
                            comboTitle.ifBlank { "My $slotName Combo" },
                            slotName,
                            slotEntries
                        )
                        mealGroupPendingSave = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                    modifier = Modifier.testTag("confirm_save_meal_group_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mealGroupPendingSave = null }) { Text("Cancel") }
            }
        )
    }
}
