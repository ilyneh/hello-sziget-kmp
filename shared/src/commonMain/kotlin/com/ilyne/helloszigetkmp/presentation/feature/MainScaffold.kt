package com.ilyne.helloszigetkmp.presentation.feature

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
import com.ilyne.helloszigetkmp.presentation.feature.discover.DiscoverScreen
import com.ilyne.helloszigetkmp.presentation.feature.lineup.MyLineupScreen
import com.ilyne.helloszigetkmp.presentation.feature.profile.ProfileScreen
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleScreen
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
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
fun MainScaffold(
    openFilterScreen: (ScheduleFilter) -> Unit,
    onNavigateToAddFriend: () -> Unit,
    appliedFilter: ScheduleFilter?,
    onAppliedFilterConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        containerColor = SzigetPalette.Navy,
        bottomBar = {
            Surface(
                modifier = modifier
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .graphicsLayer {
                        shadowElevation = 16.dp.toPx() // The size/spread of the shadow
                        shape = RoundedCornerShape(20.dp)
                        clip = false

                        // Lower the intensity by reducing the alpha (opacity) of the shadow colors
                        ambientShadowColor = Color.White // Ultra soft ambient glow
                        spotShadowColor = Color.White // Softer directional shadow
                    },
                shape = RoundedCornerShape(20.dp),
                color = SzigetPalette.Navy,
                tonalElevation = 0.25.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
            composable<ScheduleTab> {
                ScheduleScreen(
                    openFilterScreen = openFilterScreen,
                    appliedFilter = appliedFilter,
                    onAppliedFilterConsumed = onAppliedFilterConsumed
                )
            }
            composable<DiscoverTab> { DiscoverScreen() }
            composable<LineupTab> { MyLineupScreen() }
            composable<ProfileTab> { ProfileScreen(onNavigateToAddFriend = onNavigateToAddFriend) }
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
        MaterialTheme.colorScheme.onSecondary
    } else {
        SzigetPalette.FaintText
    }

    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 18.dp))
            .background(color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(value = LocalContentColor provides contentColor) {
            icon()
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = contentColor)
        }
    }
}
