package com.ilyne.helloszigetkmp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ilyne.helloszigetkmp.presentation.discover.DiscoverScreen
import com.ilyne.helloszigetkmp.presentation.lineup.MyLineupScreen
import com.ilyne.helloszigetkmp.presentation.profile.ProfileScreen
import com.ilyne.helloszigetkmp.presentation.schedule.ScheduleScreen
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_discover
import hello_sziget_kmp.shared.generated.resources.ic_heart
import hello_sziget_kmp.shared.generated.resources.ic_person
import hello_sziget_kmp.shared.generated.resources.ic_schedule
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource

@Serializable object ScheduleTab

@Serializable object DiscoverTab

@Serializable object LineupTab

@Serializable object ProfileTab

@Composable
fun MainScaffold(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        BottomTab("Schedule", ScheduleTab),
        BottomTab("Discover", DiscoverTab),
        BottomTab("My Lineup", LineupTab),
        BottomTab("Profile", ProfileTab),
    )

    val icons: Map<BottomTab, @Composable () -> Unit> = mapOf(
        tabs[0] to {
            Icon(
                painter = painterResource(Res.drawable.ic_schedule),
                contentDescription = "Schedule_Tab",
            )
        },
        tabs[1] to {
            Icon(
                painter = painterResource(Res.drawable.ic_discover),
                contentDescription = "Discover_Tab",
            )
        },
        tabs[2] to {
            Icon(
                painter = painterResource(Res.drawable.ic_heart),
                contentDescription = "My_Lineup_Tab",
            )
        },
        tabs[3] to {
            Icon(
                painter = painterResource(Res.drawable.ic_person),
                contentDescription = "Profile_Tab",
            )
        },
    )

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding().fillMaxWidth().graphicsLayer {
                    shadowElevation = 8.dp.toPx() // The size/spread of the shadow
                    shape = RoundedCornerShape(24.dp)
                    clip = false

                    // Lower the intensity by reducing the alpha (opacity) of the shadow colors
                    ambientShadowColor = Color.Black.copy(alpha = 0.2f) // Ultra soft ambient glow
                    spotShadowColor = Color.Black.copy(alpha = 0.4f) // Softer directional shadow
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.25.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                        BottomTabItem(
                            modifier = Modifier.weight(1f),
                            label = tab.label,
                            icon = { icons[tab]?.invoke() },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Keep the top status-bar inset but drop the bottom one, so content flows
        // underneath the floating navigation bar instead of stopping above it.
        NavHost(
            navController = navController,
            startDestination = ScheduleTab,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding(), bottom = 0.dp),
        ) {
            composable<ScheduleTab> { ScheduleScreen() }
            composable<DiscoverTab> { DiscoverScreen() }
            composable<LineupTab> { MyLineupScreen() }
            composable<ProfileTab> { ProfileScreen() }
        }
    }
}

private data class BottomTab(
    val label: String,
    val route: Any,
)

@Composable
private fun BottomTabItem(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = contentColor)
        }
    }
}
