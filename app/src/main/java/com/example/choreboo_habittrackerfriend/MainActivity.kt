package com.example.choreboo_habittrackerfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.airbnb.lottie.LottieCompositionFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.navigation.Screen
import com.example.choreboo_habittrackerfriend.navigation.ChorebooNavGraph
import com.example.choreboo_habittrackerfriend.ui.components.BottomNavBar
import com.example.choreboo_habittrackerfriend.ui.theme.ChorebooHabitTrackerFriendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload only the idle animation — the most likely first frame the user will see.
        // Other animations load on-demand in PetAnimation when the relevant phase triggers.
        LottieCompositionFactory.fromAsset(this, "animations/fox/fox_idle_lottie.json")

        setContent {
            val onboardingComplete by viewModel.onboardingComplete.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val petMood by viewModel.petMood.collectAsState()

            ChorebooHabitTrackerFriendTheme(
                themeMode = themeMode,
            ) {
                when (onboardingComplete) {
                    null -> {
                        // DataStore not yet loaded — show a brief spinner instead of blank screen
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        val startDestination = when {
                            !viewModel.authRepository.isAuthenticated -> Screen.Auth.route
                            onboardingComplete == false -> Screen.Onboarding.route
                            else -> Screen.Pet.route
                        }
                        ChorebooApp(
                            startDestination = startDestination,
                            petMood = petMood,
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
        Screen.Pet.route,
        Screen.Stats.route,
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
