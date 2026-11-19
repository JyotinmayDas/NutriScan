package com.example.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelGreenCard

data class NutritionStoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val badgeText: String? = null,
    val storyHeadline: String,
    val storyDescription: String,
    val statValue: String,
    val statLabel: String,
    val actionText: String,
    val onAction: () -> Unit
)

/**
 * Instagram-Style 'Stories Tray' for Daily Nutrition Moments.
 * Renders glowing green-gradient ringed circular avatars with quick story previews.
 */
@Composable
fun InstagramStoriesTray(
    profile: UserProfile,
    todayEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    remainingCalories: Int,
    onNavigateToScan: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onNavigateToDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeStory by remember { mutableStateOf<NutritionStoryItem?>(null) }

    val consumedCalories = remember(todayEntries) { todayEntries.sumOf { it.calories } }
    val consumedWater = remember(waterRecords) { waterRecords.sumOf { it.quantity } }
    val consumedProtein = remember(todayEntries) { todayEntries.sumOf { it.protein }.toInt() }

    val stories = remember(profile, todayEntries, waterRecords, remainingCalories) {
        listOf(
            NutritionStoryItem(
                id = "story_scan",
                title = "Scan Meal",
                subtitle = "Camera AI",
                icon = Icons.Default.CameraAlt,
                gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF84CC16)),
                badgeText = "+XP",
                storyHeadline = "Instant Food AI Lens",
                storyDescription = "Snap a photo of any food plate or beverage to get instant calorie & macronutrient estimation.",
                statValue = "${profile.dailyScansUsed} Scans",
                statLabel = "Used Today",
                actionText = "Open AI Camera Lens",
                onAction = onNavigateToScan
            ),
            NutritionStoryItem(
                id = "story_calories",
                title = "Calories",
                subtitle = "$remainingCalories Left",
                icon = Icons.Default.LocalFireDepartment,
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF4ADE80), Color(0xFFA3E635)),
                badgeText = if (remainingCalories > 0) "${remainingCalories}k" else "Goal",
                storyHeadline = "Daily Calorie Budget",
                storyDescription = "You have consumed $consumedCalories kcal out of your ${profile.dailyCalorieGoal} kcal daily target.",
                statValue = "$remainingCalories kcal",
                statLabel = "Remaining Budget",
                actionText = "Log Food to Diary",
                onAction = onNavigateToDiary
            ),
            NutritionStoryItem(
                id = "story_hydration",
                title = "Hydration",
                subtitle = "${consumedWater}ml",
                icon = Icons.Default.Opacity,
                gradientColors = listOf(Color(0xFF0284C7), Color(0xFF06B6D4), Color(0xFF10B981)),
                badgeText = "${(consumedWater * 100 / profile.dailyWaterGoalMl.coerceAtLeast(1))}%",
                storyHeadline = "Water Intake Progress",
                storyDescription = "Stay hydrated for peak cellular health, metabolic burn, and optimal satiety throughout the day.",
                statValue = "$consumedWater / ${profile.dailyWaterGoalMl} ml",
                statLabel = "Logged Today",
                actionText = "Track Water Intake",
                onAction = onNavigateToWater
            ),
            NutritionStoryItem(
                id = "story_protein",
                title = "Protein",
                subtitle = "${consumedProtein}g",
                icon = Icons.Default.FitnessCenter,
                gradientColors = listOf(Color(0xFF047857), Color(0xFF10B981), Color(0xFF6EE7B7)),
                badgeText = "${consumedProtein}g",
                storyHeadline = "Muscle & Recovery Fuel",
                storyDescription = "Targeting ${profile.effectiveProteinGoalG()}g daily protein helps maintain lean mass and curtails evening cravings.",
                statValue = "$consumedProtein / ${profile.effectiveProteinGoalG()}g",
                statLabel = "Daily Target",
                actionText = "View High-Protein Ideas",
                onAction = onNavigateToCoach
            ),
            NutritionStoryItem(
                id = "story_coach",
                title = "AI Coach",
                subtitle = "Ask Meal AI",
                icon = Icons.Default.AutoAwesome,
                gradientColors = listOf(Color(0xFF84CC16), Color(0xFF10B981), Color(0xFF047857)),
                badgeText = "24/7",
                storyHeadline = "Personalized Nutrition Guide",
                storyDescription = "Ask Gemini anything about Bengali traditional diets, macro balance, healthy swaps, or cooking tips.",
                statValue = "Gemini 3.8",
                statLabel = "Active Intelligence",
                actionText = "Chat with AI Nutritionist",
                onAction = onNavigateToCoach
            ),
            NutritionStoryItem(
                id = "story_recipes",
                title = "Smart Meals",
                subtitle = "Tailored",
                icon = Icons.Default.RestaurantMenu,
                gradientColors = listOf(Color(0xFF059669), Color(0xFF34D399), Color(0xFFA3E635)),
                badgeText = "Chef",
                storyHeadline = "Healthy Recipe Recommendations",
                storyDescription = "Wholesome meals customized to fit your exact remaining calories and dietary preferences.",
                statValue = "3 Ideas",
                statLabel = "Ready to Cook",
                actionText = "View Healthy Recipes",
                onAction = onNavigateToDiary
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("instagram_stories_tray")
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            items(stories, key = { it.id }) { story ->
                InstagramStoryCircle(
                    story = story,
                    onClick = { activeStory = story }
                )
            }
        }
    }

    activeStory?.let { story ->
        InstagramStoryDetailModal(
            story = story,
            onDismiss = { activeStory = null }
        )
    }
}

/**
 * Individual Instagram-style circular story bubble with glowing green-gradient ring.
 */
@Composable
private fun InstagramStoryCircle(
    story: NutritionStoryItem,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.sweepGradient(story.gradientColors)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("story_circle_${story.id}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp)
        ) {
            // Gradient Ring Border (Instagram Style)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(gradientBrush)
            )

            // Inner White/Surface Gap
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )

            // Avatar / Icon Center
            Surface(
                shape = CircleShape,
                color = PastelGreenCard,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = story.icon,
                        contentDescription = story.title,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Small Badge Dot / Count (e.g. "+XP", "Live")
            story.badgeText?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF064E3B),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Text(
            text = story.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(66.dp)
        )
    }
}

/**
 * Fullscreen / Sheet Dialog showing the Instagram-style story detail card with green theme.
 */
@Composable
private fun InstagramStoryDetailModal(
    story: NutritionStoryItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF064E3B),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .testTag("instagram_story_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Story Progress Bar at top (Instagram Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { 1f },
                        color = LimeAccent,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }

                // Story Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LimeAccent,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = story.icon,
                                    contentDescription = null,
                                    tint = DarkCharcoalText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "NutriScan Story",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = story.subtitle,
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Story",
                            tint = Color.White
                        )
                    }
                }

                // Visual Hero Card in Story
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF042F2C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = story.storyHeadline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = LimeAccent
                        )
                        Text(
                            text = story.storyDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = story.statLabel,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = story.statValue,
                                    color = LimeAccent,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Primary Action Button in Story
                Button(
                    onClick = {
                        onDismiss()
                        story.onAction()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = DarkCharcoalText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("story_action_button")
                ) {
                    Text(
                        text = story.actionText,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
