package com.metaldetector.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaldetector.app.detection.AlertLevel
import com.metaldetector.app.detection.DetectionState
import com.metaldetector.app.sensor.MagneticReading
import com.metaldetector.app.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectorScreen(
    currentReading: MagneticReading,
    currentState: DetectionState,
    historyGraphData: List<Float>,
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onSaveScanSession: () -> Unit,
    onBack: () -> Unit
) {
    val animatedPercent by animateFloatAsState(targetValue = currentState.signalStrengthPercent, label = "meter")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Metal Detector", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                actions = {
                    IconButton(onClick = { onToggleSound(!isSoundEnabled) }) {
                        Icon(
                            if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Alert",
                            tint = if (isSoundEnabled) AccentGold else Color.Gray
                        )
                    }
                    IconButton(onClick = { onToggleVibration(!isVibrationEnabled) }) {
                        Icon(
                            if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.Smartphone,
                            contentDescription = "Vibration Alert",
                            tint = if (isVibrationEnabled) AccentGold else Color.Gray
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Radial Analog Gauge Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        AnalogGaugeCanvas(
                            percentage = animatedPercent,
                            isTriggered = currentState.isTriggered,
                            alertLevel = currentState.alertLevel
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 70.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", currentState.filteredMagnitude),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentState.isTriggered) AccentRed else Color.White
                            )
                            Text(
                                "µT (microtesla)",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Badge(
                                containerColor = when (currentState.alertLevel) {
                                    AlertLevel.STRONG -> AccentRed
                                    AlertLevel.MODERATE -> AccentOrange
                                    AlertLevel.WEAK -> AccentGold
                                    AlertLevel.NONE -> DarkSurfaceElevated
                                }
                            ) {
                                Text(
                                    text = if (currentState.isTriggered) "SIGNAL: +${String.format(Locale.US, "%.1f", currentState.deviation)} µT" else "QUIET",
                                    color = if (currentState.alertLevel == AlertLevel.NONE) Color.Gray else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 3-Axis breakdown
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("X: ${String.format(Locale.US, "%.1f", currentReading.x)}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Y: ${String.format(Locale.US, "%.1f", currentReading.y)}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Z: ${String.format(Locale.US, "%.1f", currentReading.z)}", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            // Real-time Waveform Oscilloscope Graph Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LIVE SIGNAL GRAPH (µT over time)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Base: ${String.format(Locale.US, "%.1f", currentState.baseline)}",
                            fontSize = 11.sp,
                            color = AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LiveSignalGraphCanvas(
                        data = historyGraphData,
                        baseline = currentState.baseline,
                        threshold = currentState.threshold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }

            // Diagnostic & Baseline Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Noise Level", color = Color.Gray, fontSize = 11.sp)
                        Text("${String.format(Locale.US, "%.2f", currentState.noise)} µT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Trigger Point", color = Color.Gray, fontSize = 11.sp)
                        Text("${String.format(Locale.US, "%.1f", currentState.threshold)} µT", color = AccentGold, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Release Point", color = Color.Gray, fontSize = 11.sp)
                        Text("${String.format(Locale.US, "%.1f", currentState.threshold * 0.6f)} µT", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Save Scan Session Button
            OutlinedButton(
                onClick = onSaveScanSession,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Current Session to History", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnalogGaugeCanvas(
    percentage: Float,
    isTriggered: Boolean,
    alertLevel: AlertLevel,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val sweepAngle = 200f
        val startAngle = 170f
        val arcStroke = 18f
        val radius = size.minDimension / 1.7f
        val center = Offset(size.width / 2f, size.height * 0.75f)

        // Background track arc
        drawArc(
            color = Color(0xFF26334A),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = arcStroke, cap = StrokeCap.Round)
        )

        // Active value sweep arc
        val activeSweep = (percentage / 100f) * sweepAngle
        val activeBrush = Brush.sweepGradient(
            colors = listOf(AccentCyan, AccentGold, AccentOrange, AccentRed),
            center = center
        )

        drawArc(
            brush = activeBrush,
            startAngle = startAngle,
            sweepAngle = activeSweep.coerceAtLeast(1f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = arcStroke, cap = StrokeCap.Round)
        )

        // Needle indicator
        val angleRad = Math.toRadians((startAngle + activeSweep).toDouble())
        val needleLength = radius * 0.88f
        val endX = center.x + (needleLength * cos(angleRad)).toFloat()
        val endY = center.y + (needleLength * sin(angleRad)).toFloat()

        drawLine(
            color = if (isTriggered) AccentRed else AccentGold,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color.White,
            radius = 7f,
            center = center
        )
    }
}

@Composable
fun LiveSignalGraphCanvas(
    data: List<Float>,
    baseline: Float,
    threshold: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        // Determine min and max scale
        val minVal = maxOf(0f, (baseline - threshold * 2))
        val maxVal = maxOf(baseline + threshold * 5, (data.maxOrNull() ?: (baseline + 10f)) + 2f)
        val range = (maxVal - minVal).coerceAtLeast(1f)

        // Draw baseline horizontal dashed guide
        val baselineY = h - ((baseline - minVal) / range) * h
        drawLine(
            color = Color(0xFF3B82F6),
            start = Offset(0f, baselineY),
            end = Offset(w, baselineY),
            strokeWidth = 2f
        )

        // Draw threshold line
        val triggerY = h - ((baseline + threshold - minVal) / range) * h
        drawLine(
            color = Color(0x80EF4444),
            start = Offset(0f, triggerY),
            end = Offset(w, triggerY),
            strokeWidth = 2f
        )

        // Plot path
        val path = Path()
        val step = w / maxOf(1, data.size - 1)

        data.forEachIndexed { i, value ->
            val x = i * step
            val y = (h - ((value - minVal) / range) * h).coerceIn(0f, h)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = AccentCyan,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
    }
}
