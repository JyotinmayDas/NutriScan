package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.todayDateKey
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelGreenAccent
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.ProteinColor
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
    onCopyPreviousDayMeals: (String, String) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Daily Diary, 1 = Food Database
    var showManualDialog by remember { mutableStateOf(false) }
    var editingDiaryEntry by remember { mutableStateOf<FoodDiaryEntry?>(null) }
    var selectedDbFoodForAdd by remember { mutableStateOf<FoodDatabaseItem?>(null) }

    val dateOptions = remember { recentDateKeys() }
    val dayEntries = diaryEntries.filter { it.dateKey == selectedDateKey }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)
    ) {
        // Modern Segmented Pill Switcher (Daily Diary vs Food Database)
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (activeSubTab == 0) DarkCharcoalText else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { activeSubTab = 0 }
                        .testTag("tab_daily_diary")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Daily Food Diary",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSubTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (activeSubTab == 1) DarkCharcoalText else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { activeSubTab = 1 }
                        .testTag("tab_food_database")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Food Database (${FoodDatabaseCatalog.allFoods.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (activeSubTab == 0) {
            val totalKcal = dayEntries.sumOf { it.calories }
            val totalP = dayEntries.sumOf { it.protein }.roundToInt()
            val totalC = dayEntries.sumOf { it.carbohydrates }.roundToInt()
            val totalF = dayEntries.sumOf { it.fat }.roundToInt()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Date Switcher & Quick Manual / Copy Yesterday Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dateOptions.forEach { (dateKey, label) ->
                                val selected = selectedDateKey == dateKey
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) FreshGreenPrimary else MaterialTheme.colorScheme.surface,
                                    shadowElevation = if (selected) 2.dp else 0.dp,
                                    modifier = Modifier.clickable { onSelectDate(dateKey) }
                                ) {
                                    Text(
                                        text = "$label • $dateKey",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showManualDialog = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LimeAccent,
                                    contentColor = DarkCharcoalText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("add_manual_food_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add Food Manually", fontWeight = FontWeight.Bold)
                            }

                            val yesterdayKey = dateOptions.getOrNull(1)?.first ?: todayDateKey()
                            FilledTonalButton(
                                onClick = { onCopyPreviousDayMeals(yesterdayKey, selectedDateKey) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .height(46.dp)
                                    .testTag("copy_yesterday_meals_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Yesterday", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 2. Daily Intake Summary Card with 3 Macro Bars (Matching Uploaded Image 8 Center)
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily intake",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "$totalKcal / 2000 kcal",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshGreenPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (totalKcal / 2000f).coerceIn(0.05f, 1f) },
                                color = FreshGreenPrimary,
                                trackColor = Color(0xFFDCFCE7),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                MiniMacroColumn(
                                    label = "Carbs",
                                    valueText = "$totalC/250 g",
                                    progress = (totalC / 250f).coerceIn(0.06f, 1f),
                                    color = CarbsColor,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniMacroColumn(
                                    label = "Protein",
                                    valueText = "$totalP/120 g",
                                    progress = (totalP / 120f).coerceIn(0.06f, 1f),
                                    color = ProteinColor,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniMacroColumn(
                                    label = "Fat",
                                    valueText = "$totalF/65 g",
                                    progress = (totalF / 65f).coerceIn(0.06f, 1f),
                                    color = FatColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. Four Meal Sections: Breakfast, Lunch, Dinner, Snacks (Matching Image 1 Left & Image 8 Left)
                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { mealSection ->
                    val sectionEntries = dayEntries.filter { it.mealType == mealSection }
                    val sectionKcal = sectionEntries.sumOf { it.calories }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = PastelGreenCard,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Restaurant,
                                                    contentDescription = null,
                                                    tint = PastelGreenAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = mealSection,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "$sectionKcal kcal logged",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Circular + Add Button on right (Image 8 Left)
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDCFCE7),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { activeSubTab = 1 }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add food to $mealSection",
                                                tint = FreshGreenPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (sectionEntries.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    sectionEntries.forEach { entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.foodName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${entry.quantity.roundToInt()}g • ${entry.calories} kcal • P:${entry.protein.roundToInt()}g C:${entry.carbohydrates.roundToInt()}g F:${entry.fat.roundToInt()}g",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                IconButton(onClick = { editingDiaryEntry = entry }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit portion",
                                                        tint = FreshGreenPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(onClick = { onDeleteDiaryEntry(entry.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
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
        } else {
            // SEARCHABLE FOOD DATABASE & TRENDING RECIPES TAB (Matching Uploaded Image 9 Right & Image 8 Center)
            var searchQuery by remember { mutableStateOf("") }
            var selectedCategory by remember { mutableStateOf("All") }
            val filteredFoods = remember(searchQuery, selectedCategory) {
                FoodDatabaseCatalog.search(searchQuery, selectedCategory)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Bar + Filter Button Row (Matching Image 9 Right & Image 10 Left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search Rice, Macher Jhol, Biryani, Momo...") },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("food_db_search_input")
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter Categories",
                                tint = DarkCharcoalText
                            )
                        }
                    }
                }

                // Vertical Rounded-Rectangle Category Cards (Matching Uploaded Image 9 Right!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FoodDatabaseCatalog.categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF65A30D) else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier
                                .width(78.dp)
                                .height(84.dp)
                                .clickable { selectedCategory = cat }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (cat == "All") Icons.Default.GridView else Icons.Default.Eco,
                                    contentDescription = cat,
                                    tint = if (isSelected) Color.White else FreshGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Indian, Bengali & Global Foods",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${filteredFoods.size} dishes",
                        style = MaterialTheme.typography.labelMedium,
                        color = FreshGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFoods, key = { it.id }) { food ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular dish photo on left (Matching Image 8 Center)
                                Image(
                                    painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                                    contentDescription = food.localizedName(language),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = food.localizedName(language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${food.servingLabel} (${food.servingSizeG.roundToInt()}g) • ${food.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = PastelGreenCard
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF97316),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${food.calories} kcal",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkCharcoalText
                                                )
                                            }
                                        }
                                        Text(
                                            text = "P:${food.protein}g C:${food.carbohydrates}g F:${food.fat}g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Soft Green Circular + Button (Matching Image 8 Center)
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { selectedDbFoodForAdd = food }
                                        .testTag("add_db_food_${food.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add ${food.nameEn}",
                                            tint = FreshGreenPrimary
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

    // Add from Database Dialog (Styled like Image 8 Right Food Detail Card)
    val dbFood = selectedDbFoodForAdd
    if (dbFood != null) {
        var portionG by remember(dbFood) { mutableStateOf(dbFood.servingSizeG.roundToInt().toString()) }
        var targetMeal by remember { mutableStateOf("Lunch") }

        AlertDialog(
            onDismissRequest = { selectedDbFoodForAdd = null },
            title = {
                Text(
                    text = dbFood.localizedName(language),
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = portionG,
                        onValueChange = { portionG = it },
                        label = { Text("Portion Weight (grams)") },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Meal Section:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary)
                ) {
                    Text("Track in $targetMeal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDbFoodForAdd = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Existing Diary Entry Dialog
    val entryToEdit = editingDiaryEntry
    if (entryToEdit != null) {
        var newName by remember(entryToEdit) { mutableStateOf(entryToEdit.foodName) }
        var newGrams by remember(entryToEdit) { mutableStateOf(entryToEdit.quantity.roundToInt().toString()) }

        AlertDialog(
            onDismissRequest = { editingDiaryEntry = null },
            title = { Text("Edit Diary Portion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Food Name") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGrams,
                        onValueChange = { newGrams = it },
                        label = { Text("Portion Size (grams)") },
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
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDiaryEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Food Entry Dialog
    if (showManualDialog) {
        var foodName by remember { mutableStateOf("") }
        var gramsStr by remember { mutableStateOf("150") }
        var caloriesStr by remember { mutableStateOf("220") }
        var proteinStr by remember { mutableStateOf("8") }
        var carbsStr by remember { mutableStateOf("32") }
        var fatStr by remember { mutableStateOf("6") }
        var mealType by remember { mutableStateOf("Lunch") }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Add Custom Food Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food Name (English / বাংলা / हिन्दी)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gramsStr,
                            onValueChange = { gramsStr = it },
                            label = { Text("Grams (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = caloriesStr,
                            onValueChange = { caloriesStr = it },
                            label = { Text("Calories (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proteinStr,
                            onValueChange = { proteinStr = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carbsStr,
                            onValueChange = { carbsStr = it },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatStr,
                            onValueChange = { fatStr = it },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { m ->
                            FilterChip(
                                selected = mealType == m,
                                onClick = { mealType = m },
                                label = { Text(m) }
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
                                foodName = foodName.ifBlank { "Homemade Dish" },
                                quantity = gramsStr.toDoubleOrNull() ?: 150.0,
                                calories = caloriesStr.toIntOrNull() ?: 200,
                                protein = proteinStr.toDoubleOrNull() ?: 6.0,
                                carbohydrates = carbsStr.toDoubleOrNull() ?: 28.0,
                                fat = fatStr.toDoubleOrNull() ?: 5.0,
                                fiber = 2.5,
                                sugar = 2.0,
                                mealType = mealType
                            )
                        )
                        showManualDialog = false
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Save Food")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MiniMacroColumn(
    label: String,
    valueText: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = color.copy(alpha = 0.16f),
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
