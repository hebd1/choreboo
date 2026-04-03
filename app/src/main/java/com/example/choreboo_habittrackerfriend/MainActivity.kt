package com.example.choreboo_habittrackerfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.airbnb.lottie.LottieCompositionFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.navigation.Screen
import com.example.choreboo_habittrackerfriend.navigation.ChorebooNavGraph
import com.example.choreboo_habittrackerfriend.ui.components.BottomNavBar
import com.example.choreboo_habittrackerfriend.ui.theme.ChorebooHabitTrackerFriendTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var chorebooRepository: ChorebooRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload fox Lottie compositions into LottieCompositionCache so they are
        // instantly available when the user first navigates to the Pet tab.
        listOf(
            "animations/fox/fox_happy_lottie.json",
            "animations/fox/fox_hungry_lottie.json",
            "animations/fox/fox_sad_lottie.json",
            "animations/fox/fox_idle_lottie.json",
            "animations/fox/fox_eating_lottie.json",
            "animations/fox/fox_interact_lottie.json",
            "animations/fox/fox_start_sleep_lottie.json",
            "animations/fox/fox_loop_sleeping_lottie.json",
            "animations/fox/overscreen_fox_lottie.json",
        ).forEach { path ->
            LottieCompositionFactory.fromAsset(this, path)
        }

        setContent {
            val onboardingComplete by userPreferences.onboardingComplete.collectAsState(initial = null)
            val themeMode by userPreferences.themeMode.collectAsState(initial = "system")
            val petMood by chorebooRepository.getChoreboo()
                .collectAsState(initial = null)

            ChorebooHabitTrackerFriendTheme(
                themeMode = themeMode,
            ) {
                // Wait for DataStore to load
                when (onboardingComplete) {
                    null -> { /* Loading — blank screen briefly */ }
                    else -> {
                        val startDestination = when {
                            !authRepository.isAuthenticated -> Screen.Auth.route
                            onboardingComplete == false -> Screen.Onboarding.route
                            else -> Screen.HabitList.route
                        }
                        ChorebooApp(
                            startDestination = startDestination,
                            petMood = petMood?.mood ?: ChorebooMood.IDLE,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChorebooApp(
    startDestination: String,
    petMood: ChorebooMood = ChorebooMood.IDLE,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes that should show the bottom nav bar
    val bottomNavRoutes = listOf(
        Screen.HabitList.route,
        Screen.Pet.route,
        Screen.Household.route,
        Screen.Calendar.route,
        Screen.Settings.route,
    )
    val showBottomBar = currentRoute in bottomNavRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.statusBars,
        ) { innerPadding ->
            ChorebooNavGraph(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }

        if (showBottomBar) {
            BottomNavBar(
                navController = navController,
                petMood = petMood,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}