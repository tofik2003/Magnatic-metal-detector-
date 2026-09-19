package com.metaldetector.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaldetector.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    sensorInfo: String,
    onNavigatePrivacy: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Guidelines", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
            // Alert Preferences
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "DETECTION FEEDBACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Audio Beep Alert", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Dynamic frequency scales with magnetic anomaly", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = DarkSurfaceElevated)
                        )
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Vibration Alert", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Vibration pulse frequency tracks signal strength", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = onToggleVibration,
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = DarkSurfaceElevated)
                        )
                    }
                }
            }

            // User Operating Steps
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "BEST PRACTICE SCANNING STEPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )

                    val steps = listOf(
                        "1. Remove magnetic cases, MagSafe accessories, or metal grips.",
                        "2. Calibrate in the air away from metal appliances.",
                        "3. Hold phone 1 to 5 cm directly above target surface.",
                        "4. Move slowly across the area in a sweeping motion.",
                        "5. Cross-verify strong signals from orthogonal directions (90°).",
                        "6. Always use a dedicated professional scanner before drilling."
                    )

                    steps.forEach { step ->
                        Text(step, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }

            // Hardware & Diagnostics
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "DEVICE HARDWARE SENSOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(sensorInfo, color = Color.White, fontSize = 13.sp)
                    Text("Sampling Rate: 30 Hz continuous (SensorManager.SENSOR_DELAY_GAME)", color = Color.Gray, fontSize = 12.sp)
                    Text("Filtering: Exponential Moving Average (alpha = 0.2)", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Privacy & Security Button
            OutlinedButton(
                onClick = onNavigatePrivacy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AccentGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Privacy & Security Architecture", fontWeight = FontWeight.SemiBold)
            }

            // Disclaimer & Limitations
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF231713)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Important Disclaimers", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        "• Senses magnetic-field variations only (ferromagnetic materials: iron, steel, rebar, nickel).\n" +
                        "• Non-ferrous metals (copper pipes, aluminum foil, gold, brass) cannot be reliably detected.\n" +
                        "• Cannot distinguish between electrical conduit, steel rebar, and water pipes.\n" +
                        "• Do not rely on this app for structural or electrical safety decisions.",
                        color = Color(0xFFFFD4B2),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
