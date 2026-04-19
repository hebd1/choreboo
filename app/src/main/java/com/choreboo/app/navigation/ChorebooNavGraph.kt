package com.choreboo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.choreboo.app.ui.auth.AuthScreen
import com.choreboo.app.ui.habits.AddEditHabitScreen
import com.choreboo.app.ui.pet.PetScreen
import com.choreboo.app.ui.calendar.CalendarScreen
import com.choreboo.app.ui.household.HouseholdScreen
import com.choreboo.app.ui.onboarding.OnboardingScreen
import com.choreboo.app.ui.settings.SettingsScreen
import com.choreboo.app.ui.stats.StatsScreen

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Onboarding : Screen("onboarding")
    data object Pet : Screen("pet")
    data object Stats : Screen("stats")
    data object AddEditHabit : Screen("add_edit_habit?habitId={habitId}") {
        fun createRoute(habitId: Long? = null): String {
            return if (habitId != null) "add_edit_habit?habitId=$habitId" else "add_edit_habit"
        }
    }
    data object Household : Screen("household")
    data object Calendar : Screen("calendar")
    data object Settings : Screen("settings")
}

@Composable
fun ChorebooNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = { onboardingComplete ->
                    val destination = if (onboardingComplete) Screen.Pet.route else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Pet.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Pet.route) {
            PetScreen(
                onAddHabit = { navController.navigate(Screen.AddEditHabit.createRoute()) },
                onEditHabit = { id -> navController.navigate(Screen.AddEditHabit.createRoute(id)) },
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen()
        }

        composable(
            route = "add_edit_habit?habitId={habitId}",
            arguments = listOf(
                navArgument("habitId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
                ) {
                    AddEditHabitScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSavedBack = {
                            navController.popBackStack()
                        },
                        onDeletedBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Household.route) {
            HouseholdScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Pet.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
