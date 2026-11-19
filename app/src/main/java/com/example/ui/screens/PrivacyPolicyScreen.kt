package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.PastelGreenCard

private data class PolicySection(
    val number: String,
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList()
)

private val privacyPolicySections = listOf(
    PolicySection(
        number = "01",
        title = "Introduction & Overview",
        body = "Welcome to NutriScan. We are committed to protecting your personal privacy and securing your health and nutrition records. This Privacy Policy explains how we collect, use, store, and safeguard your information when you use the NutriScan mobile application."
    ),
    PolicySection(
        number = "02",
        title = "Information We Collect",
        body = "We collect only the minimum information required to provide personalized calorie and nutrition tracking:",
        bullets = listOf(
            "Account Information: When you sign in with Google Authentication, we receive your User ID, display name, and email address to synchronize your personal records securely across sessions.",
            "Body & Nutrition Profile Metrics: Information you voluntarily enter, including age, height, current weight, target weight, gender, activity level, dietary preference, fitness goal, and daily hydration target.",
            "Food Diary, Water & Weight Logs: Meal entries, macronutrient breakdowns, water intake logs, and weight history saved in your personal diary.",
            "Meal Photos & Camera Access: When you capture or select a food photo using the Scanner, the image is compressed and transmitted securely to analyze dish items, portion weights, and nutritional values. We do not sell or publicly share your food photos."
        )
    ),
    PolicySection(
        number = "03",
        title = "How We Use Your Information",
        body = "Your data is used exclusively to power your personal nutrition experience:",
        bullets = listOf(
            "Calculate your estimated Basal Metabolic Rate (BMR), Total Daily Energy Expenditure (TDEE), Body Mass Index (BMI), and daily macronutrient goals using the Mifflin-St Jeor equation.",
            "Identify dishes and estimate calories, protein, carbohydrates, fats, fiber, and sugar from food images you choose to scan.",
            "Generate personalized daily meal plans tailored to your dietary preferences (Bengali Traditional, Indian Vegetarian, Non-Vegetarian, Vegan, or High-Protein diets).",
            "Synchronize your food diary, hydration logs, and weight progress across your authenticated device sessions."
        )
    ),
    PolicySection(
        number = "04",
        title = "Data Storage, Security & Zero-Trust Protection",
        body = "Your personal profile, food diary, water intake history, and weight records are stored in Google Cloud Firestore and cached locally on your device for offline reliability.",
        bullets = listOf(
            "Strict Per-User Isolation: Cloud Firestore security rules enforce strict ownership checks so that only your authenticated account can read, create, update, or delete your personal records.",
            "Encryption in Transit & at Rest: All network requests and image analyses are transmitted over encrypted HTTPS/TLS channels."
        )
    ),
    PolicySection(
        number = "05",
        title = "Permissions We Request",
        body = "NutriScan follows least-privilege Android permission practices:",
        bullets = listOf(
            "Camera (android.permission.CAMERA): Requested only when you tap the camera button to capture a meal photo for nutritional scanning.",
            "Android Photo Picker: Allows selecting meal photos from your gallery without requesting broad storage permissions.",
            "Internet (android.permission.INTERNET): Used to authenticate your account, analyze meal images, and sync your nutrition diary."
        )
    ),
    PolicySection(
        number = "06",
        title = "Account & Data Deletion Rights",
        body = "You have full control over your personal data at all times:",
        bullets = listOf(
            "You can edit your name, age, height, weight, and dietary goals directly from the Profile tab.",
            "You can delete individual food diary entries or water logs at any time.",
            "You can permanently delete your profile data and sign out at any time by tapping Delete Profile Data in the Profile & Settings screen."
        )
    ),
    PolicySection(
        number = "07",
        title = "Medical & Nutritional Disclaimer",
        body = "NutriScan is a general wellness, food tracking, and educational nutrition tool. It is NOT a medical device and does not provide medical advice, diagnosis, or treatment. All calorie requirements, BMI classifications, visual meal scan quantities, and macronutrient breakdowns are approximate estimates. Always consult a qualified physician or registered dietitian before starting any new diet or nutrition regimen."
    ),
    PolicySection(
        number = "08",
        title = "Contact Us",
        body = "If you have questions or requests regarding this Privacy Policy or your personal data, please contact us through the application settings or via email at support@nutriscan.app."
    )
)

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("privacy_policy_screen")
    ) {
        // Top App Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("privacy_policy_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Privacy Policy & Terms",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "NutriScan • Official Policy Document",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = PastelGreenCard,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = FreshGreenPrimary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
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
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = DarkCharcoalText
                        ) {
                            Text(
                                text = "EFFECTIVE DATE: NOVEMBER 11, 2026",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your Data & Nutrition Privacy",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "NutriScan safeguards your health and calorie tracking data with zero-trust security and end-to-end privacy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkCharcoalText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

                items(privacyPolicySections, key = { it.number }) { section ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PastelGreenCard,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = section.number,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FreshGreenPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshGreenPrimary
                                )
                            }

                            Text(
                                text = section.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            section.bullets.forEach { bullet ->
                                Row(
                                    modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FreshGreenPrimary
                                    )
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }
}
