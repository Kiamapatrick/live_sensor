# Live Sensor - ICS 2300 Lab_06_hm6

An Android app demonstrating live sensor readings, lifecycle management, and data interpretation using the light sensor (TYPE_LIGHT) as the primary sensor.

## Features

### 1. Experiment Question
- Displayed at the top: "Does the light sensor's reading change measurably between covered, normal room light and torch/bright light?"

### 2. Live Panel
- **Current raw value** (lux)
- **Smoothed value** using Exponential Moving Average (EMA) with α = 0.2
- **Unit** (lux)
- **Sensor identity**: name, vendor, type, max range, resolution, power
- **Source label**: "MEASURED (hardware)" or "SIMULATED" - clearly distinguished

### 3. Controlled Conditions & Recording
- Three conditions via Spinner: **Covered** / **Room light** / **Bright light**
- **Record observation** button stores:
  - Timestamp (HH:mm:ss.SSS)
  - Condition
  - Raw value
  - Smoothed value
  - Accuracy (HIGH/MEDIUM/LOW/UNRELIABLE/NO_CONTACT)
  - Source (Measured/Simulated)

### 4. Observation Table (RecyclerView)
- Scrollable, timestamped table
- Survives rotation (ViewModel)
- Highlights unreliable rows (red background)
- Marks simulated rows (orange "SIMULATED" label)

### 5. Raw vs Smoothed Comparison
- EMA formula displayed on screen: `smoothed = α × raw + (1−α) × previous, α = 0.2`
- Both values shown in live panel and in each table row

### 6. Event Timing
- Average interval between `onSensorChanged` events (ms)
- Effective events/second (Hz)
- Note: "Requested: SENSOR_DELAY_NORMAL (hint, not guaranteed)"

### 7. Lifecycle Status
- Listener state: "Listener registered / unregistered"
- Counters for register/unregister calls (logged with Log.d for screenshot evidence)

## Lifecycle Implementation
- **Register** in `onResume()` - matching visible/active pair
- **Unregister** in `onPause()` - matching visible/active pair
- **Boolean guard** prevents duplicate registration
- **Log.d** on every register/unregister for evidence
- **No registration in onCreate**, never leaves listener registered after onPause

## Missing/Unreliable Sensor Handling
- If `getDefaultSensor(TYPE_LIGHT)` returns null:
  - Shows "Sensor not available on this device"
  - Disables record button (until simulated mode enabled)
  - Offers "Use simulated data" switch
- **Simulated mode**:
  - Generates synthetic values via Handler/Runnable (~200ms interval)
  - Clearly labeled **SIMULATED** everywhere (live panel, table rows)
  - Never presented as measured data
- **Accuracy warnings**:
  - Shows warning for UNRELIABLE or NO_CONTACT
  - Marks rows as unreliable in table
  - Ignores NaN/negative lux values
- **Disclaimer**: "Readings are uncalibrated device values, not instrument-grade measurements."

## Rotation Handling
- **ViewModel** preserves:
  - Observation table
  - Selected condition
  - Simulated/measured mode
  - Register/unregister counters
  - Listener state
- **No duplicate listener** created on rotation

## Code Quality
- **SensorData.java**: Plain Java class for smoothing & timing (testable, no Android deps)
- **Unit tests** for EMA smoothing and interval calculation
- Clear comments on lifecycle, smoothing rule, timing calculation
- No external libraries beyond AndroidX

## Building & Running

### Prerequisites
- Android Studio (latest)
- Android SDK 24+ (minSdk 24, targetSdk 37)

### Build
```bash
./gradlew assembleDebug
```

### Run on Emulator
1. Create AVD with API 24+ (e.g., Pixel 6, API 33)
2. **For sensor simulation**: Extended Controls → Virtual sensors → Light
   - Adjust the light level slider to simulate Covered/Room/Bright conditions
3. Run the app

### Run on Physical Device
1. Enable USB debugging
2. Connect device
3. Run the app from Android Studio or:
   ```bash
   ./gradlew installDebug
   ```

### Testing
```bash
./gradlew test
```
Runs unit tests for `SensorData` (smoothing and timing calculations).

## Assignment Checklist Mapping

| Requirement | Implementation |
|-------------|----------------|
| Experiment question | `tvExperimentQuestion` in activity_main.xml |
| Live panel (value, unit, sensor identity, source label) | `livePanel` LinearLayout with TextViews |
| Three conditions (Covered/Room/Bright) | Spinner with 3 options |
| Record observation button | `btnRecord` with timestamp, condition, raw, smoothed, accuracy, source |
| Scrollable observation table | RecyclerView with ObservationAdapter |
| Survives rotation | SensorViewModel (ViewModel) |
| Raw vs smoothed (EMA α=0.2) | SensorData.applySmoothing(), displayed in UI |
| Smoothing rule on screen | `tvSmoothingRule` TextView |
| Event timing (avg interval, Hz, hint note) | SensorData timing methods, `tvTiming` |
| Listener state & counters | ViewModel counters, `tvListenerState`, `tvRegisterCounter` |
| Register in onResume, unregister in onPause | MainActivity.onResume/onPause with boolean guard |
| Log.d on register/unregister | Log.d(TAG, ...) in registerListener/unregisterListener |
| No register in onCreate, no listener after onPause | Verified in lifecycle methods |
| Missing sensor handling | checkSensorAvailability() shows message, enables simulated switch |
| Simulated mode with Handler | SimulatedSensor class, labeled SIMULATED everywhere |
| Accuracy warnings (UNRELIABLE/NO_CONTACT) | handleSensorEvent checks accuracy, highlights rows |
| Ignore NaN/negative lux | handleSensorEvent filters invalid values |
| Uncalibrated disclaimer | `tvUncalibratedNote` TextView |
| Rotation survival | ViewModel preserves all state |
| Unit tests (smoothing, interval) | SensorDataTest.java |
| Plain Java testable class | SensorData.java (no Android dependencies) |

## Project Structure
```
app/src/main/java/com/example/livesensor/
├── MainActivity.java          # Main activity, sensor listener, UI logic
├── SensorViewModel.java       # ViewModel for rotation survival
├── SensorData.java            # Plain Java: EMA smoothing & timing (testable)
├── SensorDataTest.java        # JUnit tests for SensorData
├── Observation.java           # Data model for table rows
├── ObservationAdapter.java    # RecyclerView adapter
├── SimulatedSensor.java       # Synthetic data generator
└── ExampleUnitTest.java       # Template test

app/src/main/res/layout/
├── activity_main.xml          # Main UI layout
└── item_observation.xml       # RecyclerView item layout
```

## License
Educational use for ICS 2300 Lab_06_hm6.