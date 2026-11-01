package com.example.notacara.ui.navigation

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.notacara.NotacaraApplication
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.ui.screens.*
import com.example.notacara.ui.viewmodel.HomeViewModel
import com.example.notacara.ui.viewmodel.NotacaraViewModelFactory

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NotaList : Screen("nota_list/{acaraName}") {
        fun createRoute(acaraName: String) = "nota_list/$acaraName"
    }
    object Processing : Screen("processing/{acaraName}/{imageUri}") {
        fun createRoute(acaraName: String, imageUri: String) = "processing/$acaraName/${Uri.encode(imageUri)}"
    }
    object EditNota : Screen("edit_nota")
    object DetailNota : Screen("detail_nota")
    object Help : Screen("help")
}

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as NotacaraApplication
    val repository = app.repository
    
    val homeViewModel: HomeViewModel = viewModel(
        factory = NotacaraViewModelFactory(app, repository)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAcaraClick = { acaraName ->
                    navController.navigate(Screen.NotaList.createRoute(acaraName))
                },
                onHelpClick = {
                    navController.navigate(Screen.Help.route)
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        (context as? Activity)?.finish()
                    }
                },
                viewModel = homeViewModel
            )
        }
        composable(Screen.NotaList.route) { backStackEntry ->
            val acaraName = backStackEntry.arguments?.getString("acaraName") ?: ""
            NotaListScreen(
                acaraName = acaraName,
                onScannerResult = { uri ->
                    navController.navigate(Screen.Processing.createRoute(acaraName, uri.toString()))
                },
                onNotaClick = { notaId ->
                    homeViewModel.setCurrentNotaId(notaId)
                    navController.navigate(Screen.DetailNota.route)
                },
                onEditNota = {
                    navController.navigate(Screen.EditNota.route)
                },
                onHelpClick = {
                    navController.navigate(Screen.Help.route)
                },
                onBack = {
                    navController.popBackStack()
                },
                viewModel = homeViewModel
            )
        }
        composable(
            route = Screen.Processing.route,
            arguments = listOf(
                navArgument("acaraName") { type = NavType.StringType },
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val acaraName = backStackEntry.arguments?.getString("acaraName") ?: ""
            val imageUriStr = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = Uri.parse(imageUriStr)
            ProcessingScreen(
                imageUri = imageUri,
                onProcessingFinished = { result ->
                    homeViewModel.setAnalysisResult(result, imageUri, acaraName)
                    homeViewModel.setCurrentNotaId(null)
                    navController.navigate(Screen.EditNota.route) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.EditNota.route) {
            val uiState by homeViewModel.uiState.collectAsState()
            EditNotaScreen(
                analysisResult = uiState.lastAnalysisResult,
                imageUri = uiState.lastImageUri,
                isNewNota = uiState.currentNotaId == null,
                isFirstTimeEdit = uiState.isFirstTimeEdit,
                onDismissTour = { homeViewModel.completeFirstTimeEdit() },
                onSaveClick = { toko, tgl, disc, total, items, uri ->
                    homeViewModel.saveNota(toko, tgl, disc, total, items, uri) {
                        navController.navigate(Screen.DetailNota.route) {
                            popUpTo(Screen.NotaList.route) { inclusive = false }
                        }
                    }
                },
                onCancelClick = {
                    navController.popBackStack()
                },
                onReScan = { newUri ->
                    navController.navigate(Screen.Processing.createRoute(uiState.currentAcaraName, newUri.toString())) {
                        popUpTo(Screen.EditNota.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.DetailNota.route) {
            val uiState by homeViewModel.uiState.collectAsState()
            DetailNotaScreen(
                notaId = uiState.currentNotaId,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditClick = {
                    homeViewModel.prepareEditFromCurrentNota()
                    navController.navigate(Screen.EditNota.route)
                },
                viewModel = homeViewModel
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
