package com.example.mbnui.ui.components

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var searchQuery by remember { mutableStateOf("") }

    // プロバイダーをアプリ名でグループ化し、ソートする
    val groupedProviders = remember(providers, searchQuery) {
        providers
            .filter {
                val label = it.loadLabel(context.packageManager)
                label.contains(searchQuery, ignoreCase = true)
            }
            .groupBy { it.loadLabel(context.packageManager) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    // 展開されているアプリの状態管理
    var expandedApp by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101010).copy(alpha = 0.98f), // One UIらしい深い黒
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 750.dp)
        ) {
            // Header & Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    "Widgets",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )

                // One UI style Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF252525), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search widgets", color = Color(0xFF9E9E9E), fontSize = 16.sp)
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                // セクションヘッダー（画像にあるようなリスト構造を模倣）
                item {
                    Text(
                        "All Apps",
                        color = Color(0xFF9E9E9E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                groupedProviders.forEach { (appName, appWidgets) ->
                    val isExpanded = expandedApp == appName

                    item {
                        Column {
                            // アプリ行アイテム
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedApp = if (isExpanded) null else appName
                                    }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon
                                val icon = appWidgets.firstOrNull()?.loadIcon(context, 0)
                                if (icon != null) {
                                    Image(
                                        bitmap = icon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp)) // アイコンを少し角丸に
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.Gray.copy(0.3f), RoundedCornerShape(8.dp))
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = appName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )

                                // Count badge
                                Text(
                                    text = appWidgets.size.toString(),
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF9E9E9E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 展開時のプレビューエリア
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF181818)) // 展開時は少し背景を変える
                                        .padding(bottom = 16.dp)
                                ) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(appWidgets) { widget ->
                                            WidgetPreviewCard(
                                                widget = widget,
                                                onClick = { onProviderSelected(widget) }
                                            )
                                        }
                                    }
                                }
                            }

                            // 区切り線
                            if (!isExpanded) {
                                HorizontalDivider(
                                    color = Color.White.copy(0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(start = 72.dp, end = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewCard(
    widget: AppWidgetProviderInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF2C2C2C), RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            // プレビュー画像の取得を試みる
            val preview = widget.loadPreviewImage(context, 0)
            val icon = widget.loadIcon(context, 0)

            if (preview != null) {
                Image(
                    bitmap = preview.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit, // 全体が見えるようにFitにする
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            } else if (icon != null) {
                // プレビューがない場合はアイコンを大きく表示
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Text("Widget", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget Label
        Text(
            text = widget.loadLabel(context.packageManager),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Size
        Text(
            text = "${((widget.minWidth + 30) / 70).coerceAtLeast(1)} x ${((widget.minHeight + 30) / 70).coerceAtLeast(1)}", // 概算セルサイズ
            color = Color(0xFF9E9E9E),
            fontSize = 11.sp
        )
    }
}
