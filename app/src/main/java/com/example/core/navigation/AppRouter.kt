package com.example.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.LocalizationCatalog
import com.example.core.signOutUser
import com.example.features.water_tracker.WaterTrackerScreen
import com.example.model.UserProfile
import com.example.ui.screens.AiCoachAndMealPlannerScreen
import com.example.ui.screens.AiScannerScreen
import com.example.ui.screens.FoodDiaryAndDatabaseScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingProfileDialog
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.viewmodel.NutriScanViewModel

/**
 * Clean Architecture Route Definitions for NutriScan AI.
 */
object AppRoutes {
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val HOME = "dashboard"
    const val SCAN = "food_scanner"
    const val DIARY = "food_diary"
    const val AI_COACH = "ai_coach"
    const val PROFILE = "profile"
    const val WATER_TRACKER = "water_tracker"
}

/**
 * Base AppRouter component managing all primary tabs and feature routes
 * using Jetpack Navigation Compose, styled with a floating rounded bottom bar
 * matching the uploaded UI designs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRouter(
    viewModel: NutriScanViewModel,
    currentUserId: String,
    userEmail: String,
    userDisplayName: String,
    navController: NavHostController = rememberNavController()
) {
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val profile = profileState ?: UserProfile(
        userId = currentUserId,
        name = userDisplayName.ifBlank { "NutriScan Explorer" },
        email = userEmail
    )
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.foodDiaryEntries.collectAsStateWithLifecycle()
    val waterHistory by viewModel.waterHistory.collectAsStateWithLifecycle()
    val weightHistory by viewModel.weightHistory.collectAsStateWithLifecycle()
    val mealPlans by viewModel.mealPlans.collectAsStateWithLifecycle()
    val aiHistory by viewModel.aiHistory.collectAsStateWithLifecycle()
    val scanUiState by viewModel.scanUiState.collectAsStateWithLifecycle()
    val coachUiState by viewModel.coachUiState.collectAsStateWithLifecycle()
    val mealPlanUiState by viewModel.mealPlanUiState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val strings = LocalizationCatalog.forLanguage(profile.language)
    var showProfileEditSheet by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.HOME

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = DarkCharcoalText,
        selectedTextColor = FreshGreenPrimary,
        indicatorColor = LimeAccent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    AppTheme(darkTheme = isDarkMode) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    shadowElevation = 10.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.HOME,
                            onClick = { navigateToTopLevel(AppRoutes.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = strings.navHome) },
                            label = { Text(strings.navHome, fontWeight = FontWeight.SemiBold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.DIARY,
                            onClick = { navigateToTopLevel(AppRoutes.DIARY) },
                            icon = { Icon(Icons.Default.Book, contentDescription = strings.navDiary) },
                            label = { Text(strings.navDiary, fontWeight = FontWeight.SemiBold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_diary")
                        )
                        // Prominent Center Circular Dark/Green Scan Button (Matching Uploaded Images 1, 3, 9, 10)
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.SCAN,
                            onClick = { navigateToTopLevel(AppRoutes.SCAN) },
                            icon = {
                                FloatingActionButton(
                                    onClick = { navigateToTopLevel(AppRoutes.SCAN) },
                                    shape = CircleShape,
                                    containerColor = if (currentRoute == AppRoutes.SCAN) FreshGreenPrimary else DarkCharcoalText,
                                    contentColor = Color.White,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CenterFocusStrong,
                                        contentDescription = strings.navScan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = strings.navScan,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshGreenPrimary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_scan")
                        )
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.AI_COACH,
                            onClick = { navigateToTopLevel(AppRoutes.AI_COACH) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.navCoach) },
                            label = { Text(strings.navCoach, fontWeight = FontWeight.SemiBold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_coach")
                        )
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.PROFILE,
                            onClick = { navigateToTopLevel(AppRoutes.PROFILE) },
                            icon = { Icon(Icons.Default.Person, contentDescription = strings.navProfile) },
                            label = { Text(strings.navProfile, fontWeight = FontWeight.SemiBold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoutes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(AppRoutes.HOME) {
                    HomeDashboardScreen(
                        strings = strings,
                        profile = profile,
                        selectedDateKey = selectedDateKey,
                        diaryEntries = diaryEntries,
                        waterRecords = waterHistory,
                        weightRecords = weightHistory,
                        remainingScansToday = viewModel.getRemainingScansToday(),
                        onNavigateToScan = { navigateToTopLevel(AppRoutes.SCAN) },
                        onNavigateToDiary = { navigateToTopLevel(AppRoutes.DIARY) },
                        onNavigateToCoach = { navigateToTopLevel(AppRoutes.AI_COACH) },
                        onQuickAddWater = { ml -> viewModel.logWaterIntake(ml) },
                        onOpenOnboardingEdit = { showProfileEditSheet = true }
                    )
                }

                composable(AppRoutes.SCAN) {
                    AiScannerScreen(
                        scanUiState = scanUiState,
                        remainingScansToday = viewModel.getRemainingScansToday(),
                        isPremium = profile.isPremium,
                        onAnalyzeBitmap = { bmp, hint -> viewModel.analyzeFoodImage(bmp, hint) },
                        onUpdateScannedItem = { idx, name, g -> viewModel.updateScannedFoodItem(idx, name, g) },
                        onConfirmMealToDiary = { meal ->
                            viewModel.confirmScannedMealToDiary(meal)
                            navigateToTopLevel(AppRoutes.DIARY)
                        },
                        onResetScan = { viewModel.resetScanState() },
                        onGrantBonusScan = { viewModel.grantBonusScanFromReward() }
                    )
                }

                composable(AppRoutes.DIARY) {
                    FoodDiaryAndDatabaseScreen(
                        language = profile.language,
                        selectedDateKey = selectedDateKey,
                        diaryEntries = diaryEntries,
                        onSelectDate = { viewModel.selectDate(it) },
                        onAddDatabaseFood = { item, g, meal -> viewModel.addDatabaseFoodToDiary(item, g, meal) },
                        onAddManualFood = { entry -> viewModel.addManualFoodEntry(entry) },
                        onUpdateDiaryEntry = { entry, name, g -> viewModel.updateDiaryEntryPortion(entry, name, g) },
                        onDeleteDiaryEntry = { id -> viewModel.deleteDiaryEntry(id) },
                        onCopyPreviousDayMeals = { fromDate, toDate -> viewModel.copyEntriesFromDate(fromDate, toDate) }
                    )
                }

                composable(AppRoutes.AI_COACH) {
                    AiCoachAndMealPlannerScreen(
                        profile = profile,
                        diaryEntries = diaryEntries,
                        weightHistory = weightHistory,
                        mealPlans = mealPlans,
                        aiHistory = aiHistory,
                        coachUiState = coachUiState,
                        mealPlanUiState = mealPlanUiState,
                        onAskCoach = { q -> viewModel.askAiNutritionCoach(q) },
                        onGenerateMealPlan = { diet -> viewModel.generatePersonalizedMealPlan(diet) },
                        onLogWeight = { w, note -> viewModel.logWeightUpdate(w, note) }
                    )
                }

                composable(AppRoutes.PROFILE) {
                    ProfileAndSettingsScreen(
                        profile = profile,
                        isDarkMode = isDarkMode,
                        remainingScansToday = viewModel.getRemainingScansToday(),
                        onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        onTogglePremiumPlan = { viewModel.togglePremiumPlan(it) },
                        onGrantBonusScan = { viewModel.grantBonusScanFromReward() },
                        onOpenEditProfileDialog = { showProfileEditSheet = true },
                        onSaveProfile = { updated -> viewModel.updateProfile(updated) },
                        onDeleteAccountData = {
                            viewModel.deleteAccountData {
                                signOutUser(context, credentialManager, {}, scope)
                            }
                        },
                        onSignOut = {
                            signOutUser(context, credentialManager, {}, scope)
                        }
                    )
                }

                composable(AppRoutes.WATER_TRACKER) {
                    WaterTrackerScreen(
                        profile = profile,
                        selectedDateKey = selectedDateKey,
                        waterRecords = waterHistory,
                        onAddWater = { ml -> viewModel.logWaterIntake(ml) },
                        onDeleteWater = { id -> viewModel.deleteWaterEntry(id) },
                        onUpdateWaterGoal = { newGoal ->
                            viewModel.updateProfile(profile.copy(dailyWaterGoalMl = newGoal))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showProfileEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProfileEditSheet = false }
            ) {
                OnboardingProfileDialog(
                    currentProfile = profile,
                    onSaveProfile = { updated ->
                        viewModel.updateProfile(updated)
                        showProfileEditSheet = false
                    },
                    onSkip = { showProfileEditSheet = false }
                )
            }
        }
    }
}
