package com.example.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDiaryEntry
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.MockupCarbsGreen
import com.example.ui.theme.MockupFatAmber
import com.example.ui.theme.MockupSugarCoral
import kotlin.math.roundToInt

/**
 * Meal card item model matching the nutrition breakdown design.
 */
data class MockupScanItem(
    val id: String,
    val title: String,
    val grams: Double,
    val imageResId: Int,
    val carbsPercent: String,
    val fatPercent: String,
    val sugarPercent: String,
    val carbsG: Double,
    val fatG: Double,
    val proteinG: Double,
    val calories: Int
)

/**
 * "Last scans" Horizontal Carousel component showing only the user's real recorded scans,
 * or a zero-state prompt with a direct AI Food Scanner button when no scans exist yet.
 */
@Composable
fun MockupMealCardCarousel(
    onSelectMeal: (MockupScanItem) -> Unit,
    onSeeAllClick: () -> Unit,
    onScanClick: () -> Unit = {},
    recentEntries: List<FoodDiaryEntry> = emptyList(),
    modifier: Modifier = Modifier
) {
    val realScans = rememberMockupScans(recentEntries)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header Row: "Last scans" & "See all"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last scans",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (realScans.isEmpty()) "0 scans" else "See all",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (realScans.isEmpty()) {
            // Zero-State Card (No fake/sample meals shown before the user scans or logs)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("last_scans_empty_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "No meal scans yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your scan history starts at zero. Scan your first meal to track calories & macros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onScanClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FreshGreenPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("carousel_empty_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Food",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Horizontal Carousel of real user scans
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("last_scans_mockup_carousel")
            ) {
                itemsIndexed(realScans) { index, item ->
                    MockupMealCard(
                        item = item,
                        onClick = { onSelectMeal(item) },
                        modifier = Modifier.testTag("mockup_meal_card_$index")
                    )
                }
            }
        }
    }
}

@Composable
fun MockupMealCard(
    item: MockupScanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier
            .width(210.dp)
            .height(256.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Food Dish Plate
            Surface(
                shape = CircleShape,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.size(116.dp)
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Food Name & Calories
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.calories} kcal • ${item.grams.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Macro Stats in 3 neat columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Carbs
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = "Carbs",
                        tint = MockupCarbsGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.carbsPercent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 2. Fat
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.InvertColors,
                        contentDescription = "Fat",
                        tint = MockupFatAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.fatPercent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3. Protein
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Protein",
                        tint = MockupSugarCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.sugarPercent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberMockupScans(recentEntries: List<FoodDiaryEntry>): List<MockupScanItem> {
    if (recentEntries.isEmpty()) return emptyList()

    return recentEntries.take(5).mapIndexed { idx, entry ->
        val totalMacro = (entry.carbohydrates + entry.fat + entry.protein).coerceAtLeast(1.0)
        val cPct = "${((entry.carbohydrates / totalMacro) * 100).roundToInt()}%"
        val fPct = "${((entry.fat / totalMacro) * 100).roundToInt()}%"
        val pPct = "${((entry.protein / totalMacro) * 100).roundToInt()}%"

        MockupScanItem(
            id = entry.id.ifBlank { "scan_$idx" },
            title = entry.foodName,
            grams = entry.quantity,
            imageResId = FoodDatabaseCatalog.foodImageResFor(entry.foodName, entry.mealType),
            carbsPercent = cPct,
            fatPercent = fPct,
            sugarPercent = pPct,
            carbsG = entry.carbohydrates,
            fatG = entry.fat,
            proteinG = entry.protein,
            calories = entry.calories
        )
    }
}
