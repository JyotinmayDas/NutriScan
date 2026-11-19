package com.example.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.MockupCarbsBg
import com.example.ui.theme.MockupCarbsGreen
import com.example.ui.theme.MockupFatAmber
import com.example.ui.theme.MockupFatBg
import com.example.ui.theme.MockupSugarBg
import com.example.ui.theme.MockupSugarCoral
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Petal data model for the Flower Petal Nutrition Chart matching the design mockup.
 */
data class FlowerPetalItem(
    val name: String,
    val percentage: Double,
    val grams: Double,
    val carbsG: Double,
    val fatG: Double,
    val proteinG: Double,
    val gradientColors: List<Color>,
    val imageResId: Int? = null
)

/**
 * Interactive Flower Petal Nutrition & Ingredients Breakdown Screen Component (Mockup Screen 3).
 * Visualizes meal ingredients and macros as soft organic flower petals around a central food plate.
 */
@Composable
fun FlowerPetalNutritionCard(
    mealTitle: String = "Salmon Bowl",
    totalGrams: Double = 350.0,
    centerImageResId: Int = R.drawable.img_food_salmon_bowl_1794471753461,
    petals: List<FlowerPetalItem> = defaultSalmonBowlPetals(),
    onBackClick: (() -> Unit)? = null,
    onLogMeal: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(false) }
    var selectedPetalIndex by remember { mutableStateOf<Int?>(null) }
    var animateChart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateChart = true
    }

    val chartScale by animateFloatAsState(
        targetValue = if (animateChart) 1f else 0.7f,
        animationSpec = tween(durationMillis = 650),
        label = "petal_chart_scale"
    )

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("flower_petal_nutrition_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back Button (if provided) & Bookmark Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(44.dp))
                }

                Surface(
                    onClick = { isBookmarked = !isBookmarked },
                    shape = CircleShape,
                    color = if (isBookmarked) MockupFatAmber.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save recipe or meal",
                            tint = if (isBookmarked) MockupFatAmber else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The Flower Petal Radial Chart
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .testTag("radial_flower_chart")
            ) {
                val petalCount = petals.size.coerceAtLeast(1)
                val angleStep = (2 * Math.PI) / petalCount
                val radiusPx = 76.0 // Distance from center to petal center

                // Render Radial Flower Petals
                petals.forEachIndexed { index, petal ->
                    val angle = -Math.PI / 2 + (index * angleStep) // Start from top (Rice)
                    val offsetX = (radiusPx * cos(angle)).roundToInt()
                    val offsetY = (radiusPx * sin(angle)).roundToInt()
                    val isSelected = selectedPetalIndex == index

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(76.dp, 60.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = if (isSelected) listOf(petal.gradientColors.first().copy(alpha = 0.95f), petal.gradientColors.last())
                                    else petal.gradientColors
                                )
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MockupCarbsGreen else Color.White.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .clickable {
                                selectedPetalIndex = if (selectedPetalIndex == index) null else index
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${petal.percentage}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                            Text(
                                text = petal.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                ),
                                fontWeight = FontWeight.Medium,
                                color = DarkCharcoalText.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Central Dish Plate
                Surface(
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    color = Color.White,
                    border = BorderStroke(3.dp, Color.White),
                    modifier = Modifier.size(92.dp)
                ) {
                    Image(
                        painter = painterResource(id = centerImageResId),
                        contentDescription = mealTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meal Title & Gram Weight (Matching Mockup Screen 3: "Salmon Bowl - 350 g")
            Text(
                text = mealTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                ),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${totalGrams.roundToInt()} g",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Ingredients Breakdown Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                petals.forEach { item ->
                    IngredientBreakdownRow(item = item)
                }
            }

            if (onLogMeal != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onLogMeal,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("log_meal_breakdown_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Meal to Today's Diary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientBreakdownRow(item: FlowerPetalItem) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Circular thumbnail + Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    color = Color.White,
                    modifier = Modifier.size(42.dp)
                ) {
                    if (item.imageResId != null) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(item.gradientColors.first().copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = item.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: Gram weight and colored macro chips
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.grams.roundToInt()} g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Carbs (Green sprout)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = "Carbs",
                            tint = MockupCarbsGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.carbsG}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Fat (Amber droplet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InvertColors,
                            contentDescription = "Fat",
                            tint = MockupFatAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.fatG}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Protein (Coral cube)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Protein",
                            tint = MockupSugarCoral,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.proteinG}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Default sample data matching Screen 3 ("Salmon Bowl") from the user's mockup.
 */
fun defaultSalmonBowlPetals(): List<FlowerPetalItem> = listOf(
    FlowerPetalItem(
        name = "Rice",
        percentage = 42.9,
        grams = 150.0,
        carbsG = 33.0,
        fatG = 0.3,
        proteinG = 3.2,
        gradientColors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)),
        imageResId = R.drawable.img_food_biryani_rice_grains_1793802387127
    ),
    FlowerPetalItem(
        name = "Salmon",
        percentage = 28.6,
        grams = 100.0,
        carbsG = 0.0,
        fatG = 13.0,
        proteinG = 22.0,
        gradientColors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)),
        imageResId = R.drawable.img_food_salmon_bowl_1794471753461
    ),
    FlowerPetalItem(
        name = "Lettuce",
        percentage = 4.2,
        grams = 30.0,
        carbsG = 0.9,
        fatG = 0.1,
        proteinG = 0.4,
        gradientColors = listOf(Color(0xFFFEFCE8), Color(0xFFFEF9C3)),
        imageResId = R.drawable.img_food_dal_paneer_veg_1793802388466
    ),
    FlowerPetalItem(
        name = "Sesame",
        percentage = 5.7,
        grams = 20.0,
        carbsG = 2.4,
        fatG = 9.8,
        proteinG = 3.5,
        gradientColors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
        imageResId = R.drawable.img_food_sweets_dairy_fruits_1793802390207
    ),
    FlowerPetalItem(
        name = "Spinach",
        percentage = 4.4,
        grams = 25.0,
        carbsG = 0.9,
        fatG = 0.1,
        proteinG = 0.7,
        gradientColors = listOf(Color(0xFFF0FDF4), Color(0xFFBBF7D0)),
        imageResId = R.drawable.img_hero_nutrition_bowl_1793693566593
    ),
    FlowerPetalItem(
        name = "Cucumber",
        percentage = 14.3,
        grams = 50.0,
        carbsG = 1.8,
        fatG = 0.1,
        proteinG = 0.3,
        gradientColors = listOf(Color(0xFFECFDF5), Color(0xFFA7F3D0)),
        imageResId = R.drawable.img_hero_nutrition_bowl_1793693566593
    )
)
