package com.example.mbnui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search apps..."
) {
    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(24.dp).padding(start = 8.dp),
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassClock(
    modifier: Modifier = Modifier
) {
    var currentTime by androidx.compose.runtime.remember { 
        androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) 
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = now
            val nextMinute = ((now / 60000) + 1) * 60000
            kotlinx.coroutines.delay(nextMinute - System.currentTimeMillis())
        }
    }

    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime))
    val dateString = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(currentTime))

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeString,
            fontSize = 64.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
            color = Color.White
        )
        Text(
            text = dateString,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            color = Color.White.copy(0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // AOD-style additional info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weather placeholder
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_compass),
                    contentDescription = "Weather",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text("22°C", fontSize = 14.sp, color = Color.White)
            }
            // Notifications placeholder
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_popup_reminder),
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text("3", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
