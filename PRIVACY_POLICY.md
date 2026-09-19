# Privacy Policy & Security Architecture for Metal Detector

**Effective Date:** September 19, 2026  
**Application:** Metal Detector (Android)  
**Package:** `com.metaldetector.app`  

---

## 🛡️ Privacy Commitment (100% Offline & Zero-Telemetry)

Metal Detector is engineered from the ground up with a **Zero Data Collection** architecture:

1. **No Internet Permission Required:**
   The application's manifest does **not** declare `android.permission.INTERNET`. The app cannot initiate outgoing connections, transmit telemetry, or receive unsolicited external commands.
2. **No Hardware Tracking or Fingerprinting:**
   No device IDs, advertising IDs, IMEI, MAC addresses, or SIM card serial numbers are queried or accessed.
3. **No Location / GPS / Camera Access:**
   Location, Bluetooth beacons, cameras, and microphones are neither requested nor utilized.
4. **On-Device Storage Exclusively:**
   All scan session records, calibration baselines, and user preferences are saved strictly within Android's sandboxed local `SharedPreferences` (`Context.MODE_PRIVATE`). No cloud sync or third-party storage is used.
5. **No Third-Party SDKs or Trackers:**
   The project has no analytics frameworks (Google Analytics, Firebase, Mixpanel, AppsFlyer), advertisement SDKs, or crash reporter network daemons.

---

## 🔒 Security Architecture

1. **Network Security Configuration:**
   Cleartext network traffic is explicitly prohibited via `res/xml/network_security_config.xml` with empty trust anchors.
2. **Encrypted & Isolated Sandbox:**
   Android application sandbox prevents other apps on the device from accessing Metal Detector's data.
3. **Backup Protection:**
   `android:allowBackup="false"` to prevent unauthorized extraction of local scan logs via Android Debug Bridge (ADB) or cloud backups.
4. **Haptic Hardware Safeguards:**
   Only `android.permission.VIBRATE` is declared to deliver tactile feedback during high magnetic field detection. The vibration is managed through a bounded coroutine loop to avoid hardware motor overheating.

---

## ⚠️ Physical & Safety Disclaimers

1. **Magnetic Sensor Constraints:**
   The app measures local ambient magnetic flux density using the phone's built-in Hall-effect magnetometer (`Sensor.TYPE_MAGNETIC_FIELD`).
2. **Target Material Limitations:**
   Only ferromagnetic materials (Iron, Steel, Nickel, Rebar, Screws) distort the Earth's ambient field sufficiently to trigger alerts. Non-ferrous metals (Gold, Silver, Pure Copper, Aluminum, Brass) and non-metallic objects (PVC pipes, timber) are not reliably detectable.
3. **Structural / Electrical Danger:**
   **DO NOT** rely on this application to verify the absence of live high-voltage AC electrical lines, structural wall framing, or pressurized utility pipes prior to drilling, sawing, or excavating. Always utilize certified professional utility line locators and stud scanners.
