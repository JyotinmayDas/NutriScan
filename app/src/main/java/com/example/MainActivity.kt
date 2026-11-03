package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.navigation.AppRouter
import com.example.data.local.NutriScanDatabase
import com.example.data.repository.NutriScanRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.AppTheme
import com.example.viewmodel.NutriScanViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AppTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthScreen(onAuthSuccess = {})
            }
        }
    } else {
        AuthenticatedNutriScanApp(
            currentUserId = user.uid,
            userEmail = user.email.orEmpty(),
            userDisplayName = user.displayName.orEmpty()
        )
    }
}

@Composable
fun AuthenticatedNutriScanApp(
    currentUserId: String,
    userEmail: String,
    userDisplayName: String
) {
    val viewModel: NutriScanViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDao = NutriScanDatabase.getInstance(app).localDiaryDao()
                NutriScanViewModel(
                    repository = NutriScanRepository(db, localDao),
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
        userDisplayName = userDisplayName
    )
}
