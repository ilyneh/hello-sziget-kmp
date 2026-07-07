package com.ilyne.helloszigetkmp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ilyne.helloszigetkmp.presentation.feature.MainScaffold
import com.ilyne.helloszigetkmp.presentation.addfriend.AddFriendScreen
import com.ilyne.helloszigetkmp.presentation.login.LoginScreen
import kotlinx.serialization.Serializable

@Serializable object Login

@Serializable object Main

@Serializable object AddFriend

@Composable
fun AppNavGraph(startDestination: Any = Login) {
    val navController = rememberNavController()

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
        composable<Main> {
            MainScaffold(onNavigateToAddFriend = { navController.navigate(AddFriend) })
        }
        composable<AddFriend>(
            enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right) },
            exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left) }
        ) {
            AddFriendScreen(onBack = { navController.popBackStack() })
        }
    }
}
