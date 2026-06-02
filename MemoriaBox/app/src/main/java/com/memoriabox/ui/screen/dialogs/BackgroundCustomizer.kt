package com.memoriabox.ui.screen.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.memoriabox.utils.ColorUtils

@Composable
fun BackgroundCustomizerDialog(
    initialBgType: com.memoriabox.data.model.BgType,
    initialBgValue: String,
    onDismiss: () -> Unit,
    onSave: (bgType: com.memoriabox.data.model.BgType, bgValue: String) -> Unit
) {
    val context = LocalContext.current
    var selectedBgType by remember { mutableStateOf(initialBgType) }
    var selectedBgValue by remember { mutableStateOf(initialBgValue) }
    var blurLevel by remember { mutableFloatStateOf(0f) }
    var overlayAlpha by remember { mutableFloatStateOf(0f) }
    var showColorPicker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedBgType = com.memoriabox.data.model.BgType.IMAGE
            selectedBgValue = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义背景") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedBgType == com.memoriabox.data.model.BgType.COLOR) {
                                ColorUtils.hexToColor(selectedBgValue)
                            } else {
                                Color.Gray
                            }
                        )
                ) {
                    if (selectedBgType == com.memoriabox.data.model.BgType.IMAGE) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .blur(blurLevel.dp)
                                .background(Color.Gray)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = overlayAlpha))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择图片")
                    }
                    OutlinedButton(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择颜色")
                    }
                }

                if (selectedBgType == com.memoriabox.data.model.BgType.IMAGE) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("高斯模糊", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = blurLevel,
                            onValueChange = { blurLevel = it },
                            valueRange = 0f..20f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${blurLevel.toInt()}px", modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("暗色遮罩", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = overlayAlpha,
                            onValueChange = { overlayAlpha = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(overlayAlpha * 100).toInt()}%", modifier = Modifier.width(48.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedBgType, selectedBgValue) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedBgValue,
            onDismiss = { showColorPicker = false },
            onSelected = { color ->
                selectedBgValue = color
                selectedBgType = com.memoriabox.data.model.BgType.COLOR
                showColorPicker = false
            }
        )
    }
}

@Composable
fun CardStyleDialog(
    initialStyleJson: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textColor by remember { mutableStateOf("#000000") }
    var cornerRadius by remember { mutableFloatStateOf(16f) }
    var shadowSize by remember { mutableFloatStateOf(4f) }
    var showLunar by remember { mutableStateOf(true) }
    var showNote by remember { mutableStateOf(true) }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("卡片样式设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("文字颜色", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorUtils.hexToColor(textColor))
                            .clickable { showColorPicker = true }
                    )
                    Text(textColor, modifier = Modifier.padding(start = 8.dp))
                    OutlinedButton(onClick = { showColorPicker = true }) {
                        Text("选择")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("圆角半径", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = cornerRadius,
                        onValueChange = { cornerRadius = it },
                        valueRange = 0f..40f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${cornerRadius.toInt()}dp", modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("阴影大小", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = shadowSize,
                        onValueChange = { shadowSize = it },
                        valueRange = 0f..16f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${shadowSize.toInt()}dp", modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showLunar,
                        onCheckedChange = { showLunar = it }
                    )
                    Text("显示农历")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showNote,
                        onCheckedChange = { showNote = it }
                    )
                    Text("显示备注")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val styleJson = """{"textColor":"$textColor","cornerRadius":$cornerRadius,"shadowSize":$shadowSize,"showLunar":$showLunar,"showNote":$showNote}"""
                    onSave(styleJson)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = textColor,
            onDismiss = { showColorPicker = false },
            onSelected = { color ->
                textColor = color
                showColorPicker = false
            }
        )
    }
}
