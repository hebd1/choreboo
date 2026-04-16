package com.choreboo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.navigation.Screen
import com.choreboo.app.navigation.ChorebooNavGraph
import com.choreboo.app.ui.components.BottomNavBar
import com.choreboo.app.ui.theme.ChorebooHabitTrackerFriendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isAppReady by viewModel.isAppReady.collectAsStateWithLifecycle()
            val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val petMood by viewModel.petMood.collectAsStateWithLifecycle()

            ChorebooHabitTrackerFriendTheme(
                themeMode = themeMode,
            ) {
                if (!isAppReady) {
                    BrandedSplashScreen()
                } else {
                    val startDestination = remember {
                        when {
                            !viewModel.authRepository.isAuthenticated -> Screen.Auth.route
                            onboardingComplete == false -> Screen.Onboarding.route
                            else -> Screen.Pet.route
                        }
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

/**
 * Branded splash screen shown while the app initialises (DataStore, Room warmup,
 * cloud sync, and Lottie animation preloading). Displays the Choreboo egg emoji,
 * app name, and a loading spinner.
 */
@Composable
private fun BrandedSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "\uD83E\uDD5A",
                fontSize = 64.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.splash_brand_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.splash_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    petMood = petMood,
                )
            }
        },
    ) { innerPadding ->
        ChorebooNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}
