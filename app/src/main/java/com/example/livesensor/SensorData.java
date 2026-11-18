package com.example.livesensor;

/**
 * Plain Java class for sensor data processing.
 * Contains the smoothing (EMA) and timing calculations.
 * Kept separate from Android framework for unit testability.
 */
public class SensorData {

    // EMA smoothing factor
    public static final float ALPHA = 0.2f;

    private float smoothedValue = Float.NaN;
    private long lastEventTimestampNanos = 0;
    private double sumIntervalsMs = 0;
    private int intervalCount = 0;

    /**
     * Apply exponential moving average smoothing.
     * smoothed = alpha * raw + (1 - alpha) * previous_smoothed
     *
     * @param rawValue the raw sensor reading
     * @return the smoothed value
     */
    public float applySmoothing(float rawValue) {
        if (Float.isNaN(smoothedValue)) {
            // First value: initialize with raw
            smoothedValue = rawValue;
        } else {
            // EMA formula: smoothed = α * raw + (1-α) * previous
            smoothedValue = ALPHA * rawValue + (1 - ALPHA) * smoothedValue;
        }
        return smoothedValue;
    }

    /**
     * Get the current smoothed value without updating.
     *
     * @return current smoothed value or NaN if not initialized
     */
    public float getSmoothedValue() {
        return smoothedValue;
    }

    /**
     * Reset the smoothing state.
     */
    public void resetSmoothing() {
        smoothedValue = Float.NaN;
    }

    /**
     * Record a sensor event timestamp and calculate interval statistics.
     * Called from onSensorChanged with event.timestamp (nanoseconds).
     *
     * @param eventTimestampNanos the timestamp from SensorEvent
     * @return average interval in milliseconds, or -1 if not enough data
     */
    public float recordEventTimestamp(long eventTimestampNanos) {
        if (lastEventTimestampNanos != 0) {
            long intervalNanos = eventTimestampNanos - lastEventTimestampNanos;
            double intervalMs = intervalNanos / 1_000_000.0;
            sumIntervalsMs += intervalMs;
            intervalCount++;
        }
        lastEventTimestampNanos = eventTimestampNanos;

        if (intervalCount == 0) {
            return -1f;
        }
        return (float) (sumIntervalsMs / intervalCount);
    }

    /**
     * Get the average interval between events in milliseconds.
     *
     * @return average interval in ms, or -1 if no intervals recorded
     */
    public float getAverageIntervalMs() {
        if (intervalCount == 0) {
            return -1f;
        }
        return (float) (sumIntervalsMs / intervalCount);
    }

    /**
     * Get the effective event rate in Hz (events per second).
     *
     * @return events per second, or -1 if no intervals recorded
     */
    public float getEventRateHz() {
        float avgInterval = getAverageIntervalMs();
        if (avgInterval <= 0) {
            return -1f;
        }
        return 1000.0f / avgInterval;
    }

    /**
     * Get the number of intervals recorded.
     *
     * @return interval count
     */
    public int getIntervalCount() {
        return intervalCount;
    }

    /**
     * Reset the timing statistics.
     */
    public void resetTiming() {
        lastEventTimestampNanos = 0;
        sumIntervalsMs = 0;
        intervalCount = 0;
    }

    /**
     * Reset all state (smoothing and timing).
     */
    public void resetAll() {
        resetSmoothing();
        resetTiming();
    }
}