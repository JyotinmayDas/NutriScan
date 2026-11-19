package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.GoogleSignInButton
import com.example.core.attemptAutoSignIn
import com.example.model.UserProfile
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccentStrong
import com.example.ui.theme.SageHeroCardEnd
import com.example.ui.theme.SageHeroCardStart

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }
    var selectedLanguage by remember { mutableStateOf("en") }
    var showPrivacyPolicyPage by remember { mutableStateOf(false) }

    if (showPrivacyPolicyPage) {
        PrivacyPolicyScreen(onBack = { showPrivacyPolicyPage = false })
        return
    }

    // Attempt silent auto sign-in once on launch
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SageHeroCardStart,
                        SageHeroCardEnd.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Brand Pill & Language Selector
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nutriscan_logo),
                            contentDescription = "NutriScan Logo",
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NutriScan",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("en" to "EN", "bn" to "বাংলা", "hi" to "हिन्दी").forEach { (code, label) ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selectedLanguage == code) DarkCharcoalText else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clickable { selectedLanguage = code }
                                .testTag("auth_lang_$code")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedLanguage == code) Color.White else DarkCharcoalText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NutriScan Brand Logo Hero Showcase
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                // Soft mint-lime halo circle matching the logo
                Box(
                    modifier = Modifier
                        .size(272.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                )

                // Official NutriScan Logo Artwork
                Image(
                    painter = painterResource(id = R.drawable.img_nutriscan_logo),
                    contentDescription = "NutriScan Official Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(252.dp)
                        .shadow(16.dp, RoundedCornerShape(44.dp))
                        .clip(RoundedCornerShape(44.dp))
                        .background(Color.White)
                        .border(4.dp, Color.White, RoundedCornerShape(44.dp))
                        .testTag("auth_brand_logo_image")
                )
            }
        }

        // Center Typography ("Scan Your Food. Know Your Nutrition.")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = when (selectedLanguage) {
                    "bn" -> "স্মার্ট খাবার,"
                    "hi" -> "स्मार्ट खाएं,"
                    else -> "Scan Your Food,"
                },
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = when (selectedLanguage) {
                    "bn" -> "সুস্থ জীবন"
                    "hi" -> "बेहतर जिएं"
                    else -> "Know Your Nutrition"
                },
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FreshGreenPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when (selectedLanguage) {
                    "bn" -> "আপনার সুস্বাস্থ্যের জন্য আধুনিক পুষ্টি ও ক্যালোরি ট্র্যাকার।"
                    "hi" -> "आपके बेहतर स्वास्थ्य के लिए स्मार्ट पोषण और कैलोरी ट्रैकर।"
                    else -> "Smart nutrition & calorie tracking\nfor a healthier lifestyle."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Pager Indicator Dots (•  —  •)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(FreshGreenPrimary.copy(alpha = 0.35f))
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FreshGreenPrimary)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(FreshGreenPrimary.copy(alpha = 0.35f))
                )
            }
        }

        // Bottom Sign-In Action & Disclaimer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (authError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = authError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Styled Pill Container wrapping the mandatory GoogleSignInButton
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = FreshGreenPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GoogleSignInButton(
                            label = when (selectedLanguage) {
                                "bn" -> "Google দিয়ে শুরু করুন"
                                "hi" -> "Google के साथ शुरू करें"
                                else -> "Get Started with Google"
                            },
                            onAuthSuccess = onAuthSuccess,
                            onAuthError = { msg -> authError = msg }
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = DarkCharcoalText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guest Mode Button for immediate local feature exploration
            OutlinedButton(
                onClick = onContinueAsGuest,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_as_guest_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = DarkCharcoalText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (selectedLanguage) {
                        "bn" -> "গেস্ট মোডে চালিয়ে যান (Guest Mode)"
                        "hi" -> "गेस्ट मोड में जारी रखें (Guest Mode)"
                        else -> "Continue as Guest (Try Features)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkCharcoalText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Guest Mode lets you try food scanning, database search, and calorie tracking locally. Sign in with Google anytime for cloud sync.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .clickable { showPrivacyPolicyPage = true }
                    .testTag("auth_privacy_policy_button")
            ) {
                Text(
                    text = "Privacy Policy & Terms of Use",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FreshGreenPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingProfileDialog(
    currentProfile: UserProfile,
    isInitialSetup: Boolean = false,
    onSaveProfile: (UserProfile) -> Unit,
    onSkip: () -> Unit
) {
    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var ageStr by remember(currentProfile) { mutableStateOf(currentProfile.age.toString()) }
    var heightStr by remember(currentProfile) { mutableStateOf(currentProfile.height.toInt().toString()) }
    var currentWeightStr by remember(currentProfile) { mutableStateOf(currentProfile.currentWeight.toString()) }
    var targetWeightStr by remember(currentProfile) { mutableStateOf(currentProfile.targetWeight.toString()) }
    var gender by remember(currentProfile) { mutableStateOf(currentProfile.gender) }
    var activityLevel by remember(currentProfile) { mutableStateOf(currentProfile.activityLevel) }
    var fitnessGoal by remember(currentProfile) { mutableStateOf(currentProfile.fitnessGoal) }
    var dietaryPreference by remember(currentProfile) { mutableStateOf(currentProfile.dietaryPreference) }
    var waterGoalStr by remember(currentProfile) { mutableStateOf(currentProfile.dailyWaterGoalMl.toString()) }

    val age = ageStr.toIntOrNull() ?: 28
    val height = heightStr.toDoubleOrNull() ?: 168.0
    val weight = currentWeightStr.toDoubleOrNull() ?: 68.0
    val targetWeight = targetWeightStr.toDoubleOrNull() ?: 62.0

    val estimatedCalories = UserProfile.calculateDailyCalorieTarget(
        age = age,
        heightCm = height,
        weightKg = weight,
        gender = gender,
        activityLevel = activityLevel,
        fitnessGoal = fitnessGoal
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isInitialSetup) "Create Your Profile" else "Personalize Your Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isInitialSetup) "Set your goals first so your history saves cleanly" else "Tailor daily calorie & macro goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_name_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = ageStr,
                onValueChange = { ageStr = it },
                label = { Text("Age (yrs)") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightStr,
                onValueChange = { heightStr = it },
                label = { Text("Height (cm)") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = currentWeightStr,
                onValueChange = { currentWeightStr = it },
                label = { Text("Current Weight (kg)") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = targetWeightStr,
                onValueChange = { targetWeightStr = it },
                label = { Text("Target Weight (kg)") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Fitness Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Lose Weight", "Maintain Weight", "Gain Weight").forEach { goal ->
                FilterChip(
                    selected = fitnessGoal == goal,
                    onClick = { fitnessGoal = goal },
                    label = { Text(goal) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FreshGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Text("Activity Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active").forEach { level ->
                FilterChip(
                    selected = activityLevel == level,
                    onClick = { activityLevel = level },
                    label = { Text(level) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        Text("Dietary Preference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Bengali Traditional", "Indian Vegetarian", "Non-Vegetarian", "Vegan", "Eggetarian").forEach { pref ->
                FilterChip(
                    selected = dietaryPreference == pref,
                    onClick = { dietaryPreference = pref },
                    label = { Text(pref) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        Text("Gender (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Female", "Male", "Prefer not to say").forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { gender = g },
                    label = { Text(g) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        OutlinedTextField(
            value = waterGoalStr,
            onValueChange = { waterGoalStr = it },
            label = { Text("Daily Water Goal (ml)") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SageHeroCardStart),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Daily Target: $estimatedCalories kcal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkCharcoalText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = FreshGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Calculated using the Mifflin-St Jeor equation. This is an estimate and not medical advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkCharcoalText.copy(alpha = 0.78f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("skip_onboarding_button")
            ) {
                Text("Skip for Now")
            }
            Button(
                onClick = {
                    onSaveProfile(
                        currentProfile.copy(
                            name = name.ifBlank { "NutriScan User" },
                            age = age,
                            height = height,
                            currentWeight = weight,
                            targetWeight = targetWeight,
                            gender = gender,
                            activityLevel = activityLevel,
                            fitnessGoal = fitnessGoal,
                            dietaryPreference = dietaryPreference,
                            dailyCalorieGoal = estimatedCalories,
                            dailyWaterGoalMl = (waterGoalStr.toIntOrNull() ?: 2500).coerceIn(500, 10000)
                        )
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(52.dp)
                    .testTag("save_onboarding_button")
            ) {
                Text(
                    text = if (isInitialSetup) "Create Profile" else "Save Goals",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
