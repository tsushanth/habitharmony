package com.factory.habitharmony.navigation

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.factory.habitharmony.billing.BillingManager
import com.factory.habitharmony.billing.PremiumManager
import com.factory.habitharmony.ui.screen.AddEditHabitScreen
import com.factory.habitharmony.ui.screen.HabitDetailScreen
import com.factory.habitharmony.ui.screen.HabitsScreen
import com.factory.habitharmony.ui.screen.InsightsScreen
import com.factory.habitharmony.ui.screen.PaywallScreen
import com.factory.habitharmony.ui.screen.SettingsScreen
import com.factory.habitharmony.viewmodel.HabitViewModel

// ── Route definitions ─────────────────────────────────────────────────────────

private object Routes {
    const val HABITS = "habits"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
    const val ADD_HABIT = "add_habit"
    const val EDIT_HABIT = "edit_habit/{habitId}"
    const val HABIT_DETAIL = "habit_detail/{habitId}"

    fun editHabit(id: Long) = "edit_habit/$id"
    fun habitDetail(id: Long) = "habit_detail/$id"
}

// ── Bottom nav items ──────────────────────────────────────────────────────────

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.HABITS, "Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomTab(Routes.INSIGHTS, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

private val topLevelRoutes = bottomTabs.map { it.route }.toSet()

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation(
    habitViewModel: HabitViewModel,
    billingManager: BillingManager,
    premiumManager: PremiumManager,
    onDarkModeChanged: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HABITS,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // ── Habits ────────────────────────────────────────────────────────
            composable(Routes.HABITS) {
                // Show paywall once on first-ever launch (simulates post-onboarding trigger)
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val shown = prefs.getBoolean("has_shown_initial_paywall", false)
                    if (!shown && !premiumManager.isPremium) {
                        prefs.edit().putBoolean("has_shown_initial_paywall", true).apply()
                        navController.navigate(Routes.PAYWALL)
                    }
                }
                HabitsScreen(
                    viewModel = habitViewModel,
                    premiumManager = premiumManager,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                    onAddHabit = { navController.navigate(Routes.ADD_HABIT) },
                    onEditHabit = { id -> navController.navigate(Routes.editHabit(id)) },
                    onHabitDetail = { id -> navController.navigate(Routes.habitDetail(id)) }
                )
            }

            // ── Insights ──────────────────────────────────────────────────────
            composable(Routes.INSIGHTS) {
                InsightsScreen(
                    viewModel = habitViewModel,
                    premiumManager = premiumManager,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) }
                )
            }

            // ── Settings ──────────────────────────────────────────────────────
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    premiumManager = premiumManager,
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                    onDarkModeChanged = onDarkModeChanged
                )
            }

            // ── Paywall ───────────────────────────────────────────────────────
            composable(Routes.PAYWALL) {
                PaywallScreen(
                    billingManager = billingManager,
                    premiumManager = premiumManager,
                    onClose = { navController.popBackStack() }
                )
            }

            // ── Add habit ─────────────────────────────────────────────────────
            composable(Routes.ADD_HABIT) {
                AddEditHabitScreen(
                    viewModel = habitViewModel,
                    habitId = null,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Edit habit ────────────────────────────────────────────────────
            composable(
                route = Routes.EDIT_HABIT,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) { backStack ->
                val habitId = backStack.arguments?.getLong("habitId") ?: return@composable
                AddEditHabitScreen(
                    viewModel = habitViewModel,
                    habitId = habitId,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Habit detail ──────────────────────────────────────────────────
            composable(
                route = Routes.HABIT_DETAIL,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) { backStack ->
                val habitId = backStack.arguments?.getLong("habitId") ?: return@composable
                HabitDetailScreen(
                    viewModel = habitViewModel,
                    habitId = habitId,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onNavigateBack = { navController.popBackStack() },
                    onEditHabit = {
                        navController.navigate(Routes.editHabit(habitId))
                    }
                )
            }
        }
    }
}
