package com.memoriabox.ui.screen.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.memoriabox.utils.ColorUtils
import kotlinx.coroutines.delay

val PRESET_COLORS = listOf(
    Color(0xFF7C4DFF), Color(0xFFFF4081), Color(0xFF00BCD4), Color(0xFFFF9800),
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFE91E63),
    Color(0xFF009688), Color(0xFFFF5722), Color(0xFF607D8B), Color(0xFF795548),
    Color(0xFFF48FB1), Color(0xFFCE93D8), Color(0xFF9FA8DA), Color(0xFF80CBC4),
    Color(0xFFFFF59D), Color(0xFFA5D6A7), Color(0xFFEF9A9A), Color(0xFFBCAAA4),
    Color(0xFFE0E0E0), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFF90A4AE)
)

val MORANDI_COLORS = listOf(
    Color(0xFFB8A9C9), Color(0xFFC9B8A9), Color(0xFFA9B8C9), Color(0xFFC9A9B8),
    Color(0xFFB8C9A9), Color(0xFFA9C9B8), Color(0xFFD4C5B5), Color(0xFFB5D4C5),
    Color(0xFFC5B5D4), Color(0xFFE8D5C4), Color(0xFFC4E8D5), Color(0xFFD5C4E8),
    Color(0xFFC8B8A8), Color(0xFFA8C8B8), Color(0xFFB8A8C8), Color(0xFFD8C8B8)
)

@Composable
fun ColorPickerDialog(
    initialColor: String = "#7C4DFF",
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(ColorUtils.hexToColor(initialColor)) }
    var selectedMode by remember { mutableStateOf(0) }
    var hue by remember { mutableFloatStateOf(270f) }
    var saturation by remember { mutableFloatStateOf(0.7f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var red by remember { mutableIntStateOf((selectedColor.red * 255).toInt()) }
    var green by remember { mutableIntStateOf((selectedColor.green * 255).toInt()) }
    var blue by remember { mutableIntStateOf((selectedColor.blue * 255).toInt()) }

    LaunchedEffect(selectedColor) {
        red = (selectedColor.red * 255).toInt()
        green = (selectedColor.green * 255).toInt()
        blue = (selectedColor.blue * 255).toInt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 },
                        label = { Text("HSV") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        label = { Text("RGB") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMode == 2,
                        onClick = { selectedMode = 2 },
                        label = { Text("预设") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedMode) {
                    0 -> HsvColorPicker(
                        hue = hue,
                        saturation = saturation,
                        brightness = brightness,
                        onHueChange = { hue = it },
                        onSaturationChange = { saturation = it },
                        onBrightnessChange = { brightness = it },
                        onColorChanged = { selectedColor = it }
                    )
                    1 -> RgbSliders(
                        red = red,
                        green = green,
                        blue = blue,
                        onRedChange = { red = it },
                        onGreenChange = { green = it },
                        onBlueChange = { blue = it },
                        onColorChanged = { selectedColor = it }
                    )
                    2 -> PresetColors(
                        onColorSelected = { selectedColor = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = ColorUtils.colorToHex(selectedColor),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelected(ColorUtils.colorToHex(selectedColor)) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun HsvColorPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onColorChanged: (Color) -> Unit
) {
    var boxWidth by remember { mutableFloatStateOf(300f) }
    var boxHeight by remember { mutableFloatStateOf(200f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x
                        val y = change.position.y
                        val newSat = (x / boxWidth).coerceIn(0f, 1f)
                        val newBright = (1f - y / boxHeight).coerceIn(0f, 1f)
                        onSaturationChange(newSat)
                        onBrightnessChange(newBright)
                        onColorChanged(Color.hsv(hue, newSat * 100, newBright * 100))
                    }
                }
                .onSizeChanged { size ->
                    boxWidth = size.width.toFloat()
                    boxHeight = size.height.toFloat()
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color.hsv(hue, 0f, 100f),
                            Color.hsv(hue, 100f, 100f)
                        )
                    )
                )
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Black)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("色相", modifier = Modifier.width(48.dp))
            var sliderWidth by remember { mutableFloatStateOf(0f) }
            Slider(
                value = hue,
                onValueChange = { 
                    onHueChange(it)
                    onColorChanged(Color.hsv(it, saturation * 100, brightness * 100))
                },
                valueRange = 0f..360f,
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { sliderWidth = it.width.toFloat() },
                colors = SliderDefaults.colors(
                    thumbColor = Color.hsv(hue, 100f, 100f),
                    activeTrackColor = Color.hsv(hue, 80f, 80f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("饱和度", modifier = Modifier.width(48.dp))
            Slider(
                value = saturation,
                onValueChange = { 
                    onSaturationChange(it)
                    onColorChanged(Color.hsv(hue, it * 100, brightness * 100))
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("亮度", modifier = Modifier.width(48.dp))
            Slider(
                value = brightness,
                onValueChange = { 
                    onBrightnessChange(it)
                    onColorChanged(Color.hsv(hue, saturation * 100, it * 100))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RgbSliders(
    red: Int,
    green: Int,
    blue: Int,
    onRedChange: (Int) -> Unit,
    onGreenChange: (Int) -> Unit,
    onBlueChange: (Int) -> Unit,
    onColorChanged: (Color) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("R", modifier = Modifier.width(48.dp))
            Slider(
                value = red.toFloat(),
                onValueChange = { 
                    val r = it.toInt()
                    onRedChange(r)
                    onColorChanged(Color(r / 255f, green / 255f, blue / 255f))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color(0xFFFF5252)
                )
            )
            Text(red.toString(), modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("G", modifier = Modifier.width(48.dp))
            Slider(
                value = green.toFloat(),
                onValueChange = { 
                    val g = it.toInt()
                    onGreenChange(g)
                    onColorChanged(Color(red / 255f, g / 255f, blue / 255f))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Green,
                    activeTrackColor = Color(0xFF4CAF50)
                )
            )
            Text(green.toString(), modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("B", modifier = Modifier.width(48.dp))
            Slider(
                value = blue.toFloat(),
                onValueChange = { 
                    val b = it.toInt()
                    onBlueChange(b)
                    onColorChanged(Color(red / 255f, green / 255f, b / 255f))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Blue,
                    activeTrackColor = Color(0xFF2196F3)
                )
            )
            Text(blue.toString(), modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
fun PresetColors(
    onColorSelected: (Color) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("莫兰迪色系", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.height(80.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(MORANDI_COLORS) { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(color) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("预设颜色", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.height(240.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(PRESET_COLORS) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}
