package com.ilyne.hello_sziget_kmp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ilyne.hello_sziget_kmp.presentation.login.LoginScreen
import com.ilyne.hello_sziget_kmp.presentation.MainScaffold
import kotlinx.serialization.Serializable

@Serializable object Login
@Serializable object Main

@Composable
fun AppNavGraph(startDestination: Any = Login) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Main) {
                    popUpTo<Login> { inclusive = true }
                }
            })
        }
        composable<Main> {
            MainScaffold()
        }
    }
}
