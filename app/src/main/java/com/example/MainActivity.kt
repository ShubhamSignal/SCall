package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthState
import com.example.viewmodel.SClabViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SClabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SClabApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SClabApp(viewModel: SClabViewModel) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val membership by viewModel.membership.collectAsState()

    val startDestination = when {
        authState is AuthState.Authenticated && userProfile != null -> {
            if (userProfile?.isFounder == true || membership?.tier?.isNotBlank() == true) {
                "home"
            } else {
                "membership"
            }
        }
        else -> "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    val profile = viewModel.userProfile.value
                    val mem = viewModel.membership.value
                    if (profile?.isFounder == true || mem?.tier?.isNotBlank() == true) {
                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                    } else {
                        navController.navigate("membership") { popUpTo("login") { inclusive = true } }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("membership") { popUpTo("register") { inclusive = true } }
                }
            )
        }
        composable("membership") {
            MembershipScreen(
                viewModel = viewModel,
                onMembershipActivated = {
                    navController.navigate("home") { popUpTo("membership") { inclusive = true } }
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onStartCall = { targetUserId, targetName, callType ->
                    navController.navigate("call/$targetName/$callType")
                },
                onOpenChat = { targetUserId, targetName ->
                    navController.navigate("chat_detail/$targetUserId/$targetName")
                },
                onNavigateWallet = { navController.navigate("wallet") },
                onNavigateProfile = { navController.navigate("profile") },
                onNavigateChats = { navController.navigate("chats") }
            )
        }
        composable("chats") {
            ChatListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenChat = { targetUserId, targetName ->
                    navController.navigate("chat_detail/$targetUserId/$targetName")
                }
            )
        }
        composable(
            route = "chat_detail/{targetUserId}/{targetName}",
            arguments = listOf(
                navArgument("targetUserId") { type = NavType.StringType },
                navArgument("targetName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
            val targetName = backStackEntry.arguments?.getString("targetName") ?: "User"
            ChatDetailScreen(
                viewModel = viewModel,
                targetUserId = targetUserId,
                targetName = targetName,
                onBack = { navController.popBackStack() },
                onStartAudioCall = { navController.navigate("call/$targetName/audio") },
                onStartVideoCall = { navController.navigate("call/$targetName/video") }
            )
        }
        composable("wallet") {
            WalletScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateBank = { navController.navigate("bank") }
            )
        }
        composable("bank") {
            BankDetailsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(
            route = "call/{targetName}/{callType}",
            arguments = listOf(
                navArgument("targetName") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val targetName = backStackEntry.arguments?.getString("targetName") ?: "User"
            val callType = backStackEntry.arguments?.getString("callType") ?: "audio"
            CallScreen(
                targetName = targetName,
                callType = callType,
                onEndCall = { navController.popBackStack() }
            )
        }
    }
}
