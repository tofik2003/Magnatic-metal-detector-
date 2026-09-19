package com.metaldetector.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaldetector.app.detection.AlertLevel
import com.metaldetector.app.detection.CalibrationData
import com.metaldetector.app.detection.DetectionState
import com.metaldetector.app.sensor.MagneticReading
import com.metaldetector.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isSensorAvailable: Boolean,
    sensorInfo: String,
    currentState: DetectionState,
    calibrationData: CalibrationData?,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Metal Detector",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                ),
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LIVE SENSOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSensorAvailable) AccentGreen else AccentRed
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isSensorAvailable) "READY (30 Hz)" else "MISSING",
                                color = if (isSensorAvailable) AccentGreen else AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%.1f", currentState.filteredMagnitude),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentState.isTriggered) AccentRed else Color.White
                            )
                            Text(
                                "Microtesla (µT)",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (currentState.isTriggered) "TARGET DETECTED" else "SURFACE CLEAR",
                                color = if (currentState.isTriggered) AccentRed else AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Anomaly: ${String.format(Locale.US, "+%.1f µT", currentState.deviation)}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Linear Indicator Meter
                    LinearProgressIndicator(
                        progress = { (currentState.signalStrengthPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when (currentState.alertLevel) {
                            AlertLevel.STRONG -> AccentRed
                            AlertLevel.MODERATE -> AccentOrange
                            AlertLevel.WEAK -> AccentGold
                            AlertLevel.NONE -> AccentCyan
                        },
                        trackColor = DarkSurfaceElevated
                    )
                }
            }

            // Quick Actions Grid
            Text(
                "DETECTION MODES",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(
                    title = "Live Detector",
                    subtitle = "Needle & Graph",
                    icon = Icons.Default.Sensors,
                    accentColor = AccentGold,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("detector")
                }
                ModeCard(
                    title = "Scan Grid",
                    subtitle = "Wall & Grass Mapping",
                    icon = Icons.Default.GridOn,
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("grid")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(
                    title = "Calibration",
                    subtitle = "Ambient Baseline",
                    icon = Icons.Default.Tune,
                    accentColor = AccentGreen,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("calibration")
                }
                ModeCard(
                    title = "Scan History",
                    subtitle = "Saved Logs",
                    icon = Icons.Default.History,
                    accentColor = AccentOrange,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("history")
                }
            }

            // Target Compatibility Reference
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "METALS DETECTABILITY GUIDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Works With", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val detectedList = listOf("Iron", "Steel", "Nails & Screws", "Rebar", "Steel Pipes", "Some Keys")
                            detectedList.forEach { item ->
                                Text("• $item", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Does Not Detect", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val notDetectedList = listOf("Gold & Silver", "Copper", "Aluminum", "Brass", "PVC Pipes", "Plastics")
                            notDetectedList.forEach { item ->
                                Text("• $item", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Safety Warning Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1810)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "SAFETY NOTICE: This app senses magnetic-field variations. It cannot guarantee wall safety, live AC wire detection, or metal depth.",
                        color = Color(0xFFFFD8BF),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
