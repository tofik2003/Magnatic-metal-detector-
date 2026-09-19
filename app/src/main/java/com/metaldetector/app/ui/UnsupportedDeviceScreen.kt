package com.metaldetector.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metaldetector.app.ui.theme.AccentRed
import com.metaldetector.app.ui.theme.DarkBackground
import com.metaldetector.app.ui.theme.DarkSurface

@Composable
fun UnsupportedDeviceScreen(
    onSimulateClick: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Sensor missing",
                        tint = AccentRed,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "Magnetometer Not Found",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "This device does not have a physical magnetic field sensor (Sensor.TYPE_MAGNETIC_FIELD). A hardware magnetometer is strictly required to detect changes in magnetic fields caused by nearby ferromagnetic metals.",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    if (onSimulateClick != null) {
                        Button(
                            onClick = onSimulateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enter Demonstration Mode", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
