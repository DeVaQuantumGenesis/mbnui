package com.example.mbnui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgePanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(if (isVisible) 0f else -300f) }

    LaunchedEffect(isVisible) {
        scope.launch {
            offsetX.animateTo(
                targetValue = if (isVisible) 0f else -300f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    if (isVisible || offsetX.value > -300f) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(280.dp)
                .offset(x = offsetX.value.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E).copy(alpha = 0.95f),
                            Color(0xFF16213E).copy(alpha = 0.95f)
                        )
                    ),
                    RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                )
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Quick Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                // Weather Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(android.R.drawable.ic_menu_compass),
                            contentDescription = "Weather",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Weather", color = Color.White, fontSize = 14.sp)
                            Text("Sunny, 25°C", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }

                // Calculator
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { /* Open calculator */ },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(android.R.drawable.ic_menu_agenda),
                            contentDescription = "Calculator",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Calculator", color = Color.White, fontSize = 14.sp)
                    }
                }

                // Notes
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { /* Open notes */ },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(android.R.drawable.ic_menu_edit),
                            contentDescription = "Notes",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Notes", color = Color.White, fontSize = 14.sp)
                    }
                }

                // More tools can be added here
            }
        }
    }
}