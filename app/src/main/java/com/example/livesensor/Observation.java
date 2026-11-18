package com.example.livesensor;

/**
 * Model class for a single sensor observation record.
 */
public class Observation {
    public final String timestamp;      // HH:mm:ss.SSS
    public final String condition;      // Covered / Room light / Bright light
    public final float rawValue;        // raw sensor reading
    public final float smoothedValue;   // EMA smoothed value
    public final int accuracy;          // SensorManager.SENSOR_STATUS_*
    public final String source;         // "Measured" or "Simulated"
    public final boolean unreliable;    // true if accuracy is UNRELIABLE or NO_CONTACT

    public Observation(String timestamp, String condition, float rawValue,
                       float smoothedValue, int accuracy, String source) {
        this.timestamp = timestamp;
        this.condition = condition;
        this.rawValue = rawValue;
        this.smoothedValue = smoothedValue;
        this.accuracy = accuracy;
        this.source = source;
        this.unreliable = (accuracy == android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE
                || accuracy == android.hardware.SensorManager.SENSOR_STATUS_NO_CONTACT);
    }

    public String getAccuracyLabel() {
        switch (accuracy) {
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "HIGH";
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "MEDIUM";
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "LOW";
            case android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE:
                return "UNRELIABLE";
            case android.hardware.SensorManager.SENSOR_STATUS_NO_CONTACT:
                return "NO_CONTACT";
            default:
                return "UNKNOWN(" + accuracy + ")";
        }
    }
}