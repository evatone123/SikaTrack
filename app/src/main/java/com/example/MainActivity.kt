package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.screens.PinLockScreen
import com.example.ui.theme.PocketLedgerTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current.applicationContext as android.app.Application
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(context))
            val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

            PocketLedgerTheme(themePreference = userPrefs.themeMode) {
                if (userPrefs.pinEnabled && !isUnlocked) {
                    PinLockScreen(
                        correctPin = userPrefs.pinCode,
                        onSuccess = { viewModel.unlockApp() }
                    )
                } else {
                    PocketLedgerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PocketLedgerApp(viewModel: MainViewModel) {
    MainAppScaffold(viewModel = viewModel)
}
