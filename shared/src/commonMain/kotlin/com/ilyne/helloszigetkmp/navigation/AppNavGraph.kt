package com.ilyne.helloszigetkmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.ilyne.helloszigetkmp.presentation.feature.MainScaffold
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.AddFriendScreen
import com.ilyne.helloszigetkmp.presentation.feature.artistdetail.ArtistDetailScreen
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterScreen
import com.ilyne.helloszigetkmp.presentation.login.LoginScreen
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLParameter
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable object Login

@Serializable object Main

@Serializable object AddFriend

@Serializable data class ScheduleFilterRoute(val filter: ScheduleFilter)

@Serializable data class ArtistDetailRoute(val artistId: String)

private const val SCHEDULE_FILTER_RESULT_KEY = "scheduleFilterResult"

private val ScheduleFilterNavType = object : NavType<ScheduleFilter>(isNullableAllowed = false) {
    override fun get(bundle: SavedState, key: String): ScheduleFilter? =
        bundle.read { getStringOrNull(key) }?.let { Json.decodeFromString(it) }

    override fun put(bundle: SavedState, key: String, value: ScheduleFilter) {
        bundle.write { putString(key, Json.encodeToString(value)) }
    }

    override fun parseValue(value: String): ScheduleFilter =
        Json.decodeFromString(value.decodeURLPart())

    override fun serializeAsValue(value: ScheduleFilter): String =
        Json.encodeToString(value).encodeURLParameter()
}


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
            val appliedFilterJson by backStackEntry.savedStateHandle
                .getStateFlow<String?>(SCHEDULE_FILTER_RESULT_KEY, null)
                .collectAsStateWithLifecycle()

            MainScaffold(
                openFilterScreen = { filter -> navController.navigate(ScheduleFilterRoute(filter)) },
                onNavigateToAddFriend = { navController.navigate(AddFriend) },
                onArtistClick = { artistId -> navController.navigate(ArtistDetailRoute(artistId)) },
                appliedFilter = appliedFilterJson?.let { Json.decodeFromString(it) },
                onAppliedFilterConsumed = {
                    backStackEntry.savedStateHandle[SCHEDULE_FILTER_RESULT_KEY] = null
                },
                onLoggedOut = {
                    navController.navigate(Login) {
                        popUpTo<Main> { inclusive = true }
                    }
                }
            )
        }

        dialog<ScheduleFilterRoute>(
            typeMap = mapOf(typeOf<ScheduleFilter>() to ScheduleFilterNavType),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
        ) { entry ->
            val route = entry.toRoute<ScheduleFilterRoute>()
            ScheduleFilterScreen(
                initialFilter = route.filter,
                onSave = { filter ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SCHEDULE_FILTER_RESULT_KEY, Json.encodeToString(filter))
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
        }

        dialog<ArtistDetailRoute>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
        ) { entry ->
            val route = entry.toRoute<ArtistDetailRoute>()
            ArtistDetailScreen(
                artistId = route.artistId,
                onDismiss = { navController.popBackStack() }
            )
        }

        dialog<AddFriend>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AddFriendScreen(onDismiss = { navController.popBackStack() })
        }
    }
}
