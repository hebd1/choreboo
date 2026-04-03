package com.example.choreboo_habittrackerfriend.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.OtherHouses
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.navigation.Screen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

@Composable
fun BottomNavBar(
    navController: NavController,
    petMood: ChorebooMood = ChorebooMood.IDLE,
    modifier: Modifier = Modifier,
) {
    val petIcon = when (petMood) {
        ChorebooMood.HAPPY -> Icons.Default.Favorite
        ChorebooMood.CONTENT -> Icons.Default.EmojiEmotions
        ChorebooMood.HUNGRY -> Icons.Default.Restaurant
        ChorebooMood.TIRED -> Icons.Default.NightsStay
        ChorebooMood.SAD -> Icons.Default.MoodBad
        ChorebooMood.IDLE -> Icons.Default.Pets
    }

    val items = listOf(
        BottomNavItem("Habits", Icons.AutoMirrored.Filled.FormatListBulleted, Screen.HabitList.route),
        BottomNavItem("Choreboo", petIcon, Screen.Pet.route),
        BottomNavItem("House", Icons.Default.OtherHouses, Screen.Household.route),
        BottomNavItem("Calendar", Icons.Default.CalendarMonth, Screen.Calendar.route),
        BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Glassmorphic floating nav container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                )
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavBarTab(
                    item = item,
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
                )
            }
        }
    }
}

@Composable
private fun NavBarTab(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val offsetY: Dp by animateDpAsState(
        targetValue = if (selected) (-6).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navIconOffset",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = offsetY)
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .then(
                if (selected)
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                else
                    Modifier
            ),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp),
        )
    }
}
