package com.example.mbnui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsSheet(
    currentGridRows: Int,
    currentGridCols: Int,
    onGridSizeChange: (Int, Int) -> Unit,
    onOpenWidgets: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E).copy(alpha = 0.98f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                "Home Screen",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOpenWidgets() }) {
                     Box(modifier = Modifier.size(48.dp).background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                         Icon(painterResource(android.R.drawable.ic_menu_add), null, tint = Color.White)
                     }
                     Spacer(modifier = Modifier.height(8.dp))
                     Text("Widgets", color = Color.White, fontSize = 12.sp)
                 }
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Box(modifier = Modifier.size(48.dp).background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                         Icon(painterResource(android.R.drawable.ic_menu_gallery), null, tint = Color.White)
                     }
                     Spacer(modifier = Modifier.height(8.dp))
                     Text("Wallpaper", color = Color.White, fontSize = 12.sp)
                 }
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Box(modifier = Modifier.size(48.dp).background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                          // Icon(painterResource(android.R.drawable.ic_theme), null, tint = Color.White) 
                          // themes icon isn't standard, use placeholder
                          Icon(painterResource(android.R.drawable.ic_menu_manage), null, tint = Color.White)
                     }
                     Spacer(modifier = Modifier.height(8.dp))
                     Text("Themes", color = Color.White, fontSize = 12.sp)
                 }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Grid",
                color = Color.White.copy(0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val gridOptions = listOf(
                4 to 5,
                4 to 6,
                5 to 5,
                5 to 6
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridOptions.forEach { (cols, rows) ->
                    val isSelected = cols == currentGridCols && rows == currentGridRows
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .background(
                                color = if (isSelected) Color(0xFF3D5AFE) else Color.White.copy(0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onGridSizeChange(cols, rows) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_dialog_dialer), // Placeholder icon
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.White.copy(0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${cols}x${rows}",
                                color = if (isSelected) Color.White else Color.White.copy(0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
             // More One UI Style options placeholders
            SettingsItem("Lock Home Screen Layout", false)
            SettingsItem("Add new apps to Home Screen", true)
            SettingsItem("Swipe down for notification panel", true)
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsItem(title: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { checked = !checked },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3D5AFE),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
