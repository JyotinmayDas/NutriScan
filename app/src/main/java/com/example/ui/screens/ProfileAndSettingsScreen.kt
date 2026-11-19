package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.AppLanguage
import com.example.core.AppLanguageManager
import com.example.model.UserProfile
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.ThemePreferenceManager
import com.example.widgets.SetDailyNutritionGoalsDialog
import kotlin.math.roundToInt

@Composable
fun ProfileAndSettingsScreen(
    profile: UserProfile,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onOpenEditProfileDialog: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onDeleteAccountData: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentThemeMode by ThemePreferenceManager.themeMode.collectAsStateWithLifecycle()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMacroGoalsDialog by remember { mutableStateOf(false) }

    val heightMeters = (profile.height.coerceIn(80.0, 250.0) / 100.0).coerceAtLeast(0.5)
    val bmiValue = ((profile.currentWeight / (heightMeters * heightMeters)) * 10.0).roundToInt() / 10.0
    val isGuest = profile.userId == "guest_local_user"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. Clean Profile Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                shape = CircleShape,
                                color = FreshGreenPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = profile.name.take(1).uppercase().ifBlank { "U" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FreshGreenPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = profile.name.ifBlank { "User" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isGuest) "Guest Session" else profile.email.ifBlank { "${profile.age} yrs • ${profile.gender}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenEditProfileDialog,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CleanMetricTile(
                            label = "Weight",
                            value = "${profile.currentWeight} kg",
                            modifier = Modifier.weight(1f)
                        )
                        CleanMetricTile(
                            label = "BMI",
                            value = "$bmiValue",
                            modifier = Modifier.weight(1f)
                        )
                        CleanMetricTile(
                            label = "Goal",
                            value = "${profile.dailyCalorieGoal} kcal",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Guest Sign-In Prompt
        if (isGuest) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = FreshGreenPrimary.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, FreshGreenPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Back up your meals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Sign in with Google to sync your food diary and saved recipes across devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshGreenPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("guest_sign_in_upgrade_button")
                        ) {
                            Text("Sign In with Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Daily Nutrition Targets
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Daily Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Protein ${profile.effectiveProteinGoalG()}g • Carbs ${profile.effectiveCarbsGoalG()}g • Fat ${profile.effectiveFatGoalG()}g • Water ${profile.dailyWaterGoalMl}ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showMacroGoalsDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_customize_macro_goals_button")
                    ) {
                        Text("Customize Targets", fontWeight = FontWeight.Bold, color = FreshGreenPrimary)
                    }
                }
            }
        }

        // 3. Appearance & Language Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AppThemeMode.LIGHT to "Light",
                                AppThemeMode.DARK to "Dark",
                                AppThemeMode.SYSTEM to "System"
                            ).forEach { (mode, label) ->
                                val selected = currentThemeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        ThemePreferenceManager.setThemeMode(context, mode)
                                        if (mode != AppThemeMode.SYSTEM) {
                                            onToggleDarkMode(mode == AppThemeMode.DARK)
                                        }
                                    },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FreshGreenPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Language",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val selected = profile.language.equals(lang.code, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        AppLanguageManager.setLanguage(context, lang.code)
                                        onSelectLanguage(lang.code)
                                    },
                                    label = { Text(lang.nativeName, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FreshGreenPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lang_option_${lang.code}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Privacy & Account Actions
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenPrivacyPolicy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_privacy_policy_screen_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Policy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGuest) "Exit Guest Mode" else "Sign Out",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Profile Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("profile_app_version_text")
            )
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Profile Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will delete your saved profile and sign you out.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMacroGoalsDialog) {
        SetDailyNutritionGoalsDialog(
            profile = profile,
            onDismiss = { showMacroGoalsDialog = false },
            onSaveGoals = { updatedProfile ->
                onSaveProfile(updatedProfile)
                showMacroGoalsDialog = false
            }
        )
    }
}

@Composable
private fun CleanMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
