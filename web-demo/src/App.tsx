import React, { useState, useEffect, useRef } from 'react';
import {
  Compass,
  Sliders,
  Activity,
  Grid3X3,
  History,
  Settings,
  AlertTriangle,
  Volume2,
  VolumeX,
  Vibrate,
  Play,
  RotateCcw,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Smartphone,
  Flame,
  Info,
  Save,
  Trash2,
  ArrowLeft,
  ChevronRight,
  ShieldAlert,
  Zap
} from 'lucide-react';

// Models matching Kotlin implementation
interface MagneticReading {
  x: number;
  y: number;
  z: number;
  magnitude: number;
  accuracy: number;
  timestamp: number;
}

interface ScanRecord {
  id: string;
  title: string;
  type: string;
  timestamp: number;
  maxMicroTesla: number;
  avgMicroTesla: number;
  targetsDetectedCount: number;
  durationSeconds: number;
}

type ScreenType = 'home' | 'calibration' | 'detector' | 'grid' | 'history' | 'settings' | 'unsupported';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<ScreenType>('home');
  const [deviceSensorAvailable, setDeviceSensorAvailable] = useState<boolean>(true);

  // Simulation controls (for testing without live physical compass in browser)
  const [targetDistanceCm, setTargetDistanceCm] = useState<number>(15); // 0 to 20 cm
  const [selectedMetalType, setSelectedMetalType] = useState<string>('Steel Pipe');
  const [hasMagneticCase, setHasMagneticCase] = useState<boolean>(false);

  // Algorithm state
  const [rawX, setRawX] = useState<number>(18.5);
  const [rawY, setRawY] = useState<number>(24.2);
  const [rawZ, setRawZ] = useState<number>(38.1);
  const [filteredMagnitude, setFilteredMagnitude] = useState<number>(48.7);
  const [rawMagnitude, setRawMagnitude] = useState<number>(48.7);

  // Calibration state
  const [baseline, setBaseline] = useState<number>(48.0);
  const [noise, setNoise] = useState<number>(0.45);
  const [threshold, setThreshold] = useState<number>(3.0);
  const [isCalibrating, setIsCalibrating] = useState<boolean>(false);
  const [calibrationProgress, setCalibrationProgress] = useState<number>(0);
  const [calibrationSamples, setCalibrationSamples] = useState<number[]>([]);

  // Detection trigger state with hysteresis
  const [isTriggered, setIsTriggered] = useState<boolean>(false);
  const [deviation, setDeviation] = useState<number>(0);
  const [signalPercent, setSignalPercent] = useState<number>(0);

  // Settings
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);
  const [vibrationEnabled, setVibrationEnabled] = useState<boolean>(true);

  // Waveform graph history
  const [graphHistory, setGraphHistory] = useState<number[]>([48, 48.2, 47.9, 48.1, 48.0, 48.3, 48.1]);

  // Scan History
  const [historyRecords, setHistoryRecords] = useState<ScanRecord[]>([
    {
      id: 'rec-1',
      title: 'Drywall Rebar Sweep',
      type: 'Wall Scan',
      timestamp: Date.now() - 3600000 * 2,
      maxMicroTesla: 78.4,
      avgMicroTesla: 51.2,
      targetsDetectedCount: 3,
      durationSeconds: 42
    },
    {
      id: 'rec-2',
      title: 'Backyard Lawn Key Search',
      type: 'Grass Scan',
      timestamp: Date.now() - 3600000 * 24,
      maxMicroTesla: 92.1,
      avgMicroTesla: 48.9,
      targetsDetectedCount: 1,
      durationSeconds: 110
    }
  ]);

  // Scan Grid State (6 rows x 5 cols)
  const [gridCells, setGridCells] = useState<Array<{ val: number | null; isAnomaly: boolean }>>(
    Array(30).fill({ val: null, isAnomaly: false })
  );
  const [gridSelectedCell, setGridSelectedCell] = useState<number>(0);
  const [gridMode, setGridMode] = useState<'Wall (Rebar / Nails)' | 'Grass (Ground / Relics)'>('Wall (Rebar / Nails)');

  // Audio Context for Beep alert
  const audioCtxRef = useRef<AudioContext | null>(null);

  // Metal target magnetic contribution lookup (empirical inverse square anomaly)
  const metalMultipliers: Record<string, number> = {
    'Steel Pipe': 140.0,
    'Iron Rebar': 110.0,
    'Large Screw': 45.0,
    'Steel Key': 35.0,
    'Copper Pipe (Negative Test)': 0.0,
    'Aluminum Can (Negative Test)': 0.0,
    'Gold Ring (Negative Test)': 0.0
  };

  // Web Audio Synth for Detection Beeps
  const playBeep = (freq: number) => {
    if (!soundEnabled) return;
    try {
      if (!audioCtxRef.current) {
        audioCtxRef.current = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      const ctx = audioCtxRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, ctx.currentTime);

      gain.gain.setValueAtTime(0.08, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.06);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.065);
    } catch (e) {
      // Audio autoplay policy
    }
  };

  // Vibration synthesis
  const triggerHaptic = () => {
    if (!vibrationEnabled) return;
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(40);
    }
  };

  // 30 Hz Sensor Loop
  useEffect(() => {
    const timer = setInterval(() => {
      // 1. Calculate magnetic field with realistic noise and target proximity
      const baseEarthField = 48.0;
      const noiseVariation = (Math.random() - 0.5) * 0.45;
      const caseDistortion = hasMagneticCase ? 22.0 : 0.0;

      // Anomaly diminishes sharply with distance: 1 / (d + 1)^2.2
      const targetPower = metalMultipliers[selectedMetalType] || 0.0;
      const distanceFactor = Math.pow(Math.max(0.5, targetDistanceCm + 1.0) / 2.0, -2.2);
      const metalAnomaly = targetPower * distanceFactor;

      const currentMag = baseEarthField + noiseVariation + caseDistortion + metalAnomaly;

      // 3-axis decomposition
      const x = currentMag * 0.35 + (Math.random() - 0.5) * 0.3;
      const y = currentMag * 0.48 + (Math.random() - 0.5) * 0.3;
      const z = Math.sqrt(Math.max(0, currentMag * currentMag - x * x - y * y));

      setRawX(x);
      setRawY(y);
      setRawZ(z);
      setRawMagnitude(currentMag);

      // 2. Exponential Moving Average Filter (alpha = 0.2)
      const alpha = 0.2;
      const smoothed = alpha * currentMag + (1.0 - alpha) * filteredMagnitude;
      setFilteredMagnitude(smoothed);

      // 3. Anomaly deviation and detection trigger logic
      const dev = Math.abs(smoothed - baseline);
      setDeviation(dev);

      // Hysteresis: trigger >= threshold, clear <= threshold * 0.6
      const clearThresh = threshold * 0.6;
      let newTriggered = isTriggered;

      if (!isTriggered) {
        if (dev >= threshold) {
          newTriggered = true;
          setIsTriggered(true);
        } else {
          // Slow drift baseline tracking when calm
          setBaseline(b => (1 - 0.003) * b + 0.003 * smoothed);
        }
      } else {
        if (dev <= clearThresh) {
          newTriggered = false;
          setIsTriggered(false);
        }
      }

      // Signal percent (0 - 100%)
      const maxDev = Math.max(threshold * 6.0, 35.0);
      const percent = Math.min(100, Math.max(0, (dev / maxDev) * 100));
      setSignalPercent(percent);

      // Alert audio/haptic pulse if triggered
      if (newTriggered && Math.random() < Math.max(0.2, percent / 100)) {
        const pitch = 650 + (percent * 7.5);
        playBeep(pitch);
        triggerHaptic();
      }

      // Live graph history buffer (last 40 points)
      setGraphHistory(prev => {
        const next = [...prev, smoothed];
        if (next.length > 40) next.shift();
        return next;
      });

      // 4. Handle active Calibration sampling (60 samples @ 30 Hz = 2.0s)
      if (isCalibrating) {
        setCalibrationSamples(prev => {
          const updated = [...prev, currentMag];
          setCalibrationProgress(updated.length / 60);
          if (updated.length >= 60) {
            // Compute mean and standard deviation
            const mean = updated.reduce((a, b) => a + b, 0) / updated.length;
            const variance = updated.reduce((a, b) => a + Math.pow(b - mean, 2), 0) / updated.length;
            const stdDev = Math.sqrt(variance);
            const dynamicThresh = Math.max(3.0, stdDev * 4.0);

            setBaseline(mean);
            setNoise(stdDev);
            setThreshold(dynamicThresh);
            setIsCalibrating(false);
            return [];
          }
          return updated;
        });
      }
    }, 33); // ~30 Hz

    return () => clearInterval(timer);
  }, [
    filteredMagnitude,
    baseline,
    threshold,
    isTriggered,
    isCalibrating,
    targetDistanceCm,
    selectedMetalType,
    hasMagneticCase,
    soundEnabled,
    vibrationEnabled
  ]);

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#090d14', color: '#e2e8f0' }}>
      {/* Interactive Device Simulation Controller Sidebar */}
      <div
        style={{
          width: '380px',
          borderRight: '1px solid #1f293d',
          background: '#0d131f',
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
          overflowY: 'auto'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              background: '#eab30822',
              border: '1px solid #eab308',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Compass size={20} color="#eab308" />
          </div>
          <div>
            <h1 style={{ fontSize: '18px', fontWeight: 800, color: '#f8fafc', margin: 0 }}>
              Physical Magnetometer
            </h1>
            <p style={{ fontSize: '12px', color: '#94a3b8', margin: 0 }}>
              Hardware Sensor Environment
            </p>
          </div>
        </div>

        {/* Toggle Real Hardware vs Simulated Target */}
        <div
          style={{
            background: '#141d2e',
            borderRadius: '12px',
            padding: '16px',
            border: '1px solid #24324a'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600 }}>Magnetometer Status</span>
            <button
              onClick={() => {
                const next = !deviceSensorAvailable;
                setDeviceSensorAvailable(next);
                if (!next) setCurrentScreen('unsupported');
                else setCurrentScreen('home');
              }}
              style={{
                fontSize: '11px',
                padding: '4px 10px',
                borderRadius: '6px',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 700,
                background: deviceSensorAvailable ? '#10b98122' : '#ef444422',
                color: deviceSensorAvailable ? '#10b981' : '#ef4444'
              }}
            >
              {deviceSensorAvailable ? 'TYPE_MAGNETIC_FIELD: OK' : 'SENSOR MISSING'}
            </button>
          </div>

          <p style={{ fontSize: '11px', color: '#64748b', lineHeight: '15px' }}>
            Emulates <code>SensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)</code> at 30 Hz.
          </p>
        </div>

        {/* Metal Proximity Slider */}
        <div
          style={{
            background: '#141d2e',
            borderRadius: '12px',
            padding: '16px',
            border: '1px solid #24324a'
          }}
        >
          <label style={{ fontSize: '13px', fontWeight: 700, display: 'block', marginBottom: '8px' }}>
            Target Metal Object
          </label>
          <select
            value={selectedMetalType}
            onChange={e => setSelectedMetalType(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px',
              borderRadius: '8px',
              background: '#0a0f1a',
              border: '1px solid #334155',
              color: '#f8fafc',
              fontSize: '13px',
              marginBottom: '16px'
            }}
          >
            <optgroup label="Works With (Ferromagnetic)">
              <option value="Steel Pipe">Steel Pipe (Strong Anomaly)</option>
              <option value="Iron Rebar">Iron Rebar (Medium-Strong)</option>
              <option value="Large Screw">Large Screw / Bolt (Localized)</option>
              <option value="Steel Key">Steel Key (Moderate)</option>
            </optgroup>
            <optgroup label="Does Not Reliably Detect (Non-Ferrous)">
              <option value="Copper Pipe (Negative Test)">Copper Pipe (Negative Test)</option>
              <option value="Aluminum Can (Negative Test)">Aluminum Can (Negative Test)</option>
              <option value="Gold Ring (Negative Test)">Gold Ring (Negative Test)</option>
            </optgroup>
          </select>

          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
            <span style={{ fontSize: '12px', color: '#94a3b8' }}>Distance to Phone Sensor</span>
            <span style={{ fontSize: '13px', fontWeight: 800, color: '#eab308' }}>
              {targetDistanceCm === 0 ? 'Touching (0 cm)' : `${targetDistanceCm} cm`}
            </span>
          </div>
          <input
            type="range"
            min="0"
            max="20"
            step="0.5"
            value={targetDistanceCm}
            onChange={e => setTargetDistanceCm(parseFloat(e.target.value))}
            style={{ width: '100%', accentColor: '#eab308', cursor: 'pointer' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: '#64748b', marginTop: '4px' }}>
            <span>0 cm (Touch)</span>
            <span>Recommended: 1–5 cm</span>
            <span>20 cm (Far)</span>
          </div>

          <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid #24324a' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '12px' }}>
              <input
                type="checkbox"
                checked={hasMagneticCase}
                onChange={e => setHasMagneticCase(e.target.checked)}
                style={{ accentColor: '#ef4444' }}
              />
              <span style={{ color: hasMagneticCase ? '#f87171' : '#94a3b8' }}>
                Simulate Magnetic Case / MagSafe (+22 µT offset)
              </span>
            </label>
          </div>
        </div>

        {/* Live Raw Sensor Output */}
        <div
          style={{
            background: '#141d2e',
            borderRadius: '12px',
            padding: '16px',
            border: '1px solid #24324a'
          }}
        >
          <span style={{ fontSize: '12px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Raw 3-Axis Reading
          </span>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', marginTop: '10px' }}>
            <div style={{ background: '#090d14', padding: '8px', borderRadius: '6px', textAlign: 'center' }}>
              <div style={{ fontSize: '10px', color: '#64748b' }}>X-AXIS</div>
              <div style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8' }}>{rawX.toFixed(1)}</div>
            </div>
            <div style={{ background: '#090d14', padding: '8px', borderRadius: '6px', textAlign: 'center' }}>
              <div style={{ fontSize: '10px', color: '#64748b' }}>Y-AXIS</div>
              <div style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8' }}>{rawY.toFixed(1)}</div>
            </div>
            <div style={{ background: '#090d14', padding: '8px', borderRadius: '6px', textAlign: 'center' }}>
              <div style={{ fontSize: '10px', color: '#64748b' }}>Z-AXIS</div>
              <div style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8' }}>{rawZ.toFixed(1)}</div>
            </div>
          </div>
          <div style={{ marginTop: '10px', fontSize: '11px', color: '#94a3b8', display: 'flex', justifyContent: 'space-between' }}>
            <span>Vector Magnitude:</span>
            <span style={{ fontWeight: 700, color: '#f8fafc' }}>{rawMagnitude.toFixed(1)} µT</span>
          </div>
        </div>

        {/* Specs Overview */}
        <div style={{ marginTop: 'auto', fontSize: '11px', color: '#64748b', lineHeight: '16px' }}>
          <p>• Sampling Rate: 30 Hz</p>
          <p>• EMA Filter: α = 0.2</p>
          <p>• Threshold: max(3.0, noise × 4)</p>
          <p>• Hysteresis: Trigger @ 1.0×, Clear @ 0.6×</p>
        </div>
      </div>

      {/* Android Device Mockup Frame */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '24px',
          background: 'radial-gradient(circle at 50% 50%, #151f33 0%, #070a10 100%)'
        }}
      >
        <div
          style={{
            width: '390px',
            height: '800px',
            background: '#0c1017',
            borderRadius: '44px',
            border: '8px solid #232d3f',
            boxShadow: '0 25px 60px -15px rgba(0, 0, 0, 0.8), 0 0 0 1px #334155',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            position: 'relative'
          }}
        >
          {/* Android Status Bar */}
          <div
            style={{
              height: '36px',
              padding: '0 20px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              fontSize: '12px',
              fontWeight: 600,
              color: '#94a3b8',
              background: '#0c1017',
              zIndex: 10
            }}
          >
            <span>9:41</span>
            {/* Phone Speaker & Camera Notch */}
            <div
              style={{
                width: '80px',
                height: '16px',
                background: '#1a2333',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#090d14' }} />
            </div>
            <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
              <span style={{ fontSize: '10px' }}>5G</span>
              <span style={{ fontSize: '10px' }}>100%</span>
            </div>
          </div>

          {/* Screen Content Router */}
          <div style={{ flex: 1, overflowY: 'auto', position: 'relative' }}>
            {currentScreen === 'unsupported' && (
              <UnsupportedView onRetry={() => { setDeviceSensorAvailable(true); setCurrentScreen('home'); }} />
            )}

            {currentScreen === 'home' && (
              <HomeView
                isSensorAvailable={deviceSensorAvailable}
                filteredMag={filteredMagnitude}
                deviation={deviation}
                isTriggered={isTriggered}
                signalPercent={signalPercent}
                onNavigate={setCurrentScreen}
              />
            )}

            {currentScreen === 'detector' && (
              <DetectorView
                filteredMag={filteredMagnitude}
                rawX={rawX}
                rawY={rawY}
                rawZ={rawZ}
                deviation={deviation}
                threshold={threshold}
                baseline={baseline}
                isTriggered={isTriggered}
                signalPercent={signalPercent}
                graphHistory={graphHistory}
                soundEnabled={soundEnabled}
                vibrationEnabled={vibrationEnabled}
                onToggleSound={() => setSoundEnabled(!soundEnabled)}
                onToggleVibration={() => setVibrationEnabled(!vibrationEnabled)}
                onBack={() => setCurrentScreen('home')}
                onSaveSession={() => {
                  const newRec: ScanRecord = {
                    id: `rec-${Date.now()}`,
                    title: `Live Sweep #${historyRecords.length + 1}`,
                    type: 'Live Detector',
                    timestamp: Date.now(),
                    maxMicroTesla: Math.max(...graphHistory),
                    avgMicroTesla: graphHistory.reduce((a, b) => a + b, 0) / graphHistory.length,
                    targetsDetectedCount: isTriggered ? 1 : 0,
                    durationSeconds: 20
                  };
                  setHistoryRecords([newRec, ...historyRecords]);
                  alert('Scan session saved to local history!');
                }}
              />
            )}

            {currentScreen === 'calibration' && (
              <CalibrationView
                isCalibrating={isCalibrating}
                progress={calibrationProgress}
                samplesCount={calibrationSamples.length}
                baseline={baseline}
                noise={noise}
                threshold={threshold}
                onStart={() => {
                  setCalibrationSamples([]);
                  setCalibrationProgress(0);
                  setIsCalibrating(true);
                }}
                onCancel={() => {
                  setIsCalibrating(false);
                  setCalibrationSamples([]);
                }}
                onBack={() => setCurrentScreen('home')}
              />
            )}

            {currentScreen === 'grid' && (
              <ScanGridView
                gridMode={gridMode}
                setGridMode={setGridMode}
                cells={gridCells}
                selectedCell={gridSelectedCell}
                setSelectedCell={setGridSelectedCell}
                currentMag={filteredMagnitude}
                isTriggered={isTriggered}
                onSampleCurrentCell={() => {
                  const next = [...gridCells];
                  next[gridSelectedCell] = {
                    val: filteredMagnitude,
                    isAnomaly: isTriggered
                  };
                  setGridCells(next);
                  if (gridSelectedCell < 29) {
                    setGridSelectedCell(gridSelectedCell + 1);
                  }
                }}
                onReset={() => {
                  setGridCells(Array(30).fill({ val: null, isAnomaly: false }));
                  setGridSelectedCell(0);
                }}
                onSaveGrid={() => {
                  const anomalies = gridCells.filter(c => c.isAnomaly).length;
                  const vals = gridCells.map(c => c.val).filter(v => v !== null) as number[];
                  const maxV = vals.length ? Math.max(...vals) : filteredMagnitude;
                  const newRec: ScanRecord = {
                    id: `rec-${Date.now()}`,
                    title: `${gridMode.split(' ')[0]} Matrix Map`,
                    type: gridMode.startsWith('Wall') ? 'Wall Scan' : 'Grass Scan',
                    timestamp: Date.now(),
                    maxMicroTesla: maxV,
                    avgMicroTesla: baseline,
                    targetsDetectedCount: anomalies,
                    durationSeconds: 35
                  };
                  setHistoryRecords([newRec, ...historyRecords]);
                  alert('Scan grid map saved to history!');
                }}
                onBack={() => setCurrentScreen('home')}
              />
            )}

            {currentScreen === 'history' && (
              <HistoryView
                records={historyRecords}
                onDelete={id => setHistoryRecords(historyRecords.filter(r => r.id !== id))}
                onClearAll={() => setHistoryRecords([])}
                onBack={() => setCurrentScreen('home')}
              />
            )}

            {currentScreen === 'settings' && (
              <SettingsView
                soundEnabled={soundEnabled}
                vibrationEnabled={vibrationEnabled}
                onToggleSound={() => setSoundEnabled(!soundEnabled)}
                onToggleVibration={() => setVibrationEnabled(!vibrationEnabled)}
                onBack={() => setCurrentScreen('home')}
              />
            )}
          </div>

          {/* Android Navigation Bar */}
          <div
            style={{
              height: '48px',
              background: '#0c1017',
              borderTop: '1px solid #161e2e',
              display: 'flex',
              justifyContent: 'space-around',
              alignItems: 'center',
              padding: '0 8px'
            }}
          >
            <button
              onClick={() => setCurrentScreen('home')}
              style={{
                background: 'none',
                border: 'none',
                color: currentScreen === 'home' ? '#eab308' : '#64748b',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '2px',
                cursor: 'pointer'
              }}
            >
              <Compass size={18} />
              <span style={{ fontSize: '10px', fontWeight: 600 }}>Home</span>
            </button>

            <button
              onClick={() => setCurrentScreen('detector')}
              style={{
                background: 'none',
                border: 'none',
                color: currentScreen === 'detector' ? '#eab308' : '#64748b',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '2px',
                cursor: 'pointer'
              }}
            >
              <Activity size={18} />
              <span style={{ fontSize: '10px', fontWeight: 600 }}>Detector</span>
            </button>

            <button
              onClick={() => setCurrentScreen('grid')}
              style={{
                background: 'none',
                border: 'none',
                color: currentScreen === 'grid' ? '#eab308' : '#64748b',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '2px',
                cursor: 'pointer'
              }}
            >
              <Grid3X3 size={18} />
              <span style={{ fontSize: '10px', fontWeight: 600 }}>Grid</span>
            </button>

            <button
              onClick={() => setCurrentScreen('calibration')}
              style={{
                background: 'none',
                border: 'none',
                color: currentScreen === 'calibration' ? '#eab308' : '#64748b',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '2px',
                cursor: 'pointer'
              }}
            >
              <Sliders size={18} />
              <span style={{ fontSize: '10px', fontWeight: 600 }}>Calibrate</span>
            </button>

            <button
              onClick={() => setCurrentScreen('history')}
              style={{
                background: 'none',
                border: 'none',
                color: currentScreen === 'history' ? '#eab308' : '#64748b',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '2px',
                cursor: 'pointer'
              }}
            >
              <History size={18} />
              <span style={{ fontSize: '10px', fontWeight: 600 }}>History</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ----------------- SUB-COMPONENTS (SCREENS) -----------------

function HomeView({
  isSensorAvailable,
  filteredMag,
  deviation,
  isTriggered,
  signalPercent,
  onNavigate
}: {
  isSensorAvailable: boolean;
  filteredMag: number;
  deviation: number;
  isTriggered: boolean;
  signalPercent: number;
  onNavigate: (s: ScreenType) => void;
}) {
  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {/* App Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Compass color="#eab308" size={24} />
          <span style={{ fontSize: '18px', fontWeight: 800, color: '#f8fafc' }}>Metal Detector</span>
        </div>
        <button
          onClick={() => onNavigate('settings')}
          style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
        >
          <Settings size={20} />
        </button>
      </div>

      {/* Primary Sensor Card */}
      <div
        style={{
          background: '#161e2e',
          borderRadius: '16px',
          padding: '16px',
          border: '1px solid #24324a'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '11px', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em' }}>
            LIVE SENSOR
          </span>
          <span
            style={{
              fontSize: '11px',
              fontWeight: 800,
              color: isSensorAvailable ? '#10b981' : '#ef4444',
              display: 'flex',
              alignItems: 'center',
              gap: '4px'
            }}
          >
            <span
              style={{
                width: '7px',
                height: '7px',
                borderRadius: '50%',
                background: isSensorAvailable ? '#10b981' : '#ef4444'
              }}
            />
            {isSensorAvailable ? 'READY (30 Hz)' : 'MISSING'}
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: '12px' }}>
          <div>
            <div
              style={{
                fontSize: '44px',
                fontWeight: 900,
                color: isTriggered ? '#ef4444' : '#f8fafc',
                lineHeight: 1
              }}
            >
              {filteredMag.toFixed(1)}
            </div>
            <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>Microtesla (µT)</div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div
              style={{
                fontSize: '13px',
                fontWeight: 800,
                color: isTriggered ? '#ef4444' : '#10b981'
              }}
            >
              {isTriggered ? 'TARGET DETECTED' : 'SURFACE CLEAR'}
            </div>
            <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '2px' }}>
              Anomaly: +{deviation.toFixed(1)} µT
            </div>
          </div>
        </div>

        {/* Meter progress bar */}
        <div
          style={{
            height: '8px',
            background: '#0a0e17',
            borderRadius: '4px',
            overflow: 'hidden',
            marginTop: '16px'
          }}
        >
          <div
            style={{
              height: '100%',
              width: `${signalPercent}%`,
              background: isTriggered
                ? 'linear-gradient(90deg, #eab308, #ef4444)'
                : 'linear-gradient(90deg, #06b6d4, #10b981)',
              transition: 'width 0.1s ease-out'
            }}
          />
        </div>
      </div>

      {/* Screen Modes 2x2 Grid */}
      <span style={{ fontSize: '11px', fontWeight: 800, color: '#94a3b8', letterSpacing: '0.05em' }}>
        DETECTION MODES
      </span>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
        <button
          onClick={() => onNavigate('detector')}
          style={{
            background: '#161e2e',
            border: '1px solid #24324a',
            borderRadius: '14px',
            padding: '14px',
            textAlign: 'left',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            height: '95px'
          }}
        >
          <Activity size={24} color="#eab308" />
          <div>
            <div style={{ fontSize: '13px', fontWeight: 800, color: '#f8fafc' }}>Live Detector</div>
            <div style={{ fontSize: '10px', color: '#94a3b8' }}>Gauge & Graph</div>
          </div>
        </button>

        <button
          onClick={() => onNavigate('grid')}
          style={{
            background: '#161e2e',
            border: '1px solid #24324a',
            borderRadius: '14px',
            padding: '14px',
            textAlign: 'left',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            height: '95px'
          }}
        >
          <Grid3X3 size={24} color="#06b6d4" />
          <div>
            <div style={{ fontSize: '13px', fontWeight: 800, color: '#f8fafc' }}>Scan Grid</div>
            <div style={{ fontSize: '10px', color: '#94a3b8' }}>Wall & Grass Map</div>
          </div>
        </button>

        <button
          onClick={() => onNavigate('calibration')}
          style={{
            background: '#161e2e',
            border: '1px solid #24324a',
            borderRadius: '14px',
            padding: '14px',
            textAlign: 'left',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            height: '95px'
          }}
        >
          <Sliders size={24} color="#10b981" />
          <div>
            <div style={{ fontSize: '13px', fontWeight: 800, color: '#f8fafc' }}>Calibration</div>
            <div style={{ fontSize: '10px', color: '#94a3b8' }}>Baseline Reset</div>
          </div>
        </button>

        <button
          onClick={() => onNavigate('history')}
          style={{
            background: '#161e2e',
            border: '1px solid #24324a',
            borderRadius: '14px',
            padding: '14px',
            textAlign: 'left',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            height: '95px'
          }}
        >
          <History size={24} color="#f97316" />
          <div>
            <div style={{ fontSize: '13px', fontWeight: 800, color: '#f8fafc' }}>Scan History</div>
            <div style={{ fontSize: '10px', color: '#94a3b8' }}>Saved Sweeps</div>
          </div>
        </button>
      </div>

      {/* Metal Compatibility Guide */}
      <div
        style={{
          background: '#161e2e',
          borderRadius: '14px',
          padding: '14px',
          border: '1px solid #24324a'
        }}
      >
        <span style={{ fontSize: '11px', fontWeight: 800, color: '#94a3b8', letterSpacing: '0.05em' }}>
          METALS DETECTABILITY GUIDE
        </span>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginTop: '10px' }}>
          <div>
            <div style={{ fontSize: '12px', fontWeight: 700, color: '#10b981', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <CheckCircle2 size={13} /> Works With
            </div>
            <ul style={{ listStyle: 'none', padding: 0, marginTop: '6px', fontSize: '11px', color: '#cbd5e1', lineHeight: '18px' }}>
              <li>• Iron & Rebar</li>
              <li>• Steel Pipes</li>
              <li>• Nails & Screws</li>
              <li>• Some Steel Keys</li>
            </ul>
          </div>
          <div>
            <div style={{ fontSize: '12px', fontWeight: 700, color: '#ef4444', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <XCircle size={13} /> Does Not Detect
            </div>
            <ul style={{ listStyle: 'none', padding: 0, marginTop: '6px', fontSize: '11px', color: '#cbd5e1', lineHeight: '18px' }}>
              <li>• Copper & Brass</li>
              <li>• Aluminum Foil/Cans</li>
              <li>• Gold & Silver</li>
              <li>• Plastic / PVC</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Safety Notice Banner */}
      <div
        style={{
          background: '#2b170e',
          border: '1px solid #7c2d12',
          borderRadius: '12px',
          padding: '12px',
          display: 'flex',
          gap: '10px',
          alignItems: 'center'
        }}
      >
        <ShieldAlert size={24} color="#f97316" style={{ flexShrink: 0 }} />
        <p style={{ fontSize: '10.5px', color: '#fed7aa', margin: 0, lineHeight: '15px' }}>
          SAFETY: Senses magnetic variations only. Always use an inspection scanner before drilling into walls.
        </p>
      </div>
    </div>
  );
}

function DetectorView({
  filteredMag,
  rawX,
  rawY,
  rawZ,
  deviation,
  threshold,
  baseline,
  isTriggered,
  signalPercent,
  graphHistory,
  soundEnabled,
  vibrationEnabled,
  onToggleSound,
  onToggleVibration,
  onBack,
  onSaveSession
}: any) {
  // Gauge needle angle calculation (-90 to +90 deg)
  const angle = -90 + (signalPercent / 100) * 180;

  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button
          onClick={onBack}
          style={{ background: 'none', border: 'none', color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
        >
          <ArrowLeft size={18} />
          <span style={{ fontSize: '16px', fontWeight: 800 }}>Live Detector</span>
        </button>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={onToggleSound}
            style={{
              background: soundEnabled ? '#eab30822' : '#1e293b',
              border: 'none',
              borderRadius: '8px',
              padding: '6px',
              cursor: 'pointer',
              color: soundEnabled ? '#eab308' : '#64748b'
            }}
          >
            {soundEnabled ? <Volume2 size={16} /> : <VolumeX size={16} />}
          </button>
          <button
            onClick={onToggleVibration}
            style={{
              background: vibrationEnabled ? '#eab30822' : '#1e293b',
              border: 'none',
              borderRadius: '8px',
              padding: '6px',
              cursor: 'pointer',
              color: vibrationEnabled ? '#eab308' : '#64748b'
            }}
          >
            <Vibrate size={16} />
          </button>
        </div>
      </div>

      {/* Analog Needle Gauge */}
      <div
        style={{
          background: '#161e2e',
          borderRadius: '20px',
          padding: '20px 16px',
          border: '1px solid #24324a',
          textAlign: 'center',
          position: 'relative'
        }}
      >
        <svg viewBox="0 0 200 120" style={{ width: '100%', height: '140px' }}>
          {/* Arc Background */}
          <path
            d="M 20 100 A 80 80 0 0 1 180 100"
            fill="none"
            stroke="#24324a"
            strokeWidth="14"
            strokeLinecap="round"
          />
          {/* Color Zones */}
          <path
            d="M 20 100 A 80 80 0 0 1 90 28"
            fill="none"
            stroke="#06b6d4"
            strokeWidth="14"
            strokeLinecap="round"
          />
          <path
            d="M 90 28 A 80 80 0 0 1 140 40"
            fill="none"
            stroke="#eab308"
            strokeWidth="14"
          />
          <path
            d="M 140 40 A 80 80 0 0 1 180 100"
            fill="none"
            stroke="#ef4444"
            strokeWidth="14"
            strokeLinecap="round"
          />

          {/* Needle */}
          <g transform={`rotate(${angle}, 100, 100)`}>
            <line x1="100" y1="100" x2="100" y2="28" stroke={isTriggered ? '#ef4444' : '#eab308'} strokeWidth="4" strokeLinecap="round" />
            <circle cx="100" cy="100" r="7" fill="#f8fafc" />
          </g>
        </svg>

        <div style={{ marginTop: '-20px' }}>
          <div style={{ fontSize: '46px', fontWeight: 900, color: isTriggered ? '#ef4444' : '#f8fafc' }}>
            {filteredMag.toFixed(1)}
          </div>
          <div style={{ fontSize: '12px', color: '#94a3b8' }}>µT (Microtesla)</div>

          <div
            style={{
              display: 'inline-block',
              marginTop: '6px',
              padding: '4px 10px',
              borderRadius: '6px',
              background: isTriggered ? '#ef444422' : '#1e293b',
              color: isTriggered ? '#ef4444' : '#94a3b8',
              fontSize: '11px',
              fontWeight: 800
            }}
          >
            {isTriggered ? `METAL DETECTED (+${deviation.toFixed(1)} µT)` : 'QUIET AMBIENT'}
          </div>
        </div>

        {/* 3-axis mini breakdown */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-around',
            background: '#0c1017',
            padding: '8px',
            borderRadius: '10px',
            marginTop: '16px',
            fontSize: '11px',
            color: '#94a3b8'
          }}
        >
          <span>X: {rawX.toFixed(1)}</span>
          <span>Y: {rawY.toFixed(1)}</span>
          <span>Z: {rawZ.toFixed(1)}</span>
        </div>
      </div>

      {/* Live Graph Oscilloscope */}
      <div
        style={{
          background: '#161e2e',
          borderRadius: '16px',
          padding: '14px',
          border: '1px solid #24324a'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#94a3b8', marginBottom: '8px' }}>
          <span style={{ fontWeight: 700 }}>LIVE OSCILLOSCOPE (µT)</span>
          <span>Base: {baseline.toFixed(1)} µT</span>
        </div>

        {/* SVG Polyline Graph */}
        <div style={{ width: '100%', height: '90px', background: '#0a0e17', borderRadius: '8px', overflow: 'hidden' }}>
          <svg viewBox="0 0 100 50" preserveAspectRatio="none" style={{ width: '100%', height: '100%' }}>
            {/* Baseline Guide Line */}
            <line x1="0" y1="32" x2="100" y2="32" stroke="#3b82f644" strokeWidth="1" strokeDasharray="2,2" />
            {/* Trigger Threshold Line */}
            <line x1="0" y1="18" x2="100" y2="18" stroke="#ef444444" strokeWidth="1" strokeDasharray="2,2" />

            {/* Signal curve */}
            <polyline
              fill="none"
              stroke="#06b6d4"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              points={graphHistory
                .map((val: number, i: number) => {
                  const x = (i / Math.max(1, graphHistory.length - 1)) * 100;
                  // Scale around 48 +/- 30
                  const y = 32 - ((val - baseline) / 25) * 30;
                  return `${x},${Math.max(2, Math.min(48, y))}`;
                })
                .join(' ')}
            />
          </svg>
        </div>
      </div>

      {/* Save Session CTA */}
      <button
        onClick={onSaveSession}
        style={{
          background: '#eab308',
          color: '#090d14',
          border: 'none',
          borderRadius: '12px',
          padding: '14px',
          fontWeight: 800,
          fontSize: '13px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '8px'
        }}
      >
        <Save size={16} /> Save Sweep to History
      </button>
    </div>
  );
}

function CalibrationView({
  isCalibrating,
  progress,
  samplesCount,
  baseline,
  noise,
  threshold,
  onStart,
  onCancel,
  onBack
}: any) {
  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <button
        onClick={onBack}
        style={{ background: 'none', border: 'none', color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
      >
        <ArrowLeft size={18} />
        <span style={{ fontSize: '16px', fontWeight: 800 }}>Sensor Calibration</span>
      </button>

      {/* Checklist */}
      <div style={{ background: '#161e2e', borderRadius: '16px', padding: '16px', border: '1px solid #24324a' }}>
        <span style={{ fontSize: '11px', fontWeight: 800, color: '#eab308' }}>PRE-CALIBRATION CHECKLIST</span>
        <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px', color: '#cbd5e1' }}>
          <div>1. Remove phone cases with magnets, metals, or kickstands.</div>
          <div>2. Hold the phone in air away from computers, tables, or speakers.</div>
          <div>3. Keep phone stationary for 60 samples (~2 seconds).</div>
        </div>
      </div>

      {/* Calibration Dial */}
      <div
        style={{
          background: '#161e2e',
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #24324a',
          textAlign: 'center'
        }}
      >
        <div
          style={{
            width: '140px',
            height: '140px',
            borderRadius: '50%',
            border: `8px solid ${isCalibrating ? '#eab308' : '#10b981'}`,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto'
          }}
        >
          <span style={{ fontSize: '26px', fontWeight: 900, color: '#f8fafc' }}>
            {isCalibrating ? `${Math.round(progress * 100)}%` : 'READY'}
          </span>
          <span style={{ fontSize: '11px', color: '#94a3b8' }}>
            {isCalibrating ? `${samplesCount} / 60 samples` : 'Calibrated'}
          </span>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '8px',
            marginTop: '20px',
            paddingTop: '16px',
            borderTop: '1px solid #24324a'
          }}
        >
          <div>
            <div style={{ fontSize: '10px', color: '#64748b' }}>BASELINE</div>
            <div style={{ fontSize: '14px', fontWeight: 800, color: '#06b6d4' }}>{baseline.toFixed(1)} µT</div>
          </div>
          <div>
            <div style={{ fontSize: '10px', color: '#64748b' }}>NOISE FLOOR</div>
            <div style={{ fontSize: '14px', fontWeight: 800, color: '#eab308' }}>{noise.toFixed(2)} µT</div>
          </div>
          <div>
            <div style={{ fontSize: '10px', color: '#64748b' }}>TRIGGER</div>
            <div style={{ fontSize: '14px', fontWeight: 800, color: '#f97316' }}>{threshold.toFixed(1)} µT</div>
          </div>
        </div>
      </div>

      {isCalibrating ? (
        <button
          onClick={onCancel}
          style={{
            background: '#ef4444',
            color: '#fff',
            border: 'none',
            borderRadius: '12px',
            padding: '14px',
            fontWeight: 800,
            cursor: 'pointer'
          }}
        >
          Cancel Calibration
        </button>
      ) : (
        <button
          onClick={onStart}
          style={{
            background: '#eab308',
            color: '#090d14',
            border: 'none',
            borderRadius: '12px',
            padding: '14px',
            fontWeight: 800,
            cursor: 'pointer'
          }}
        >
          Calibrate Ambient Baseline (60 Samples)
        </button>
      )}
    </div>
  );
}

function ScanGridView({
  gridMode,
  setGridMode,
  cells,
  selectedCell,
  setSelectedCell,
  currentMag,
  isTriggered,
  onSampleCurrentCell,
  onReset,
  onSaveGrid,
  onBack
}: any) {
  const anomalies = cells.filter((c: any) => c.isAnomaly).length;
  const sampled = cells.filter((c: any) => c.val !== null).length;

  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '14px', height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button
          onClick={onBack}
          style={{ background: 'none', border: 'none', color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
        >
          <ArrowLeft size={18} />
          <span style={{ fontSize: '16px', fontWeight: 800 }}>Scan Matrix</span>
        </button>
        <button
          onClick={onReset}
          style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
        >
          <RotateCcw size={16} />
        </button>
      </div>

      {/* Mode Selector */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
        <button
          onClick={() => setGridMode('Wall (Rebar / Nails)')}
          style={{
            padding: '8px',
            borderRadius: '8px',
            border: 'none',
            background: gridMode.startsWith('Wall') ? '#eab308' : '#1e293b',
            color: gridMode.startsWith('Wall') ? '#090d14' : '#94a3b8',
            fontSize: '11px',
            fontWeight: 800,
            cursor: 'pointer'
          }}
        >
          Wall (Stud/Rebar)
        </button>
        <button
          onClick={() => setGridMode('Grass (Ground / Relics)')}
          style={{
            padding: '8px',
            borderRadius: '8px',
            border: 'none',
            background: gridMode.startsWith('Grass') ? '#06b6d4' : '#1e293b',
            color: gridMode.startsWith('Grass') ? '#090d14' : '#94a3b8',
            fontSize: '11px',
            fontWeight: 800,
            cursor: 'pointer'
          }}
        >
          Grass / Ground
        </button>
      </div>

      {/* Stats summary */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-around',
          background: '#161e2e',
          padding: '10px',
          borderRadius: '10px',
          fontSize: '11px'
        }}
      >
        <span>Scanned: <b>{sampled}/30</b></span>
        <span>Targets: <b style={{ color: anomalies > 0 ? '#ef4444' : '#10b981' }}>{anomalies}</b></span>
        <span>Live: <b style={{ color: '#06b6d4' }}>{currentMag.toFixed(1)} µT</b></span>
      </div>

      {/* 6x5 Interactive Matrix */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 1fr)',
          gap: '6px',
          background: '#161e2e',
          padding: '10px',
          borderRadius: '12px'
        }}
      >
        {cells.map((cell: any, idx: number) => {
          const isSel = idx === selectedCell;
          let bg = '#0c1017';
          if (cell.isAnomaly) bg = '#ef4444';
          else if (cell.val !== null) bg = '#15803d';
          else if (isSel) bg = '#eab30844';

          return (
            <div
              key={idx}
              onClick={() => setSelectedCell(idx)}
              style={{
                aspectRatio: '1',
                background: bg,
                borderRadius: '6px',
                border: isSel ? '2px solid #eab308' : '1px solid #24324a',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                fontSize: '10px'
              }}
            >
              {cell.val !== null ? (
                <>
                  <span style={{ fontWeight: 800, color: '#fff' }}>{Math.round(cell.val)}</span>
                  <span style={{ fontSize: '8px', color: '#e2e8f0' }}>µT</span>
                </>
              ) : (
                <span style={{ color: '#475569' }}>{Math.floor(idx / 5) + 1},{ (idx % 5) + 1 }</span>
              )}
            </div>
          );
        })}
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', gap: '8px', marginTop: 'auto' }}>
        <button
          onClick={onSampleCurrentCell}
          style={{
            flex: 1.5,
            background: '#eab308',
            color: '#090d14',
            border: 'none',
            borderRadius: '10px',
            padding: '12px',
            fontWeight: 800,
            fontSize: '12px',
            cursor: 'pointer'
          }}
        >
          Sample Cell ({Math.floor(selectedCell / 5) + 1},{ (selectedCell % 5) + 1 })
        </button>
        <button
          onClick={onSaveGrid}
          style={{
            flex: 1,
            background: '#1e293b',
            color: '#fff',
            border: 'none',
            borderRadius: '10px',
            padding: '12px',
            fontWeight: 700,
            fontSize: '12px',
            cursor: 'pointer'
          }}
        >
          Save Map
        </button>
      </div>
    </div>
  );
}

function HistoryView({ records, onDelete, onClearAll, onBack }: any) {
  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button
          onClick={onBack}
          style={{ background: 'none', border: 'none', color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
        >
          <ArrowLeft size={18} />
          <span style={{ fontSize: '16px', fontWeight: 800 }}>Scan History</span>
        </button>
        {records.length > 0 && (
          <button
            onClick={onClearAll}
            style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer' }}
          >
            <Trash2 size={16} />
          </button>
        )}
      </div>

      {records.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px 10px', color: '#64748b' }}>
          <History size={48} style={{ margin: '0 auto 12px' }} />
          <div style={{ fontSize: '14px', fontWeight: 700, color: '#f8fafc' }}>No Scans Saved Yet</div>
          <p style={{ fontSize: '12px', marginTop: '4px' }}>Run a sweep or scan grid and press save.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {records.map((r: ScanRecord) => (
            <div
              key={r.id}
              style={{
                background: '#161e2e',
                borderRadius: '14px',
                padding: '14px',
                border: '1px solid #24324a'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '13px', fontWeight: 800, color: '#f8fafc' }}>{r.title}</span>
                <button
                  onClick={() => onDelete(r.id)}
                  style={{ background: 'none', border: 'none', color: '#64748b', cursor: 'pointer' }}
                >
                  <Trash2 size={14} />
                </button>
              </div>

              <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>
                {new Date(r.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} • {r.type}
              </div>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(3, 1fr)',
                  gap: '8px',
                  marginTop: '10px',
                  paddingTop: '8px',
                  borderTop: '1px solid #24324a',
                  fontSize: '11px'
                }}
              >
                <div>
                  <div style={{ color: '#64748b', fontSize: '10px' }}>PEAK</div>
                  <div style={{ fontWeight: 800, color: '#ef4444' }}>{r.maxMicroTesla.toFixed(1)} µT</div>
                </div>
                <div>
                  <div style={{ color: '#64748b', fontSize: '10px' }}>TARGETS</div>
                  <div style={{ fontWeight: 800, color: r.targetsDetectedCount > 0 ? '#eab308' : '#10b981' }}>
                    {r.targetsDetectedCount}
                  </div>
                </div>
                <div>
                  <div style={{ color: '#64748b', fontSize: '10px' }}>DURATION</div>
                  <div style={{ fontWeight: 800, color: '#cbd5e1' }}>{r.durationSeconds}s</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function SettingsView({ soundEnabled, vibrationEnabled, onToggleSound, onToggleVibration, onBack }: any) {
  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <button
        onClick={onBack}
        style={{ background: 'none', border: 'none', color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
      >
        <ArrowLeft size={18} />
        <span style={{ fontSize: '16px', fontWeight: 800 }}>Settings</span>
      </button>

      {/* Audio/Vibe */}
      <div style={{ background: '#161e2e', borderRadius: '16px', padding: '16px', border: '1px solid #24324a' }}>
        <span style={{ fontSize: '11px', fontWeight: 800, color: '#eab308' }}>ALERTS & FEEDBACK</span>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
          <div>
            <div style={{ fontSize: '13px', fontWeight: 700 }}>Audio Beep Alert</div>
            <div style={{ fontSize: '11px', color: '#64748b' }}>Frequency modulates with signal anomaly</div>
          </div>
          <input type="checkbox" checked={soundEnabled} onChange={onToggleSound} style={{ accentColor: '#eab308' }} />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '14px', paddingTop: '12px', borderTop: '1px solid #24324a' }}>
          <div>
            <div style={{ fontSize: '13px', fontWeight: 700 }}>Haptic Vibration</div>
            <div style={{ fontSize: '11px', color: '#64748b' }}>Geiger-counter vibration pulses</div>
          </div>
          <input type="checkbox" checked={vibrationEnabled} onChange={onToggleVibration} style={{ accentColor: '#eab308' }} />
        </div>
      </div>

      {/* Guidelines */}
      <div style={{ background: '#161e2e', borderRadius: '16px', padding: '16px', border: '1px solid #24324a' }}>
        <span style={{ fontSize: '11px', fontWeight: 800, color: '#06b6d4' }}>HOW TO SCAN ACCURATELY</span>
        <ul style={{ paddingLeft: '18px', marginTop: '10px', fontSize: '12px', color: '#cbd5e1', lineHeight: '20px' }}>
          <li>Remove magnetic accessories or metal stands.</li>
          <li>Calibrate in clear air away from computers.</li>
          <li>Hold phone 1 to 5 cm directly above surface.</li>
          <li>Move slowly in orthogonal cross patterns.</li>
          <li>Re-verify signals from perpendicular angle.</li>
        </ul>
      </div>
    </div>
  );
}

function UnsupportedView({ onRetry }: { onRetry: () => void }) {
  return (
    <div
      style={{
        padding: '30px 20px',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%'
      }}
    >
      <div
        style={{
          width: '64px',
          height: '64px',
          borderRadius: '50%',
          background: '#ef444422',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '16px'
        }}
      >
        <AlertTriangle size={32} color="#ef4444" />
      </div>
      <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#f8fafc', marginBottom: '8px' }}>
        Magnetometer Not Found
      </h2>
      <p style={{ fontSize: '12px', color: '#94a3b8', lineHeight: '18px', marginBottom: '24px' }}>
        This device does not have a physical magnetic sensor (<code>Sensor.TYPE_MAGNETIC_FIELD</code>).
        A physical magnetometer is required to detect ferromagnetic anomalies.
      </p>
      <button
        onClick={onRetry}
        style={{
          background: '#eab308',
          color: '#090d14',
          border: 'none',
          borderRadius: '12px',
          padding: '12px 24px',
          fontWeight: 800,
          cursor: 'pointer'
        }}
      >
        Simulate Magnetometer
      </button>
    </div>
  );
}
