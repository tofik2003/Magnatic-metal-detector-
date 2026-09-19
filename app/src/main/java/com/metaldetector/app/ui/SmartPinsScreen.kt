package com.metaldetector.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaldetector.app.detection.DetectionState
import com.metaldetector.app.storage.DetectionPin
import com.metaldetector.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPinsScreen(
    currentState: DetectionState,
    pins: List<DetectionPin>,
    onSavePin: (DetectionPin) -> Unit,
    onDeletePin: (String) -> Unit,
    onClearPins: () -> Unit,
    onBack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault())
    var showAddDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("Target Metal Object") }
    var surfaceType by remember { mutableStateOf("Drywall") }
    var pinNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Location Pins", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    if (pins.isNotEmpty()) {
                        IconButton(onClick = onClearPins) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Pins", tint = AccentRed)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    pinTitle = if (currentState.isTriggered) "Metal Detected (+${String.format(Locale.US, "%.1f", currentState.deviation)} µT)" else "Tagged Location"
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.Black) },
                text = { Text("Remember This Spot", fontWeight = FontWeight.Bold, color = Color.Black) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Target Distance Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED DISTANCE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (currentState.isTriggered) String.format(Locale.US, "≈ %.1f cm", currentState.estimatedDistanceCm) else "> 20 cm (Clear)",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (currentState.isTriggered) AccentGold else Color.LightGray
                        )
                        Text(currentState.depthCategory, fontSize = 12.sp, color = AccentCyan)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("DOMINANT AXIS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(currentState.dominantAxis.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Anomaly: +${String.format(Locale.US, "%.1f", currentState.deviation)} µT", fontSize = 12.sp, color = AccentRed)
                    }
                }
            }

            // Pins List
            if (pins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No smart metal locations remembered yet", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hover over a metal target and tap 'Remember This Spot'", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pins, key = { it.id }) { pin ->
                        PinItemCard(
                            pin = pin,
                            formattedDate = dateFormat.format(Date(pin.timestamp)),
                            onDelete = { onDeletePin(pin.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog to Add Pin
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Remember Metal Location", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pinTitle,
                        onValueChange = { pinTitle = it },
                        label = { Text("Spot Name / Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Surface Material", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("Drywall", "Lawn", "Floor", "Wood").forEach { surf ->
                            FilterChip(
                                selected = surfaceType == surf,
                                onClick = { surfaceType = surf },
                                label = { Text(surf, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pinNotes,
                        onValueChange = { pinNotes = it },
                        label = { Text("Notes (e.g. 10cm above switch)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Auto-Captured: Peak ${String.format(Locale.US, "%.1f", currentState.filteredMagnitude)} µT • Dist ≈ ${String.format(Locale.US, "%.1f", currentState.estimatedDistanceCm)} cm",
                        fontSize = 11.sp,
                        color = AccentCyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPin = DetectionPin(
                            id = UUID.randomUUID().toString(),
                            title = pinTitle.ifBlank { "Metal Hotspot" },
                            surfaceType = surfaceType,
                            timestamp = System.currentTimeMillis(),
                            peakMicroTesla = currentState.filteredMagnitude,
                            estimatedDistanceCm = currentState.estimatedDistanceCm,
                            relativeDepthCategory = currentState.depthCategory,
                            notes = pinNotes
                        )
                        onSavePin(newPin)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Pin", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PinItemCard(
    pin: DetectionPin,
    formattedDate: String,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(pin.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("${pin.surfaceType} • $formattedDate", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = DarkBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Est. Distance", color = Color.Gray, fontSize = 10.sp)
                    Text("≈ ${String.format(Locale.US, "%.1f", pin.estimatedDistanceCm)} cm", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
                }
                Column {
                    Text("Depth Band", color = Color.Gray, fontSize = 10.sp)
                    Text(pin.relativeDepthCategory, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
                Column {
                    Text("Peak Field", color = Color.Gray, fontSize = 10.sp)
                    Text("${String.format(Locale.US, "%.1f", pin.peakMicroTesla)} µT", fontWeight = FontWeight.Bold, color = AccentRed, fontSize = 13.sp)
                }
            }

            if (pin.notes.isNotBlank()) {
                Text("Notes: ${pin.notes}", fontSize = 11.sp, color = Color(0xFFCBD5E1))
            }
        }
    }
}
