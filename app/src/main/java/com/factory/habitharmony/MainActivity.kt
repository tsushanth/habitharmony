package com.factory.habitharmony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.habitharmony.navigation.AppNavigation
import com.factory.habitharmony.ui.theme.HabitHarmonyTheme
import com.factory.habitharmony.viewmodel.HabitViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission denied by user")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        val app = application as HabitHarmonyApp
        val settingsPrefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by remember {
                mutableStateOf(settingsPrefs.getBoolean("dark_mode_enabled", systemDark))
            }
            HabitHarmonyTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val habitViewModel: HabitViewModel = viewModel(
                        factory = HabitViewModel.factory(app.habitRepository)
                    )
                    AppNavigation(
                        habitViewModel = habitViewModel,
                        billingManager = app.billingManager,
                        premiumManager = app.premiumManager,
                        onDarkModeChanged = { enabled ->
                            darkMode = enabled
                            settingsPrefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
