package com.metaldetector.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaldetector.app.detection.CalibrationData
import com.metaldetector.app.detection.DetectionState
import com.metaldetector.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    isCalibrating: Boolean,
    calibrationProgress: Float,
    samplesGathered: Int,
    calibrationData: CalibrationData?,
    currentState: DetectionState,
    onStartCalibration: () -> Unit,
    onCancelCalibration: () -> Unit,
    onBack: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = calibrationProgress, label = "progress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Calibration", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Instruction Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PRE-CALIBRATION CHECKLIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                    StepItem(number = "1", text = "Remove magnetic phone cases or metal ring stands.")
                    StepItem(number = "2", text = "Hold the phone in mid-air, at least 1 meter away from computers, speakers, or large metal.")
                    StepItem(number = "3", text = "Keep phone still while 60 background samples are captured (~2 seconds).")
                }
            }

            // Progress & Baseline Status
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        CircularProgressIndicator(
                            progress = { if (isCalibrating) animatedProgress else 1f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = if (isCalibrating) AccentGold else AccentGreen,
                            trackColor = DarkSurfaceElevated
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isCalibrating) "${(calibrationProgress * 100).toInt()}%" else "READY",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (isCalibrating) "$samplesGathered / 60 samples" else "Calibrated",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Baseline", fontSize = 11.sp, color = Color.LightGray)
                            Text(
                                "${String.format(Locale.US, "%.1f", currentState.baseline)} µT",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Noise Floor", fontSize = 11.sp, color = Color.LightGray)
                            Text(
                                "${String.format(Locale.US, "%.2f", currentState.noise)} µT",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trigger Threshold", fontSize = 11.sp, color = Color.LightGray)
                            Text(
                                "${String.format(Locale.US, "%.1f", currentState.threshold)} µT",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                    }
                }
            }

            // Buttons
            if (isCalibrating) {
                Button(
                    onClick = onCancelCalibration,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Calibration", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStartCalibration,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calibrate Baseline (60 Samples)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                "Formula: Threshold = max(3.0 µT, Noise × 4). Baseline adapts continuously to ambient drift.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AccentGold),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color(0xFFE2E8F0), fontSize = 13.sp, lineHeight = 18.sp)
    }
}
