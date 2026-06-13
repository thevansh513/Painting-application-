package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.DrawingRepository
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.FeatureLabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DrawingViewModel
import com.example.ui.viewmodel.DrawingViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Offline database & repository initialization
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DrawingRepository(applicationContext, database.drawingDao())
        val factory = DrawingViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[DrawingViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    var currentScreen by remember { mutableStateOf("gallery") }
                    var previousScreenBeforeLab by remember { mutableStateOf("gallery") }

                    when (currentScreen) {
                        "gallery" -> {
                            GalleryScreen(
                                viewModel = viewModel,
                                onOpenDrawing = { id ->
                                    viewModel.loadDrawing(id)
                                    currentScreen = "canvas"
                                },
                                onStartNewDrawing = {
                                    viewModel.startNewDrawing()
                                    currentScreen = "canvas"
                                },
                                onOpenFeatureLab = {
                                    previousScreenBeforeLab = "gallery"
                                    currentScreen = "feature_lab"
                                }
                            )
                        }
                        "canvas" -> {
                            CanvasScreen(
                                viewModel = viewModel,
                                onBackToGallery = {
                                    currentScreen = "gallery"
                                },
                                onOpenFeatureLab = {
                                    previousScreenBeforeLab = "canvas"
                                    currentScreen = "feature_lab"
                                }
                            )
                        }
                        "feature_lab" -> {
                            FeatureLabScreen(
                                onBack = {
                                    currentScreen = previousScreenBeforeLab
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

