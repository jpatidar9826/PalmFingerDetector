package com.example.palmdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.palmdetector.model.BiometricStep
import com.example.palmdetector.model.HandType
import com.example.palmdetector.ui.screens.CameraScreen
import com.example.palmdetector.ui.screens.HandSelectionScreen
import com.example.palmdetector.ui.screens.HomeScreen
import com.example.palmdetector.ui.screens.PermissionScreen
import com.example.palmdetector.ui.screens.VerificationScreen
import com.example.palmdetector.ui.theme.PalmDetectorTheme
import com.example.palmdetector.viewmodel.BiometricViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PalmDetectorTheme {
                PalmDetectorApp()
            }
        }
    }
}

@Composable
fun PalmDetectorApp() {
    val navController = rememberNavController()
    val sharedViewModel: BiometricViewModel = viewModel()

    NavHost(navController = navController, startDestination = "permission_screen") {

        composable("permission_screen") {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate("home_screen") {
                        popUpTo("permission_screen") { inclusive = true }
                    }
                }
            )
        }

        composable("home_screen") {
            HomeScreen(
                viewModel = sharedViewModel,
                onAddBiometric = { navController.navigate("hand_selection_screen") },
                onVerify = { navController.navigate("verification_camera") }
            )
        }

        composable("hand_selection_screen") {
            HandSelectionScreen(
                viewModel = sharedViewModel,
                onStepClick = { hand, step ->
                    navController.navigate("camera_screen/${hand.name}/${step.name}")
                }
            )
        }

        composable(
            route = "camera_screen/{handType}/{stepName}",
            arguments = listOf(
                navArgument("handType") { type = NavType.StringType },
                navArgument("stepName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val handTypeStr = backStackEntry.arguments?.getString("handType") ?: "LEFT"
            val stepNameStr = backStackEntry.arguments?.getString("stepName") ?: "PALM"

            val currentHand = HandType.valueOf(handTypeStr)
            val currentStep = BiometricStep.valueOf(stepNameStr)

            CameraScreen(
                handType = currentHand,
                step = currentStep,
                viewModel = sharedViewModel,
                onCaptureSuccess = {
                    val nextStep = getNextBiometricStep(currentStep)

                    if (nextStep != null) {
                        navController.popBackStack()
                        navController.navigate("camera_screen/${currentHand.name}/${nextStep.name}")
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable("verification_camera") {
            VerificationScreen(
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

fun getNextBiometricStep(current: BiometricStep): BiometricStep? {
    return when (current) {
        BiometricStep.PALM -> BiometricStep.THUMB
        BiometricStep.THUMB -> BiometricStep.INDEX
        BiometricStep.INDEX -> BiometricStep.MIDDLE
        BiometricStep.MIDDLE -> BiometricStep.RING
        BiometricStep.RING -> BiometricStep.LITTLE
        BiometricStep.LITTLE -> null
    }
}