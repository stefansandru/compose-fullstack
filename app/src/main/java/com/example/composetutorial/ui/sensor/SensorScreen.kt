package com.example.composetutorial.ui.sensor

import android.hardware.Sensor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composetutorial.sensor.SensorHelper
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorHelper = remember { SensorHelper(context) }

    // Collect sensor data
    val accelerometerData by
            sensorHelper
                    .getAccelerometerFlow()
                    .collectAsState(initial = SensorHelper.AccelerometerData())

    val lightLevel by sensorHelper.getLightSensorFlow().collectAsState(initial = 0f)

    // Check sensor availability
    val hasAccelerometer = remember { sensorHelper.isSensorAvailable(Sensor.TYPE_ACCELEROMETER) }
    val hasLightSensor = remember { sensorHelper.isSensorAvailable(Sensor.TYPE_LIGHT) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Sensors Demo") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                )
            }
    ) { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Accelerometer Section
            AccelerometerCard(data = accelerometerData, isAvailable = hasAccelerometer)

            // Light Sensor Section
            LightSensorCard(lightLevel = lightLevel, isAvailable = hasLightSensor)

            // Tilt Indicator
            if (hasAccelerometer) {
                TiltIndicatorCard(data = accelerometerData)
            }

            // Available Sensors List
            AvailableSensorsCard(sensorHelper = sensorHelper)
        }
    }
}

@Composable
private fun AccelerometerCard(data: SensorHelper.AccelerometerData, isAvailable: Boolean) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                    text = "📱 Accelerometer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
            )

            if (isAvailable) {
                AccelerometerBar(label = "X", value = data.x, color = Color(0xFFE57373))
                AccelerometerBar(label = "Y", value = data.y, color = Color(0xFF81C784))
                AccelerometerBar(label = "Z", value = data.z, color = Color(0xFF64B5F6))
            } else {
                Text(
                        text = "Accelerometer not available on this device",
                        color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AccelerometerBar(label: String, value: Float, color: Color) {
    val animatedValue by
            animateFloatAsState(
                    targetValue = value,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "accelerometer_$label"
            )

    // Normalize value to 0-1 range (accelerometer typically ranges from -10 to 10)
    val normalizedValue = (animatedValue.absoluteValue / 10f).coerceIn(0f, 1f)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)

        Box(
                modifier =
                        Modifier.weight(1f)
                                .height(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                    modifier =
                            Modifier.fillMaxHeight()
                                    .fillMaxWidth(normalizedValue)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
            )
        }

        Text(
                text = String.format("%+.2f", animatedValue),
                modifier = Modifier.width(70.dp),
                style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LightSensorCard(lightLevel: Float, isAvailable: Boolean) {
    // Animate color based on light level
    val backgroundColor by
            animateColorAsState(
                    targetValue =
                            when {
                                lightLevel < 50 -> Color(0xFF1A237E) // Dark blue for low light
                                lightLevel < 200 -> Color(0xFF303F9F)
                                lightLevel < 500 -> Color(0xFF3F51B5)
                                lightLevel < 1000 -> Color(0xFFFFA726)
                                else -> Color(0xFFFFEB3B) // Bright yellow for high light
                            },
                    label = "light_color"
            )

    val textColor by
            animateColorAsState(
                    targetValue = if (lightLevel < 500) Color.White else Color.Black,
                    label = "text_color"
            )

    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                    text = "💡 Light Sensor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
            )

            if (isAvailable) {
                Text(
                        text = "${lightLevel.toInt()} lux",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                )

                Text(
                        text =
                                when {
                                    lightLevel < 50 -> "🌙 Very Dark"
                                    lightLevel < 200 -> "🌑 Dark"
                                    lightLevel < 500 -> "🏠 Indoor"
                                    lightLevel < 1000 -> "☁️ Cloudy"
                                    lightLevel < 10000 -> "🌤️ Daylight"
                                    else -> "☀️ Bright Sunlight"
                                },
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                )
            } else {
                Text(text = "Light sensor not available", color = Color.White)
            }
        }
    }
}

@Composable
private fun TiltIndicatorCard(data: SensorHelper.AccelerometerData) {
    val animatedX by
            animateFloatAsState(
                    targetValue = data.x.coerceIn(-5f, 5f) * -3f,
                    animationSpec = spring(dampingRatio = 0.6f),
                    label = "tilt_x"
            )
    val animatedY by
            animateFloatAsState(
                    targetValue = data.y.coerceIn(-5f, 5f) * 3f,
                    animationSpec = spring(dampingRatio = 0.6f),
                    label = "tilt_y"
            )

    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = "🎯 Tilt Indicator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                    modifier =
                            Modifier.size(200.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
            ) {
                // Crosshair lines
                Box(
                        modifier =
                                Modifier.width(180.dp)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                )
                Box(
                        modifier =
                                Modifier.width(1.dp)
                                        .height(180.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                )

                // Moving ball
                Box(
                        modifier =
                                Modifier.size(30.dp)
                                        .graphicsLayer {
                                            translationX = animatedX * 10
                                            translationY = animatedY * 10
                                        }
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "Tilt your device to move the ball",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvailableSensorsCard(sensorHelper: SensorHelper) {
    val sensors = remember { sensorHelper.getAllSensors() }

    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                    text = "📋 Available Sensors (${sensors.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
            )

            sensors.take(10).forEach { sensor ->
                Text(
                        text = "• ${sensor.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sensors.size > 10) {
                Text(
                        text = "... and ${sensors.size - 10} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
