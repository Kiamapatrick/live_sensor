package com.example.livesensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "LiveSensor";
    private static final float EMA_ALPHA = 0.2f;

    // UI Components
    private TextView tvSensorIdentity;
    private TextView tvSourceLabel;
    private SwitchCompat switchSimulated;
    private TextView tvRawValue;
    private TextView tvSmoothedValue;
    private TextView tvAccuracy;
    private TextView tvTiming;
    private Spinner spinnerCondition;
    private TextView tvListenerState;
    private TextView tvRegisterCounter;
    private ObservationAdapter observationAdapter;

    // Sensor components
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorData sensorData;
    private SimulatedSensor simulatedSensor;

    // ViewModel
    private SensorViewModel viewModel;

    // State
    private boolean listenerRegistered = false; // Guard against duplicate registration
    private boolean isSimulatedMode = false;
    private boolean hasReading = false;       // true once a real/simulated event has arrived
    private float lastRaw = 0f;
    private float lastSmoothed = 0f;
    private int lastAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
    private int currentConditionIndex = 1; // Default: Room light
    private static final String[] CONDITIONS = {"Covered", "Room light", "Bright light"};

    // Timestamp formatter for observations
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ViewModel (survives rotation)
        viewModel = new ViewModelProvider(this).get(SensorViewModel.class);

        // Initialize SensorData (plain Java, testable)
        sensorData = new SensorData();

        // Initialize UI
        initViews();
        initSpinner();
        initRecyclerView();
        initSimulatedSensor();

        // Initialize SensorManager and sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Check sensor availability
        checkSensorAvailability();

        // Restore state from ViewModel (survives rotation)
        restoreStateFromViewModel();

        // Observe ViewModel changes
        observeViewModel();
    }

    private void initViews() {
        tvSensorIdentity = findViewById(R.id.tvSensorIdentity);
        tvSourceLabel = findViewById(R.id.tvSourceLabel);
        switchSimulated = findViewById(R.id.switchSimulated);
        tvRawValue = findViewById(R.id.tvRawValue);
        tvSmoothedValue = findViewById(R.id.tvSmoothedValue);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvTiming = findViewById(R.id.tvTiming);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        tvListenerState = findViewById(R.id.tvListenerState);
        tvRegisterCounter = findViewById(R.id.tvRegisterCounter);

        // Record button
        findViewById(R.id.btnRecord).setOnClickListener(v -> recordObservation());

        // Simulated mode switch
        switchSimulated.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setSimulatedMode(isChecked);
        });
    }

    private void initSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CONDITIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(adapter);
        spinnerCondition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentConditionIndex = position;
                viewModel.setSelectedConditionIndex(position);
                if (simulatedSensor != null) {
                    simulatedSensor.setCondition(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initRecyclerView() {
        observationAdapter = new ObservationAdapter();
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvObservations);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(observationAdapter);
    }

    private void initSimulatedSensor() {
        simulatedSensor = new SimulatedSensor((luxValue, timestampNanos, accuracy) ->
                handleSensorEvent(luxValue, timestampNanos, accuracy, true));
    }

    private void checkSensorAvailability() {
        if (lightSensor == null) {
            // No light sensor available
            viewModel.setSensorAvailable(false);
            tvSensorIdentity.setText(R.string.sensor_not_available);
            tvSourceLabel.setText(R.string.source_simulated);
            tvSourceLabel.setTextColor(0xFFFF9800); // Orange
            switchSimulated.setEnabled(true);
            switchSimulated.setChecked(true);
            setSimulatedMode(true); // enables Record button (simulated data is allowed, labelled)
        } else {
            // Light sensor available
            viewModel.setSensorAvailable(true);
            String identity = getString(R.string.sensor_identity_format,
                    lightSensor.getName(),
                    lightSensor.getVendor(),
                    lightSensor.getType(),
                    lightSensor.getMaximumRange(),
                    lightSensor.getResolution(),
                    lightSensor.getPower());
            tvSensorIdentity.setText(identity);
            tvSourceLabel.setText(R.string.source_measured);
            tvSourceLabel.setTextColor(0xFF2E7D32); // Green
            switchSimulated.setEnabled(true);
            switchSimulated.setChecked(false);
            setSimulatedMode(false);
            findViewById(R.id.btnRecord).setEnabled(true);
        }
    }

    private void restoreStateFromViewModel() {
        // Restore simulated mode
        boolean savedSimulated = Boolean.TRUE.equals(viewModel.getSimulatedMode().getValue());
        if (savedSimulated != isSimulatedMode) {
            switchSimulated.setChecked(savedSimulated); // listener -> setSimulatedMode updates label + button
        }

        // Restore selected condition
        Integer savedCondition = viewModel.getSelectedConditionIndex().getValue();
        if (savedCondition != null) {
            currentConditionIndex = savedCondition;
            spinnerCondition.setSelection(savedCondition);
        }

        // Restore observations
        List<Observation> savedObservations = viewModel.getObservations().getValue();
        if (savedObservations != null) {
            observationAdapter.setObservations(savedObservations);
        }

        // Restore register/unregister counters
        Integer regCount = viewModel.getRegisterCount().getValue();
        Integer unregCount = viewModel.getUnregisterCount().getValue();
        if (regCount != null && unregCount != null) {
            updateCounterDisplay(regCount, unregCount);
        }

        // Restore listener state (but DON'T register here - only in onResume)
        Boolean listenerState = viewModel.getListenerRegistered().getValue();
        if (listenerState != null) {
            listenerRegistered = listenerState;
            updateListenerStateDisplay(listenerState);
        }
    }

    private void observeViewModel() {
        // Observe simulated mode changes (from rotation restore)
        viewModel.getSimulatedMode().observe(this, simulated -> {
            if (simulated != null && simulated != isSimulatedMode) {
                isSimulatedMode = simulated;
                switchSimulated.setChecked(simulated);
                if (simulated) {
                    startSimulatedMode();
                } else {
                    stopSimulatedMode();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register listener in onResume (matching visible/active pair)
        // Guard with boolean to prevent duplicate registration
        if (!listenerRegistered) {
            registerListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister listener in onPause (matching visible/active pair)
        // Guard with boolean to prevent duplicate unregistration
        if (listenerRegistered) {
            unregisterListener();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up simulated sensor
        if (simulatedSensor != null) {
            simulatedSensor.stop();
        }
    }

    /**
     * Register the SensorEventListener.
     * Called from onResume. Guarded by listenerRegistered boolean.
     * Logs registration for screenshot evidence.
     */
    private void registerListener() {
        if (listenerRegistered) {
            Log.d(TAG, "registerListener: already registered, skipping");
            return;
        }

        // Start a fresh timing window: the time spent in the background must not be
        // counted as an "interval between events".
        sensorData.resetTiming();

        if (!isSimulatedMode && lightSensor != null) {
            // Register for light sensor with NORMAL delay
            // Note: requested delay is a HINT, not a guaranteed rate
            boolean success = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            if (success) {
                listenerRegistered = true;
                viewModel.setListenerRegistered(true);
                viewModel.incrementRegisterCount();
                Log.d(TAG, "Listener REGISTERED for light sensor (SENSOR_DELAY_NORMAL)");
                updateListenerStateDisplay(true);
                updateCounterDisplay();
            } else {
                Log.e(TAG, "Failed to register listener for light sensor");
                Toast.makeText(this, R.string.failed_register_listener, Toast.LENGTH_SHORT).show();
            }
        } else if (isSimulatedMode) {
            // In simulated mode, start the simulated sensor
            listenerRegistered = true;
            viewModel.setListenerRegistered(true);
            viewModel.incrementRegisterCount();
            Log.d(TAG, "Listener REGISTERED (simulated mode)");
            updateListenerStateDisplay(true);
            updateCounterDisplay();
            startSimulatedMode();
        }
    }

    /**
     * Unregister the SensorEventListener.
     * Called from onPause. Guarded by listenerRegistered boolean.
     * Logs unregistration for screenshot evidence.
     */
    private void unregisterListener() {
        if (!listenerRegistered) {
            Log.d(TAG, "unregisterListener: not registered, skipping");
            return;
        }

        if (!isSimulatedMode) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Listener UNREGISTERED for light sensor");
        } else {
            stopSimulatedMode();
            Log.d(TAG, "Listener UNREGISTERED (simulated mode)");
        }

        listenerRegistered = false;
        viewModel.setListenerRegistered(false);
        viewModel.incrementUnregisterCount();
        updateListenerStateDisplay(false);
        updateCounterDisplay();
    }

    private void startSimulatedMode() {
        if (simulatedSensor != null) {
            simulatedSensor.setCondition(currentConditionIndex);
            simulatedSensor.start();
        }
    }

    private void stopSimulatedMode() {
        if (simulatedSensor != null) {
            simulatedSensor.stop();
        }
    }

    private void setSimulatedMode(boolean simulated) {
        if (simulated == isSimulatedMode) return;

        // If switching modes while listener is registered, need to swap
        boolean wasRegistered = listenerRegistered;
        if (wasRegistered) {
            unregisterListener();
        }

        isSimulatedMode = simulated;
        viewModel.setSimulatedMode(simulated);
        sensorData.resetAll();   // never mix simulated and measured values in one EMA
        hasReading = false;

        if (simulated) {
            tvSourceLabel.setText(R.string.source_simulated);
            tvSourceLabel.setTextColor(0xFFFF9800); // Orange
            findViewById(R.id.btnRecord).setEnabled(true);
        } else {
            tvSourceLabel.setText(R.string.source_measured);
            tvSourceLabel.setTextColor(0xFF2E7D32); // Green
            // Enable record button only if real sensor is available
            findViewById(R.id.btnRecord).setEnabled(lightSensor != null);
        }

        // Re-register if it was registered before
        if (wasRegistered) {
            registerListener();
        }
    }

    /**
     * Handle sensor event from either real sensor or simulated sensor.
     *
     * @param value      sensor reading (lux for light sensor)
     * @param timestamp  event timestamp in nanoseconds
     * @param accuracy   sensor accuracy constant
     * @param simulated  true if from simulated sensor
     */
    private void handleSensorEvent(float value, long timestamp, int accuracy, boolean simulated) {
        // Ignore NaN and negative lux values
        if (Float.isNaN(value) || value < 0) {
            return;
        }

        // Apply EMA smoothing
        float smoothed = sensorData.applySmoothing(value);

        // Record timing
        float avgInterval = sensorData.recordEventTimestamp(timestamp);
        float eventRate = sensorData.getEventRateHz();

        // Update UI on main thread
        runOnUiThread(() -> {
            hasReading = true;
            lastRaw = value;
            lastSmoothed = smoothed;
            lastAccuracy = accuracy;
            tvRawValue.setText(getString(R.string.raw_value_format, value));
            tvSmoothedValue.setText(getString(R.string.smoothed_value_format, EMA_ALPHA, smoothed));

            String accuracyLabel = getAccuracyLabel(accuracy);
            tvAccuracy.setText(getString(R.string.accuracy_format, accuracyLabel));
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                    || accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT) {
                tvAccuracy.setTextColor(0xFFC62828); // Red
            } else {
                tvAccuracy.setTextColor(0xFF000000); // Black
            }

            // Timing display
            if (avgInterval > 0) {
                tvTiming.setText(getString(R.string.timing_format, avgInterval, eventRate));
            } else {
                tvTiming.setText(R.string.timing_initial);
            }
        });
    }

    private String getAccuracyLabel(int accuracy) {
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return getString(R.string.accuracy_high);
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return getString(R.string.accuracy_medium);
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return getString(R.string.accuracy_low);
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return getString(R.string.accuracy_unreliable);
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                return getString(R.string.accuracy_no_contact);
            default:
                return getString(R.string.accuracy_unknown, accuracy);
        }
    }

    private void recordObservation() {
        if (!hasReading) {
            Toast.makeText(this, R.string.no_reading_yet, Toast.LENGTH_SHORT).show();
            return;
        }
        String timestamp = timeFormat.format(new Date());
        String condition = CONDITIONS[currentConditionIndex];
        String source = isSimulatedMode ? "Simulated" : "Measured";

        Observation obs = new Observation(timestamp, condition, lastRaw, lastSmoothed, lastAccuracy, source);
        observationAdapter.addObservation(obs);
        viewModel.addObservation(obs);

        Toast.makeText(this, getString(R.string.observation_recorded, condition), Toast.LENGTH_SHORT).show();
    }

    private void updateListenerStateDisplay(boolean registered) {
        tvListenerState.setText(registered ? R.string.listener_registered : R.string.listener_unregistered);
        tvListenerState.setTextColor(registered ? 0xFF2E7D32 : 0xFFC62828);
    }

    private void updateCounterDisplay() {
        Integer reg = viewModel.getRegisterCount().getValue();
        Integer unreg = viewModel.getUnregisterCount().getValue();
        if (reg != null && unreg != null) {
            updateCounterDisplay(reg, unreg);
        }
    }

    private void updateCounterDisplay(int reg, int unreg) {
        tvRegisterCounter.setText(getString(R.string.register_counter_format, reg, unreg));
    }

    // ==================== SensorEventListener Implementation ====================

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Light sensor event
            float lux = event.values[0];
            long timestamp = event.timestamp; // nanoseconds
            int accuracy = event.accuracy;
            handleSensorEvent(lux, timestamp, accuracy, false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy changes are reflected in onSensorChanged events
        Log.d(TAG, "onAccuracyChanged: " + sensor.getName() + " -> " + getAccuracyLabel(accuracy));
    }
}