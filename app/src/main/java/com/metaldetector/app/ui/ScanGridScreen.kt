package com.metaldetector.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.metaldetector.app.ui.theme.*
import java.util.Locale

/**
 * Grid cell holding sample value and detection status
 */
data class GridCellData(
    val row: Int,
    val col: Int,
    val valueMicroTesla: Float? = null,
    val isAnomaly: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanGridScreen(
    currentState: DetectionState,
    onSaveGridAsSession: (type: String, targetCount: Int, maxUt: Float) -> Unit,
    onBack: () -> Unit
) {
    var scanMode by remember { mutableStateOf("Wall (Rebar / Nails)") } // or "Grass (Yard / Relics)"
    val gridRows = 6
    val gridCols = 5
    val totalCells = gridRows * gridCols

    var cells by remember {
        mutableStateOf(List(totalCells) { index ->
            GridCellData(row = index / gridCols, col = index % gridCols)
        })
    }

    var selectedCellIndex by remember { mutableIntStateOf(0) }

    fun recordAtSelectedCell() {
        val currentMag = currentState.filteredMagnitude
        val isAnomaly = currentState.isTriggered

        val updated = cells.toMutableList()
        updated[selectedCellIndex] = GridCellData(
            row = selectedCellIndex / gridCols,
            col = selectedCellIndex % gridCols,
            valueMicroTesla = currentMag,
            isAnomaly = isAnomaly
        )
        cells = updated

        // Advance to next cell if within bounds
        if (selectedCellIndex < totalCells - 1) {
            selectedCellIndex += 1
        }
    }

    val scannedCount = cells.count { it.valueMicroTesla != null }
    val anomaliesCount = cells.count { it.isAnomaly }
    val maxDetected = cells.mapNotNull { it.valueMicroTesla }.maxOrNull() ?: currentState.filteredMagnitude

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wall & Grass Scan Grid", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                actions = {
                    IconButton(onClick = {
                        cells = List(totalCells) { index ->
                            GridCellData(row = index / gridCols, col = index % gridCols)
                        }
                        selectedCellIndex = 0
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Grid", tint = Color.White)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Selector Tabs (Wall vs Grass)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = scanMode.startsWith("Wall"),
                    onClick = { scanMode = "Wall (Rebar / Nails)" },
                    label = { Text("Wall Scan (Stud/Rebar)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGold,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = scanMode.startsWith("Grass"),
                    onClick = { scanMode = "Grass (Ground / Lost Keys)" },
                    label = { Text("Grass / Ground") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Summary stats banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Cells Scanned", color = Color.Gray, fontSize = 11.sp)
                    Text("$scannedCount / $totalCells", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Targets Found", color = Color.Gray, fontSize = 11.sp)
                    Text("$anomaliesCount", fontWeight = FontWeight.Bold, color = if (anomaliesCount > 0) AccentRed else AccentGreen)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Live", color = Color.Gray, fontSize = 11.sp)
                    Text("${String.format(Locale.US, "%.1f", currentState.filteredMagnitude)} µT", fontWeight = FontWeight.Bold, color = AccentCyan)
                }
            }

            // Interactive Scan Matrix
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(cells) { index, cell ->
                    val isSelected = index == selectedCellIndex
                    val cellColor by animateColorAsState(
                        targetValue = when {
                            cell.isAnomaly -> AccentRed
                            cell.valueMicroTesla != null -> Color(0xFF2E7D32) // muted green
                            isSelected -> AccentGold.copy(alpha = 0.35f)
                            else -> DarkSurfaceElevated
                        },
                        label = "cellColor"
                    )

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cellColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) AccentGold else DarkBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCellIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (cell.valueMicroTesla != null) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", cell.valueMicroTesla),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "µT",
                                    fontSize = 9.sp,
                                    color = Color(0xCCFFFFFF)
                                )
                            } else {
                                Text(
                                    text = "${cell.row + 1},${cell.col + 1}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Bottom action panel
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { recordAtSelectedCell() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f).height(50.dp)
                ) {
                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sample Cell (${selectedCellIndex / gridCols + 1},${selectedCellIndex % gridCols + 1})",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        onSaveGridAsSession(scanMode, anomaliesCount, maxDetected)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Map", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
