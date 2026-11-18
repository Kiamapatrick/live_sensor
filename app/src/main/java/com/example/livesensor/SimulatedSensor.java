package com.example.livesensor;

import android.os.Handler;
import android.os.Looper;

/**
 * Generates simulated light sensor data for testing on devices without a light sensor.
 * Runs on a Handler with a fixed delay to simulate SENSOR_DELAY_NORMAL (~200ms).
 */
public class SimulatedSensor {

    public interface Callback {
        void onSensorChanged(float luxValue, long timestampNanos, int accuracy);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable;
    private final Callback callback;

    private boolean running = false;
    private int conditionIndex = 1; // 0=Covered, 1=Room light, 2=Bright light
    private long baseTimestamp = System.nanoTime();

    // Simulated values for each condition (lux)
    private static final float[] CONDITION_BASE_VALUES = {5f, 300f, 10000f};
    private static final float[] CONDITION_VARIANCE = {2f, 50f, 2000f};

    public SimulatedSensor(Callback callback) {
        this.callback = callback;
        this.runnable = new Runnable() {
            @Override
            public void run() {
                if (running && callback != null) {
                    generateAndEmit();
                    handler.postDelayed(this, 200); // ~5 Hz, similar to SENSOR_DELAY_NORMAL
                }
            }
        };
    }

    private void generateAndEmit() {
        float base = CONDITION_BASE_VALUES[conditionIndex];
        float variance = CONDITION_VARIANCE[conditionIndex];
        // Add some random noise
        float noise = (float) (Math.random() * 2 - 1) * variance;
        float value = base + noise;
        // Ensure non-negative
        if (value < 0) value = 0;

        // Simulate accuracy: mostly HIGH, occasionally MEDIUM
        int accuracy = Math.random() < 0.9 ?
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH :
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;

        // Real elapsed time (Handler jitter included), not a faked constant 200 ms
        callback.onSensorChanged(value, System.nanoTime(), accuracy);
    }

    public void start() {
        if (!running) {
            running = true;
            baseTimestamp = System.nanoTime();
            handler.post(runnable);
        }
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(runnable);
    }

    public void setCondition(int index) {
        if (index >= 0 && index < CONDITION_BASE_VALUES.length) {
            conditionIndex = index;
        }
    }

    public int getCondition() {
        return conditionIndex;
    }

    public boolean isRunning() {
        return running;
    }
}