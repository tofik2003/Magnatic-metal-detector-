package com.metaldetector.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metaldetector.app.audio.AlertSoundManager
import com.metaldetector.app.detection.CalibrationData
import com.metaldetector.app.detection.DetectionEngine
import com.metaldetector.app.detection.DetectionState
import com.metaldetector.app.sensor.MagneticReading
import com.metaldetector.app.sensor.MagnetometerReader
import com.metaldetector.app.storage.ScanHistoryRepository
import com.metaldetector.app.storage.ScanRecord
import com.metaldetector.app.ui.*
import com.metaldetector.app.ui.theme.MetalDetectorTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var magnetometerReader: MagnetometerReader
    private lateinit var alertSoundManager: AlertSoundManager
    private lateinit var historyRepository: ScanHistoryRepository
    private val detectionEngine = DetectionEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        magnetometerReader = MagnetometerReader(this)
        alertSoundManager = AlertSoundManager(this)
        historyRepository = ScanHistoryRepository(this)

        val isSensorAvailable = magnetometerReader.isSensorAvailable()
        val sensorInfo = magnetometerReader.getSensorInfo()

        setContent {
            MetalDetectorTheme {
                val navController = rememberNavController()

                var currentReading by remember {
                    mutableStateOf(MagneticReading(0f, 0f, 0f, 48f, 3))
                }
                var currentState by remember {
                    mutableStateOf(DetectionState(baseline = 48f, threshold = 3f))
                }
                var calibrationData by remember { mutableStateOf<CalibrationData?>(null) }
                val graphHistory = remember { mutableStateListOf<Float>() }

                var isSoundEnabled by remember { mutableStateOf(alertSoundManager.isSoundEnabled) }
                var isVibrationEnabled by remember { mutableStateOf(alertSoundManager.isVibrationEnabled) }

                var scanRecords by remember {
                    mutableStateOf(historyRepository.getAllRecords())
                }

                // Collect sensor readings
                LaunchedEffect(Unit) {
                    if (isSensorAvailable) {
                        magnetometerReader.getReadingsFlow().collectLatest { reading ->
                            currentReading = reading
                            val state = detectionEngine.processSample(reading)
                            currentState = state

                            // Update audio and haptics
                            alertSoundManager.updateAlert(state.alertLevel, state.signalStrengthPercent)

                            // Keep rolling window for live graph (last 40 samples ~ 1.3 seconds)
                            graphHistory.add(state.filteredMagnitude)
                            if (graphHistory.size > 45) {
                                graphHistory.removeAt(0)
                            }
                        }
                    }
                }

                if (!isSensorAvailable) {
                    UnsupportedDeviceScreen()
                } else {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                isSensorAvailable = isSensorAvailable,
                                sensorInfo = sensorInfo,
                                currentState = currentState,
                                calibrationData = calibrationData,
                                onNavigate = { dest -> navController.navigate(dest) }
                            )
                        }

                        composable("detector") {
                            DetectorScreen(
                                currentReading = currentReading,
                                currentState = currentState,
                                historyGraphData = graphHistory,
                                isSoundEnabled = isSoundEnabled,
                                isVibrationEnabled = isVibrationEnabled,
                                onToggleSound = { enabled ->
                                    isSoundEnabled = enabled
                                    alertSoundManager.isSoundEnabled = enabled
                                },
                                onToggleVibration = { enabled ->
                                    isVibrationEnabled = enabled
                                    alertSoundManager.isVibrationEnabled = enabled
                                },
                                onSaveScanSession = {
                                    val newRecord = ScanRecord(
                                        id = UUID.randomUUID().toString(),
                                        title = "Live Sweep #${scanRecords.size + 1}",
                                        type = "Live Detector",
                                        timestamp = System.currentTimeMillis(),
                                        maxMicroTesla = graphHistory.maxOrNull() ?: currentState.filteredMagnitude,
                                        avgMicroTesla = if (graphHistory.isNotEmpty()) graphHistory.average().toFloat() else currentState.filteredMagnitude,
                                        targetsDetectedCount = if (currentState.isTriggered) 1 else 0,
                                        durationSeconds = 15
                                    )
                                    historyRepository.saveRecord(newRecord)
                                    scanRecords = historyRepository.getAllRecords()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("calibration") {
                            CalibrationScreen(
                                isCalibrating = detectionEngine.calibrationManager.isCalibrating,
                                calibrationProgress = detectionEngine.calibrationManager.getProgress(),
                                samplesGathered = detectionEngine.calibrationManager.getSamplesGathered(),
                                calibrationData = calibrationData,
                                currentState = currentState,
                                onStartCalibration = {
                                    detectionEngine.calibrationManager.startCalibration()
                                },
                                onCancelCalibration = {
                                    detectionEngine.calibrationManager.cancelCalibration()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("grid") {
                            ScanGridScreen(
                                currentState = currentState,
                                onSaveGridAsSession = { type, targets, maxUt ->
                                    val newRecord = ScanRecord(
                                        id = UUID.randomUUID().toString(),
                                        title = "$type Scan",
                                        type = type,
                                        timestamp = System.currentTimeMillis(),
                                        maxMicroTesla = maxUt,
                                        avgMicroTesla = currentState.baseline,
                                        targetsDetectedCount = targets,
                                        durationSeconds = 30
                                    )
                                    historyRepository.saveRecord(newRecord)
                                    scanRecords = historyRepository.getAllRecords()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                records = scanRecords,
                                onDeleteRecord = { id ->
                                    historyRepository.deleteRecord(id)
                                    scanRecords = historyRepository.getAllRecords()
                                },
                                onClearAll = {
                                    historyRepository.clearAll()
                                    scanRecords = emptyList()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                isSoundEnabled = isSoundEnabled,
                                isVibrationEnabled = isVibrationEnabled,
                                onToggleSound = { enabled ->
                                    isSoundEnabled = enabled
                                    alertSoundManager.isSoundEnabled = enabled
                                },
                                onToggleVibration = { enabled ->
                                    isVibrationEnabled = enabled
                                    alertSoundManager.isVibrationEnabled = enabled
                                },
                                sensorInfo = sensorInfo,
                                onNavigatePrivacy = { navController.navigate("privacy") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("privacy") {
                            PrivacyPolicyScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alertSoundManager.release()
    }
}
