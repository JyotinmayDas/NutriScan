package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndSettingsScreen(
    profile: UserProfile,
    isDarkMode: Boolean,
    @Suppress("UNUSED_PARAMETER") remainingScansToday: Int = 5,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onTogglePremiumPlan: (Boolean) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onGrantBonusScan: () -> Unit = {},
    onOpenEditProfileDialog: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit = {},
    onDeleteAccountData: () -> Unit,
    onSignOut: () -> Unit
) {
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Inline editable profile fields (Name, Age, Height, Current Weight, Target Weight, Gender, Activity, Goal, Diet, Water)
    var nameInput by remember(profile) { mutableStateOf(profile.name) }
    var ageInput by remember(profile) { mutableStateOf(profile.age.toString()) }
    var heightInput by remember(profile) { mutableStateOf(profile.height.toInt().toString()) }
    var currentWeightInput by remember(profile) { mutableStateOf(profile.currentWeight.toString()) }
    var targetWeightInput by remember(profile) { mutableStateOf(profile.targetWeight.toString()) }
    var selectedGender by remember(profile) { mutableStateOf(profile.gender) }
    var selectedActivity by remember(profile) { mutableStateOf(profile.activityLevel) }
    var selectedGoal by remember(profile) { mutableStateOf(profile.fitnessGoal) }
    var selectedDiet by remember(profile) { mutableStateOf(profile.dietaryPreference) }
    var waterGoalInput by remember(profile) { mutableStateOf(profile.dailyWaterGoalMl.toString()) }

    val parsedAge = (ageInput.toIntOrNull() ?: profile.age).coerceIn(10, 120)
    val parsedHeightCm = (heightInput.toDoubleOrNull() ?: profile.height).coerceIn(80.0, 250.0)
    val parsedWeightKg = (currentWeightInput.toDoubleOrNull() ?: profile.currentWeight).coerceIn(20.0, 300.0)
    val parsedTargetWeightKg = (targetWeightInput.toDoubleOrNull() ?: profile.targetWeight).coerceIn(20.0, 300.0)
    val parsedWaterGoalMl = (waterGoalInput.toIntOrNull() ?: profile.dailyWaterGoalMl).coerceIn(500, 10000)

    val heightMeters = (parsedHeightCm / 100.0).coerceAtLeast(0.5)
    val bmiValue = ((parsedWeightKg / (heightMeters * heightMeters)) * 10.0).roundToInt() / 10.0
    val bmiCategory = when {
        bmiValue < 18.5 -> "Underweight"
        bmiValue < 25.0 -> "Normal"
        bmiValue < 30.0 -> "Overweight"
        else -> "Obese"
    }

    val calculatedDailyCalories = UserProfile.calculateDailyCalorieTarget(
        age = parsedAge,
        heightCm = parsedHeightCm,
        weightKg = parsedWeightKg,
        gender = selectedGender,
        activityLevel = selectedActivity,
        fitnessGoal = selectedGoal
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Summary Hero Card (Name, Email, Age, Height, Weight, Target Weight, Daily Calories, Hydration)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFD9F99D),
                                Color(0xFFDCFCE7)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
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
                                color = Color.White,
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(2.dp, FreshGreenPrimary, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FreshGreenPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkCharcoalText
                                )
                                Text(
                                    text = "${profile.age} yrs • ${profile.gender} • ${profile.height.toInt()} cm",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkCharcoalText.copy(alpha = 0.8f)
                                )
                                if (profile.email.isNotBlank()) {
                                    Text(
                                        text = profile.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkCharcoalText.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onOpenEditProfileDialog,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCharcoalText,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 1: Age, Height, BMI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = "Age",
                            value = "${profile.age} yrs",
                            sub = profile.gender,
                            bg = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "Height",
                            value = "${profile.height.toInt()} cm",
                            sub = "BMI $bmiValue",
                            bg = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "BMI Status",
                            value = bmiCategory,
                            sub = profile.fitnessGoal,
                            bg = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Weight, Daily Calorie Target, Hydration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = "Weight",
                            value = "${profile.currentWeight} kg",
                            sub = "Target ${profile.targetWeight} kg",
                            bg = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "Daily Target",
                            value = "${profile.dailyCalorieGoal}",
                            sub = "kcal / day",
                            bg = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "Hydration",
                            value = "${profile.dailyWaterGoalMl}",
                            sub = "ml / day",
                            bg = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Personal Profile Details & Body Metrics Editor (Name, Age, Weight, Height, Goal, Activity, Diet)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelGreenCard,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Personal Profile & Body Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Update your name, age, height, weight, and nutrition goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Age (years)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_age_input")
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_height_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = currentWeightInput,
                            onValueChange = { currentWeightInput = it },
                            label = { Text("Current Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_weight_input")
                        )
                        OutlinedTextField(
                            value = targetWeightInput,
                            onValueChange = { targetWeightInput = it },
                            label = { Text("Target Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_target_weight_input")
                        )
                    }

                    OutlinedTextField(
                        value = waterGoalInput,
                        onValueChange = { waterGoalInput = it },
                        label = { Text("Daily Water Goal (ml)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_water_goal_input")
                    )

                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = selectedGender == g,
                                onClick = { selectedGender = g },
                                label = { Text(g) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkCharcoalText,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = "Fitness Goal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lose Weight", "Maintain Weight", "Gain Weight").forEach { goal ->
                            FilterChip(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal },
                                label = { Text(goal) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FreshGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = "Activity Level",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active").forEach { act ->
                            FilterChip(
                                selected = selectedActivity == act,
                                onClick = { selectedActivity = act },
                                label = { Text(act) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkCharcoalText,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = "Dietary Preference",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Bengali Traditional",
                            "Indian Vegetarian",
                            "Non-Vegetarian",
                            "Vegan",
                            "High-Protein Indian"
                        ).forEach { diet ->
                            FilterChip(
                                selected = selectedDiet == diet,
                                onClick = { selectedDiet = diet },
                                label = { Text(diet) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkCharcoalText,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PastelGreenCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Estimated Daily Calorie Requirement",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DarkCharcoalText.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "$calculatedDailyCalories kcal / day",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkCharcoalText
                                )
                            }
                            Text(
                                text = "BMI $bmiValue ($bmiCategory)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FreshGreenPrimary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val updatedProfile = profile.copy(
                                name = nameInput.ifBlank { profile.name },
                                age = parsedAge,
                                height = parsedHeightCm,
                                currentWeight = parsedWeightKg,
                                targetWeight = parsedTargetWeightKg,
                                gender = selectedGender,
                                activityLevel = selectedActivity,
                                fitnessGoal = selectedGoal,
                                dietaryPreference = selectedDiet,
                                dailyWaterGoalMl = parsedWaterGoalMl,
                                dailyCalorieGoal = calculatedDailyCalories
                            )
                            onSaveProfile(updatedProfile)
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = DarkCharcoalText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_details_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Details", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 3. Language & Localization Settings (English, Bengali, Hindi)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelBlueCard,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = FreshGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "App Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "en" to "English",
                            "bn" to "বাংলা (Bengali)",
                            "hi" to "हिन्दी (Hindi)"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = profile.language == code,
                                onClick = { onSelectLanguage(code) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkCharcoalText,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("settings_lang_$code")
                            )
                        }
                    }
                }
            }
        }

        // 4. Appearance / Dark Mode
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelPeachCard,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = DarkCharcoalText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Dark Mode Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Switch between Soft Sage Light and Deep Emerald Dark",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // 5. Privacy Policy, Terms & Account Actions
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Policy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy, Terms & Medical Disclaimer")
                    }

                    Button(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkCharcoalText,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Profile Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy, Security & Medical Disclaimer") },
            text = {
                Text(
                    "1. Medical Disclaimer: All calorie targets (Mifflin-St Jeor formula) and visual meal scans are approximate educational estimates and NOT medical advice.\n\n" +
                        "2. Data Privacy: Your food diary, weight history, and water logs are isolated under your authenticated Google account in Cloud Firestore with zero-trust security rules.\n\n" +
                        "3. Image Processing: Meal photos are compressed and analyzed securely via encrypted cloud nutrition services."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Profile Data?") },
            text = {
                Text("This will permanently delete your profile document and sign you out of NutriScan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete & Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    sub: String,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkCharcoalText.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DarkCharcoalText
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = FreshGreenPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
