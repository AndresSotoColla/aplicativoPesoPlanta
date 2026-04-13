package com.example.aplicativopesoplanta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aplicativopesoplanta.ui.screens.HistoryScreen
import com.example.aplicativopesoplanta.ui.screens.HomeScreen
import com.example.aplicativopesoplanta.ui.screens.SamplingFormScreen
import com.example.aplicativopesoplanta.ui.theme.AplicativoPesoPlantaTheme
import com.example.aplicativopesoplanta.ui.viewmodel.SamplingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AplicativoPesoPlantaTheme {
                val navController = rememberNavController()
                val viewModel: SamplingViewModel = viewModel(factory = SamplingViewModel.Factory)
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { 
                            HomeScreen(
                                onNavigateToForm = { navController.navigate("form") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onExport = { viewModel.exportToCsv(this@MainActivity) }
                            ) 
                        }
                        composable("form") { 
                            SamplingFormScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            ) 
                        }
                        composable("history") { 
                            HistoryScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            ) 
                        }
                    }
                }
            }
        }
    }
}