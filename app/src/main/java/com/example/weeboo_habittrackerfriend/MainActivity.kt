package com.example.weeboo_habittrackerfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weeboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.weeboo_habittrackerfriend.data.repository.WeebooRepository
import com.example.weeboo_habittrackerfriend.domain.model.WeebooMood
import com.example.weeboo_habittrackerfriend.navigation.Screen
import com.example.weeboo_habittrackerfriend.navigation.WeebooNavGraph
import com.example.weeboo_habittrackerfriend.ui.components.BottomNavBar
import com.example.weeboo_habittrackerfriend.ui.theme.WeebooHabitTrackerFriendTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var weebooRepository: WeebooRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val onboardingComplete by userPreferences.onboardingComplete.collectAsState(initial = null)
            val themeMode by userPreferences.themeMode.collectAsState(initial = "system")
            val petMood by weebooRepository.getWeeboo()
                .collectAsState(initial = null)

            WeebooHabitTrackerFriendTheme(
                themeMode = themeMode,
            ) {
                // Wait for DataStore to load
                when (onboardingComplete) {
                    null -> { /* Loading — blank screen briefly */ }
                    else -> {
                        WeebooApp(
                            startDestination = if (onboardingComplete == true)
                                Screen.HabitList.route else Screen.Onboarding.route,
                            petMood = petMood?.mood ?: WeebooMood.IDLE,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeebooApp(
    startDestination: String,
    petMood: WeebooMood = WeebooMood.IDLE,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes that should show the bottom nav bar
    val bottomNavRoutes = listOf(
        Screen.HabitList.route,
        Screen.Pet.route,
        Screen.Shop.route,
        Screen.Calendar.route,
    )
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, petMood = petMood)
            }
        }
    ) { innerPadding ->
        WeebooNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}