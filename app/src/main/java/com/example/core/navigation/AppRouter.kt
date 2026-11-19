package com.example.core.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.core.ads.AdMobManager
import com.example.core.ads.findActivity
import com.example.core.signOutUser
import com.example.model.UserProfile
import com.example.ui.screens.AiScannerScreen
import com.example.ui.screens.FoodDiaryAndDatabaseScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingProfileDialog
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.ThemePreferenceManager
import com.example.viewmodel.NutriScanViewModel
import com.example.widgets.AnchoredAdaptiveBannerAd

object AppRoutes {
    const val HOME = "dashboard"
    const val SCAN = "food_scanner"
    const val DIARY = "food_diary"
    const val PROFILE = "profile"
    const val PRIVACY_POLICY = "privacy_policy"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRouter(
    viewModel: NutriScanViewModel,
    currentUserId: String,
    userEmail: String,
    userDisplayName: String,
    onExitGuestMode: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val profile = profileState ?: UserProfile(
        userId = currentUserId,
        name = userDisplayName.ifBlank { "User" },
        email = userEmail
    )
    val vmDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val themeMode by ThemePreferenceManager.themeMode.collectAsStateWithLifecycle()
    val isDarkMode = ThemePreferenceManager.resolveIsDarkTheme(themeMode, vmDarkMode)
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.foodDiaryEntries.collectAsStateWithLifecycle()
    val waterHistory by viewModel.waterHistory.collectAsStateWithLifecycle()
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val scanUiState by viewModel.scanUiState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val needsProfileSetup by viewModel.needsProfileSetup.collectAsStateWithLifecycle()

    val strings = LocalizationCatalog.forLanguage(profile.language)
    var showProfileEditSheet by rememberSaveable { mutableStateOf(false) }
    var pendingDiarySubTab by rememberSaveable { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
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

    LaunchedEffect(profile.isPremium) {
        AdMobManager.updatePremiumStatus(profile.isPremium)
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val handleToggleDarkMode: (Boolean) -> Unit = { enabled ->
        ThemePreferenceManager.setDarkMode(context, enabled)
        viewModel.toggleDarkMode(enabled)
    }

    AppTheme(darkTheme = isDarkMode) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (currentRoute != AppRoutes.PRIVACY_POLICY) {
                    Column {
                        AnchoredAdaptiveBannerAd(
                            isPremium = profile.isPremium
                        )
                        Surface(
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.testTag("main_bottom_navigation_bar")
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                val itemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FreshGreenPrimary,
                                    selectedTextColor = FreshGreenPrimary,
                                    indicatorColor = FreshGreenPrimary.copy(alpha = 0.14f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                NavigationBarItem(
                                    selected = currentRoute == AppRoutes.HOME,
                                    onClick = { navigateToTopLevel(AppRoutes.HOME) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == AppRoutes.HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                                            contentDescription = strings.navHome
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = strings.navHome,
                                            fontWeight = if (currentRoute == AppRoutes.HOME) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_home")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == AppRoutes.SCAN,
                                    onClick = { navigateToTopLevel(AppRoutes.SCAN) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == AppRoutes.SCAN) Icons.Rounded.CameraAlt else Icons.Outlined.CameraAlt,
                                            contentDescription = strings.navScan
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = strings.navScan,
                                            fontWeight = if (currentRoute == AppRoutes.SCAN) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_scan")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == AppRoutes.DIARY,
                                    onClick = { navigateToTopLevel(AppRoutes.DIARY) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == AppRoutes.DIARY) Icons.Rounded.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                                            contentDescription = strings.navDiary
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = strings.navDiary,
                                            fontWeight = if (currentRoute == AppRoutes.DIARY) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_diary")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == AppRoutes.PROFILE,
                                    onClick = { navigateToTopLevel(AppRoutes.PROFILE) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == AppRoutes.PROFILE) Icons.Rounded.Person else Icons.Outlined.Person,
                                            contentDescription = strings.navProfile
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = strings.navProfile,
                                            fontWeight = if (currentRoute == AppRoutes.PROFILE) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_profile")
                                )
                            }
                        }
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
                        profile = profile,
                        selectedDateKey = selectedDateKey,
                        diaryEntries = diaryEntries,
                        waterRecords = waterHistory,
                        savedRecipes = savedRecipes,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = handleToggleDarkMode,
                        onNavigateToScan = { navigateToTopLevel(AppRoutes.SCAN) },
                        onNavigateToDiary = { navigateToTopLevel(AppRoutes.DIARY) },
                        onNavigateToMyRecipes = {
                            pendingDiarySubTab = 1
                            navigateToTopLevel(AppRoutes.DIARY)
                        },
                        onQuickAddWater = { ml -> viewModel.logWaterIntake(ml) },
                        onDecrementWater = { ml -> viewModel.decrementWaterIntake(ml) },
                        onSaveProfile = { updated -> viewModel.updateProfile(updated) },
                        onQuickLogSavedRecipe = { recipe -> viewModel.logSavedRecipeToDiary(recipe) }
                    )
                }

                composable(AppRoutes.SCAN) {
                    AiScannerScreen(
                        scanUiState = scanUiState,
                        onAnalyzeBitmap = { bmp, hint -> viewModel.analyzeFoodImage(bmp, hint) },
                        onAnalyzeText = { desc -> viewModel.analyzeFoodText(desc) },
                        onUpdateScannedItem = { idx, name, g -> viewModel.updateScannedFoodItem(idx, name, g) },
                        onUpdatePortionMultiplier = { mult -> viewModel.updateScanPortionMultiplier(mult) },
                        onConfirmMealToDiary = { meal ->
                            viewModel.confirmScannedMealToDiary(meal)
                            AdMobManager.showInterstitialIfReady(
                                activity = activity,
                                isPremium = profile.isPremium,
                                onContinue = { navigateToTopLevel(AppRoutes.DIARY) }
                            )
                        },
                        onResetScan = { viewModel.resetScanState() },
                        onSaveScannedComboToRecipes = { title, mealType ->
                            viewModel.saveScannedMealToRecipes(title, mealType)
                        }
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
                        profile = profile,
                        onSaveProfile = { updated -> viewModel.updateProfile(updated) },
                        savedRecipes = savedRecipes,
                        initialSubTab = pendingDiarySubTab,
                        onInitialSubTabConsumed = { pendingDiarySubTab = 0 },
                        onSaveCustomRecipe = { recipe -> viewModel.saveCustomRecipe(recipe) },
                        onDeleteSavedRecipe = { id -> viewModel.deleteSavedRecipe(id) },
                        onToggleSavedRecipeFavorite = { recipe -> viewModel.toggleSavedRecipeFavorite(recipe) },
                        onLogSavedRecipe = { recipe, slot -> viewModel.logSavedRecipeToDiary(recipe, slot) },
                        onSaveDiaryGroupAsRecipe = { title, mealType, entries ->
                            viewModel.saveDiaryEntriesToRecipes(title, mealType, entries)
                        }
                    )
                }

                composable(AppRoutes.PROFILE) {
                    ProfileAndSettingsScreen(
                        profile = profile,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = handleToggleDarkMode,
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        onOpenEditProfileDialog = { showProfileEditSheet = true },
                        onSaveProfile = { updated -> viewModel.updateProfile(updated) },
                        onOpenPrivacyPolicy = { navController.navigate(AppRoutes.PRIVACY_POLICY) },
                        onDeleteAccountData = {
                            viewModel.deleteAccountData {
                                if (viewModel.isGuestMode) {
                                    onExitGuestMode()
                                } else {
                                    signOutUser(context, credentialManager, {}, scope)
                                }
                            }
                        },
                        onSignOut = {
                            if (viewModel.isGuestMode) {
                                onExitGuestMode()
                            } else {
                                signOutUser(context, credentialManager, {}, scope)
                            }
                        }
                    )
                }

                composable(AppRoutes.PRIVACY_POLICY) {
                    PrivacyPolicyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (needsProfileSetup || showProfileEditSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (needsProfileSetup) viewModel.dismissProfileSetup()
                    showProfileEditSheet = false
                }
            ) {
                OnboardingProfileDialog(
                    currentProfile = profile,
                    isInitialSetup = needsProfileSetup,
                    onSaveProfile = { updated ->
                        viewModel.updateProfile(updated)
                        showProfileEditSheet = false
                    },
                    onSkip = {
                        if (needsProfileSetup) viewModel.dismissProfileSetup()
                        showProfileEditSheet = false
                    }
                )
            }
        }
    }
}
