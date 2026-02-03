package com.example.mbnui.ui.components

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.mbnui.data.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    providers: List<AppWidgetProviderInfo>,
    onDismiss: () -> Unit,
    onProviderSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            Text(
                "Select Widget",
                modifier = Modifier.padding(24.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(providers) { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProviderSelected(provider) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        val icon = provider.loadIcon(context, 0) // 0 for required density
                        if (icon != null) {
                            Image(
                                bitmap = icon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(modifier = Modifier.size(48.dp).background(Color.Gray.copy(0.3f), RoundedCornerShape(8.dp)))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = provider.loadLabel(context.packageManager),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${provider.minWidth}x${provider.minHeight} dp",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(0.1f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }
}
