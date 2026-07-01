package com.ilyne.hello_sziget_kmp.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ilyne.hello_sziget_kmp.presentation.discover.DiscoverScreen
import com.ilyne.hello_sziget_kmp.presentation.lineup.MyLineupScreen
import com.ilyne.hello_sziget_kmp.presentation.profile.ProfileScreen
import com.ilyne.hello_sziget_kmp.presentation.schedule.ScheduleScreen
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource

@Serializable object ScheduleTab
@Serializable object DiscoverTab
@Serializable object LineupTab
@Serializable object ProfileTab

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        BottomTab("Schedule", ScheduleTab),
        BottomTab("Discover", DiscoverTab),
        BottomTab("My Lineup", LineupTab),
        BottomTab("Profile", ProfileTab),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hasRoute(tab.route::class) == true
                    NavigationBarItem(
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
                        label = { Text(tab.label) },
                        icon = { /* icons to be added */ },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScheduleTab,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<ScheduleTab> { ScheduleScreen() }
            composable<DiscoverTab> { DiscoverScreen() }
            composable<LineupTab> { MyLineupScreen() }
            composable<ProfileTab> { ProfileScreen() }
        }
    }
}

private data class BottomTab(val label: String, val route: Any)
