package com.ilyne.helloszigetkmp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ilyne.helloszigetkmp.presentation.feature.MainScaffold
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.AddFriendScreen
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterScreen
import com.ilyne.helloszigetkmp.presentation.login.LoginScreen
import kotlinx.serialization.Serializable

@Serializable object Login

@Serializable object Main

@Serializable object AddFriend

@Serializable data class ScheduleFilterRoute(val filter: ScheduleFilter)

private const val SCHEDULE_FILTER_RESULT_KEY = "scheduleFilterResult"


@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: Any = Login
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Main) {
                        popUpTo<Login> { inclusive = true }
                    }
                },
            )
        }
        composable<Main> { backStackEntry ->
            val appliedFilter by backStackEntry.savedStateHandle
                .getStateFlow<ScheduleFilter?>(SCHEDULE_FILTER_RESULT_KEY, null)
                .collectAsStateWithLifecycle()

            MainScaffold(
                openFilterScreen = { filter -> navController.navigate(ScheduleFilterRoute(filter)) },
                onNavigateToAddFriend = { navController.navigate(AddFriend) },
                appliedFilter = appliedFilter,
                onAppliedFilterConsumed = {
                    backStackEntry.savedStateHandle[SCHEDULE_FILTER_RESULT_KEY] = null
                }
            )
        }

        composable<ScheduleFilterRoute> { entry ->
            val route = entry.toRoute<ScheduleFilterRoute>()
            ScheduleFilterScreen(
                initialFilter = route.filter,
                onSave = { filter ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SCHEDULE_FILTER_RESULT_KEY, filter)
                    navController.popBackStack()
                }
            )
        }

        composable<AddFriend>(
            enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
            exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left) }
        ) {
            AddFriendScreen(onBack = { navController.popBackStack() })
        }
    }
}
