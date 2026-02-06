package com.example.mbnui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.mbnui.ui.screens.HomeScreen
import com.example.mbnui.ui.theme.MbnuiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var appWidgetHost: android.appwidget.AppWidgetHost? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AppWidgetHost ID: 1024 is a random host ID
        appWidgetHost = android.appwidget.AppWidgetHost(applicationContext, 1024)
        
        enableEdgeToEdge()
        setContent {
            MbnuiTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        appWidgetHost?.let {
                            HomeScreen(appWidgetHost = it)
                        }
                    }
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        appWidgetHost?.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost?.stopListening()
    }
}
