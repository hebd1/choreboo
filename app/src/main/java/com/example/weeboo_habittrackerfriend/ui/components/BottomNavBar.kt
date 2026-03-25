package com.example.weeboo_habittrackerfriend.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.weeboo_habittrackerfriend.domain.model.WeebooMood
import com.example.weeboo_habittrackerfriend.navigation.Screen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

@Composable
fun BottomNavBar(
    navController: NavController,
    petMood: WeebooMood = WeebooMood.IDLE,
) {
    val petIcon = when (petMood) {
        WeebooMood.HAPPY -> Icons.Default.Favorite
        WeebooMood.CONTENT -> Icons.Default.EmojiEmotions
        WeebooMood.HUNGRY -> Icons.Default.Restaurant
        WeebooMood.TIRED -> Icons.Default.NightsStay
        WeebooMood.SAD -> Icons.Default.MoodBad
        WeebooMood.IDLE -> Icons.Default.Pets
    }

    val items = listOf(
        BottomNavItem("Habits", Icons.Default.CheckCircle, Screen.HabitList.route),
        BottomNavItem("Weeboo", petIcon, Screen.Pet.route),
        BottomNavItem("Shop", Icons.Default.Store, Screen.Shop.route),
        BottomNavItem("Calendar", Icons.Default.CalendarMonth, Screen.Calendar.route),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.HabitList.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

