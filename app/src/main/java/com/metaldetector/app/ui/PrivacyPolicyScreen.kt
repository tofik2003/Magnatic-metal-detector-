package com.metaldetector.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security Policy", fontWeight = FontWeight.Bold, color = Color.White) },
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
            // Privacy Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("100% Offline & Zero Data Collection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        "Metal Detector operates completely offline. It does not request internet access, location permissions, camera, contacts, or storage permissions. Your scanning data stays solely on your local device.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            // Key Guarantees
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SECURITY ARCHITECTURE", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    PolicyItem(
                        icon = Icons.Default.Lock,
                        title = "Zero Network Permissions",
                        desc = "The manifest contains no INTERNET permission. Network traffic is physically disabled at the Android OS sandbox level."
                    )
                    PolicyItem(
                        icon = Icons.Default.Security,
                        title = "Sandboxed Local Storage",
                        desc = "Saved scans and baseline calibration parameters are saved via Context.MODE_PRIVATE SharedPreferences. No third-party apps can access your data."
                    )
                    PolicyItem(
                        icon = Icons.Default.Shield,
                        title = "No Trackers or Analytics",
                        desc = "The app includes zero advertising SDKs, zero telemetry daemons, and zero third-party tracking libraries."
                    )
                }
            }

            // Safety Warning
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF28140E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Safety Disclaimer", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "This app measures magnetic flux density via the device's hardware compass. It cannot determine pipe depth, distinguish live AC high voltage wires, or guarantee structural drilling safety. Always verify with certified stud finders before altering walls.",
                        color = Color(0xFFFFD4B2),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
