package com.ilyne.helloszigetkmp.presentation.feature

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
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
import hello_sziget_kmp.shared.generated.resources.ic_heart_outline
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
    onNavigateToPhotoPicker: () -> Unit,
    onArtistClick: (String) -> Unit,
    appliedFilter: ScheduleFilter?,
    onAppliedFilterConsumed: () -> Unit,
    pickedPhotoUrl: String?,
    onPickedPhotoConsumed: () -> Unit,
    onLoggedOut: () -> Unit = {},
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

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentTabIndex = tabs.indexOfFirst { currentDestination?.hasRoute(it.route::class) == true }

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
                painter = painterResource(Res.drawable.ic_heart_outline),
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
                            onClick = { navigateToTab(tab.route) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val swipeState = remember { SwipeAccumulator() }
        // Keep the top status-bar inset but drop the bottom one, so content flows
        // underneath the floating navigation bar instead of stopping above it.
        fun tabIndex(destination: NavDestination?) =
            tabs.indexOfFirst { destination?.hasRoute(it.route::class) == true }

        NavHost(
            navController = navController,
            startDestination = ScheduleTab,
            enterTransition = {
                if (tabIndex(targetState.destination) >= tabIndex(initialState.destination)) {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right)
                }
            },
            exitTransition = {
                if (tabIndex(targetState.destination) >= tabIndex(initialState.destination)) {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
                }
            },
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding(), bottom = 0.dp)
                .pointerInput(currentTabIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeState.total = 0f },
                        onDragCancel = { swipeState.total = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            swipeState.total += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (currentTabIndex >= 0) {
                                if (swipeState.total <= -SwipeThresholdPx && currentTabIndex < tabs.lastIndex) {
                                    navigateToTab(tabs[currentTabIndex + 1].route)
                                } else if (swipeState.total >= SwipeThresholdPx && currentTabIndex > 0) {
                                    navigateToTab(tabs[currentTabIndex - 1].route)
                                }
                            }
                            swipeState.total = 0f
                        },
                    )
                },
        ) {
            composable<ScheduleTab> {
                ScheduleScreen(
                    openFilterScreen = openFilterScreen,
                    appliedFilter = appliedFilter,
                    onAppliedFilterConsumed = onAppliedFilterConsumed,
                    onArtistClick = onArtistClick,
                )
            }
            composable<DiscoverTab> { DiscoverScreen(onArtistClick = onArtistClick) }
            composable<LineupTab> { MyLineupScreen(onArtistClick = onArtistClick) }
            composable<ProfileTab> {
                ProfileScreen(
                    onNavigateToAddFriend = onNavigateToAddFriend,
                    onNavigateToPhotoPicker = onNavigateToPhotoPicker,
                    pickedPhotoUrl = pickedPhotoUrl,
                    onPickedPhotoConsumed = onPickedPhotoConsumed,
                    onLoggedOut = onLoggedOut,
                )
            }
        }
    }
}

private const val SwipeThresholdPx = 150f

private class SwipeAccumulator {
    var total: Float = 0f
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
