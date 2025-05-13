package com.example.gestionintelligentedesabsences.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.gestionintelligentedesabsences.data.model.UserRole
import com.example.gestionintelligentedesabsences.ui.screens.dashboard.AdminDashboard
import com.example.gestionintelligentedesabsences.ui.screens.dashboard.StudentDashboard
import com.example.gestionintelligentedesabsences.ui.screens.dashboard.ProfessorDashboard
import com.example.gestionintelligentedesabsences.ui.screens.dashboard.SuperAdminDashboard
import com.example.gestionintelligentedesabsences.ui.screens.login.LoginScreen
import com.example.gestionintelligentedesabsences.ui.screens.management.ProfileCaptureScreen
import com.example.gestionintelligentedesabsences.ui.screens.management.UserManagement

// Define the navigation routes
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StudentDashboard : Screen("student_dashboard")
    object ProfessorDashboard : Screen("teacher_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object SuperAdminDashboard : Screen("super_admin_dashboard")

    // Management screens for Super Admin
    object UserManagement : Screen("user_management")
    object CourseManagement : Screen("course_management")
    object ScheduleManagement : Screen("schedule_management")

    // New profile capture route with argument
    object ProfileCapture : Screen("profile_capture/{userId}") {
        fun createRoute(userId: String) = "profile_capture/$userId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { userRole ->
                    val route = when (userRole) {
                        UserRole.STUDENT -> Screen.StudentDashboard.route
                        UserRole.TEACHER -> Screen.ProfessorDashboard.route
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                        UserRole.SUPER_ADMIN -> Screen.SuperAdminDashboard.route
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Placeholder screens - you'll implement these later
        composable(Screen.StudentDashboard.route) {
            StudentDashboard(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboard(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProfessorDashboard.route) {
            ProfessorDashboard(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SuperAdminDashboard.route) {
            SuperAdminDashboard(
                onNavigateToUserManagement = {
                    navController.navigate(Screen.UserManagement.route)
                },
                onNavigateToCourseManagement = {
                    navController.navigate(Screen.CourseManagement.route)
                },
                onNavigateToScheduleManagement = {
                    navController.navigate(Screen.ScheduleManagement.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SuperAdminDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Management screens
        composable(Screen.UserManagement.route) {
            UserManagement(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfileCapture = { userId ->
                    // Navigate to profile capture screen with the userId
                    navController.navigate(Screen.ProfileCapture.createRoute(userId))
                }
            )
        }

        // Add the profile capture screen with the userId parameter
        composable(
            route = Screen.ProfileCapture.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileCaptureScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CourseManagement.route) {
            // CourseManagementScreen(
            //     onBack = { navController.popBackStack() }
            // )
        }

        composable(Screen.ScheduleManagement.route) {
            // ScheduleManagementScreen(
            //     onBack = { navController.popBackStack() }
            // )
        }
    }
}