package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.ads.AdMobManager
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.navigation.AppRouter
import com.example.data.local.NutriScanDatabase
import com.example.data.repository.NutriScanRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ThemePreferenceManager
import com.example.viewmodel.NutriScanViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private var hasBeenStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Initialize persistent Theme Mode, Language, and AdMob SDK
        ThemePreferenceManager.initialize(this)
        com.example.core.AppLanguageManager.initialize(this)
        AdMobManager.initialize(this)

        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasBeenStopped) {
            hasBeenStopped = false
            AdMobManager.showAppOpenAdIfReady(this)
        } else {
            AdMobManager.preloadAppOpenAd(this)
        }
    }

    override fun onStop() {
        super.onStop()
        hasBeenStopped = true
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val themeMode by ThemePreferenceManager.themeMode.collectAsStateWithLifecycle()
    val darkOverride by ThemePreferenceManager.isDarkModeOverride.collectAsStateWithLifecycle()
    val resolvedDarkTheme = ThemePreferenceManager.resolveIsDarkTheme(themeMode, darkOverride)

    val user = currentUser
    var isGuestSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null) {
            isGuestSession = false
        }
    }

    if (user != null) {
        AuthenticatedNutriScanApp(
            currentUserId = user.uid,
            userEmail = user.email.orEmpty(),
            userDisplayName = user.displayName.orEmpty(),
            onExitGuestMode = {}
        )
    } else if (isGuestSession) {
        AuthenticatedNutriScanApp(
            currentUserId = NutriScanViewModel.GUEST_USER_ID,
            userEmail = "Guest Mode • Local Device Session",
            userDisplayName = "Guest Explorer",
            onExitGuestMode = { isGuestSession = false }
        )
    } else {
        AppTheme(darkTheme = resolvedDarkTheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthScreen(
                    onAuthSuccess = {},
                    onContinueAsGuest = { isGuestSession = true }
                )
            }
        }
    }
}

@Composable
fun AuthenticatedNutriScanApp(
    currentUserId: String,
    userEmail: String,
    userDisplayName: String,
    onExitGuestMode: () -> Unit = {}
) {
    val viewModel: NutriScanViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId).apply {
                    try {
                        val newSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder(firestoreSettings)
                            .setLocalCacheSettings(
                                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                                    .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                    .build()
                            )
                            .build()
                        firestoreSettings = newSettings
                    } catch (e: Exception) {
                        android.util.Log.w("MainActivity", "Firestore local persistence config: ${e.message}")
                    }
                }
                val localDao = NutriScanDatabase.getInstance(app).localDiaryDao()
                NutriScanViewModel(
                    repository = NutriScanRepository(db, localDao, app.applicationContext),
                    currentUserId = currentUserId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    AppRouter(
        viewModel = viewModel,
        currentUserId = currentUserId,
        userEmail = userEmail,
        userDisplayName = userDisplayName,
        onExitGuestMode = onExitGuestMode
    )
}
