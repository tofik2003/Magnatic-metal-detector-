# Metal Detector (Android & Jetpack Compose)

An offline Android Metal Detector application built with Kotlin, Jetpack Compose, and Material 3 that detects nearby ferromagnetic metals using the device's hardware magnetometer (`Sensor.TYPE_MAGNETIC_FIELD`).

---

## 📱 Key Specifications & Features

- **Platform & Language:** Android (Min SDK 26, Target SDK 34), 100% Kotlin.
- **UI Framework:** Jetpack Compose + Material 3 Dark High-Contrast Theme.
- **Offline & Private:** 100% offline. Zero internet, GPS, camera, or storage permissions required. Optional `android.permission.VIBRATE` for haptic feedback.
- **Detection Algorithm:**
  - **Sensor Rate:** Continuous 30 Hz sampling rate (~33,333 µs).
  - **Magnitude Formula:** $\text{magnitude} = \sqrt{x^2 + y^2 + z^2}$.
  - **Signal Smoothing:** Exponential Moving Average (EMA) filter with $\alpha = 0.2$ to reject jitter.
  - **Calibration:** Collects 60 baseline samples to determine ambient geomagnetic baseline and sample standard deviation ($\text{noise}$).
  - **Dynamic Threshold:** $\text{threshold} = \max(3.0\,\mu\text{T}, \text{noise} \times 4)$.
  - **Dual-Threshold Hysteresis:** Trigger alert at $\text{Signal} \ge \text{threshold}$, clear alert at $\text{Signal} \le \text{threshold} \times 0.6$.
  - **Slow Baseline Adaptation:** Automatically drifts and tracks slowly ($\alpha = 0.005$) when no target is present.

---

## 🧲 Detectability Guide

| Works With (Ferromagnetic) | Does Not Reliably Detect (Non-Ferrous) |
|:---------------------------|:---------------------------------------|
| Iron & Rebar               | Copper Pipes & Wiring                  |
| Structural & Mild Steel    | Aluminum Foil & Cans                   |
| Construction Screws & Nails| Gold & Silver                          |
| Heavy Steel Keys           | Brass Fixtures                         |
| Steel Conduit & Pipes      | Plastic & PVC Pipes                    |

---

## 📂 Project Architecture

```
app/src/main/
├── AndroidManifest.xml                  # VIBRATE permission, hardware compass feature declaration
└── java/com/metaldetector/app/
    ├── MainActivity.kt                  # Navigation host, sensor lifecycle, state collection
    ├── MetalDetectorApp.kt              # Android application entry point
    ├── sensor/
    │   └── MagnetometerReader.kt        # SensorManager flow at 30 Hz (TYPE_MAGNETIC_FIELD)
    ├── detection/
    │   ├── SignalFilter.kt              # EMA filter (alpha 0.2)
    │   ├── CalibrationManager.kt        # 60-sample baseline + noise std dev
    │   └── DetectionEngine.kt           # Anomaly trigger, 0.6x hysteresis, slow drift tracking
    ├── audio/
    │   └── AlertSoundManager.kt         # AudioTrack sine tone synthesis & Vibrator pulses
    ├── storage/
    │   └── ScanHistoryRepository.kt     # Local JSON persistence of sweeps & grid maps
    └── ui/
        ├── HomeScreen.kt                # Primary dashboard, live µT reading, quick actions
        ├── DetectorScreen.kt            # Analog needle meter, 3-axis readings, live waveform graph
        ├── CalibrationScreen.kt         # 60-sample progress dial, checklist, baseline stats
        ├── ScanGridScreen.kt            # 6x5 interactive grid matrix for wall & grass mapping
        ├── HistoryScreen.kt             # Saved scan logs with peak µT, target count, duration
        ├── SettingsScreen.kt            # Sound/vibration toggles, scanning instructions, safety notices
        ├── UnsupportedDeviceScreen.kt   # Graceful fallback when TYPE_MAGNETIC_FIELD is missing
        └── theme/Theme.kt               # Dark high-contrast palette (Gold, Cyan, Red)
```

---

## 🛠️ Testing & Verification Checklist

1. **Steel Key / Screw Distance Test:** Move a steel fastener towards phone from 15 cm down to 1–5 cm. Observe proportional needle swing and frequency increase in audio beeps.
2. **Negative Tests:** Bring copper wire or aluminum soda cans directly against the device; confirm signal stays below trigger threshold.
3. **Magnetic Case Test:** Test with a MagSafe or magnetic case to observe baseline offset and calibrate it out.
4. **Hysteresis Verification:** Ensure target trigger triggers at $\ge \text{threshold}$ and does not rapidly flicker off until dropping below $0.6 \times \text{threshold}$.
5. **Scan Matrix Mapping:** Map a 6×5 grid across drywall studs or lawn patches, tapping cells to record local field hotspots.
