package com.example.colorpick

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.colorpick.ui.CameraScreen
import com.example.colorpick.ui.CropScreen
import com.example.colorpick.ui.GalleryScreen
import com.example.colorpick.ui.theme.ColorPickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorPickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    var currentRoute by rememberSaveable { mutableStateOf<String>("camera") }
    var cropUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    when (currentRoute) {
        "camera" -> CameraScreen(
            onNavigateToCrop = { uri ->
                cropUri = uri
                currentRoute = "crop"
            },
            onNavigateToGallery = {
                currentRoute = "gallery"
            }
        )
        "crop" -> cropUri?.let { uri ->
            CropScreen(
                photoUri = uri,
                onBack = { currentRoute = "camera" }
            )
        } ?: run {
            currentRoute = "camera"
        }
        "gallery" -> GalleryScreen(
            onBack = { currentRoute = "camera" }
        )
    }
}
