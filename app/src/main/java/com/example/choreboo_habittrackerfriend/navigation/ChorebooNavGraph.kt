package com.example.choreboo_habittrackerfriend.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.choreboo_habittrackerfriend.ui.auth.AuthScreen
import com.example.choreboo_habittrackerfriend.ui.habits.HabitListScreen
import com.example.choreboo_habittrackerfriend.ui.habits.AddEditHabitScreen
import com.example.choreboo_habittrackerfriend.ui.pet.PetScreen
import com.example.choreboo_habittrackerfriend.ui.calendar.CalendarScreen
import com.example.choreboo_habittrackerfriend.ui.onboarding.OnboardingScreen
import com.example.choreboo_habittrackerfriend.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Onboarding : Screen("onboarding")
    data object HabitList : Screen("habits_list")
    data object AddEditHabit : Screen("add_edit_habit?habitId={habitId}") {
        fun createRoute(habitId: Long? = null): String {
            return if (habitId != null) "add_edit_habit?habitId=$habitId" else "add_edit_habit"
        }
    }
    data object Pet : Screen("pet")
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
                onAuthSuccess = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.HabitList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HabitList.route) {
            HabitListScreen(
                onAddHabit = { navController.navigate(Screen.AddEditHabit.createRoute()) },
                onEditHabit = { id -> navController.navigate(Screen.AddEditHabit.createRoute(id)) },
            )
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
            )
        }

        composable(Screen.Pet.route) {
            PetScreen()
        }

        composable(Screen.Calendar.route) {
            CalendarScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
