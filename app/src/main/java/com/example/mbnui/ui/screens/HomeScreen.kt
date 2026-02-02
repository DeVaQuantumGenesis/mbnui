package com.example.mbnui.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mbnui.data.AppInfo
import com.example.mbnui.ui.LauncherViewModel
import com.example.mbnui.ui.components.GlassBox
import com.example.mbnui.ui.components.GlassClock
import com.example.mbnui.ui.components.GlassSearchBar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.*
import com.example.mbnui.ui.components.DockItem
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Intent
import android.provider.Settings

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val apps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    // Drawer state
    var drawerProgress by remember { mutableStateOf(1f) } // 1f = closed, 0f = opened
    val animatedProgress by animateFloatAsState(
        targetValue = drawerProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drawer_animation"
    )

    val drawerOffsetY = animatedProgress * screenHeight.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 1.5f
                        val delta = (dragAmount / density.density) / screenHeight.value
                        drawerProgress = (drawerProgress + delta * sensitivity).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        drawerProgress = if (drawerProgress < 0.5f) 0f else 1f
                        if (drawerProgress == 0f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
    ) {
        // Aesthetic Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460)
                        )
                    )
                )
        )
        
        // Home Screen Content (Workspace)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .alpha(1f - (1f - animatedProgress) * 0.8f) // Fade out when drawer opens
                .scale(1f - (1f - animatedProgress) * 0.05f) // Slight shrink
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            GlassClock(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                try {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback if ACTION_HOME_SETTINGS is not available
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
            )
            
            Spacer(modifier = Modifier.weight(1f))

            // Stationary Dock at the bottom of the workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassBox(
                    modifier = Modifier
                        .width(320.dp)
                        .height(96.dp),
                    cornerRadius = 28.dp,
                    isDark = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Show first 4 apps as docked apps
                        val dockApps = apps.take(4)
                        dockApps.forEach { app ->
                            DockItem(app) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                app.launchIntent?.let { intent ->
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
        }

        // App Drawer (Overlays the Home Screen)
        AppDrawer(
            apps = apps,
            offsetY = drawerOffsetY,
            onClose = { 
                drawerProgress = if (drawerProgress < 0.5f) 0f else 1f 
            },
            onDrag = { dragAmount ->
                val delta = (dragAmount / density.density) / screenHeight.value
                drawerProgress = (drawerProgress + delta * 1.5f).coerceIn(0f, 1f)
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
        )

        // Swipe Indicator
        if (drawerProgress > 0.9f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    }
}
