package com.example.livesensor;

import org.junit.Test;
import static org.junit.Assert.*;

public class SensorDataTest {

    private static final float DELTA = 0.0001f;

    @Test
    public void testSmoothingFirstValue() {
        SensorData data = new SensorData();
        float result = data.applySmoothing(100.0f);
        // First value should be returned as-is (initialization)
        assertEquals(100.0f, result, DELTA);
        assertEquals(100.0f, data.getSmoothedValue(), DELTA);
    }

    @Test
    public void testSmoothingSecondValue() {
        SensorData data = new SensorData();
        data.applySmoothing(100.0f); // First value
        float result = data.applySmoothing(200.0f); // Second value
        // EMA: smoothed = 0.2 * 200 + 0.8 * 100 = 40 + 80 = 120
        assertEquals(120.0f, result, DELTA);
    }

    @Test
    public void testSmoothingMultipleValues() {
        SensorData data = new SensorData();
        data.applySmoothing(100.0f); // 1st: 100
        data.applySmoothing(200.0f); // 2nd: 120
        float result = data.applySmoothing(300.0f); // 3rd: 0.2*300 + 0.8*120 = 60 + 96 = 156
        assertEquals(156.0f, result, DELTA);
    }

    @Test
    public void testSmoothingConvergence() {
        SensorData data = new SensorData();
        // Feed constant value, smoothed should converge to that value
        for (int i = 0; i < 100; i++) {
            data.applySmoothing(500.0f);
        }
        assertEquals(500.0f, data.getSmoothedValue(), 0.01f);
    }

    @Test
    public void testSmoothingReset() {
        SensorData data = new SensorData();
        data.applySmoothing(100.0f);
        data.applySmoothing(200.0f);
        data.resetSmoothing();
        assertTrue(Float.isNaN(data.getSmoothedValue()));
        // After reset, first value should initialize again
        float result = data.applySmoothing(300.0f);
        assertEquals(300.0f, result, DELTA);
    }

    @Test
    public void testTimingFirstEvent() {
        SensorData data = new SensorData();
        float avg = data.recordEventTimestamp(1_000_000_000L); // 1 second in nanos
        // First event: no interval to calculate
        assertEquals(-1f, avg, DELTA);
        assertEquals(0, data.getIntervalCount());
    }

    @Test
    public void testTimingSecondEvent() {
        SensorData data = new SensorData();
        data.recordEventTimestamp(1_000_000_000L);      // t=1s
        float avg = data.recordEventTimestamp(1_200_000_000L); // t=1.2s, interval=200ms
        assertEquals(200.0f, avg, DELTA);
        assertEquals(1, data.getIntervalCount());
    }

    @Test
    public void testTimingMultipleEvents() {
        SensorData data = new SensorData();
        data.recordEventTimestamp(1_000_000_000L);      // t=1s
        data.recordEventTimestamp(1_200_000_000L);      // t=1.2s, interval=200ms
        float avg = data.recordEventTimestamp(1_450_000_000L); // t=1.45s, interval=250ms
        // Average of 200 and 250 = 225
        assertEquals(225.0f, avg, DELTA);
        assertEquals(2, data.getIntervalCount());
    }

    @Test
    public void testEventRateCalculation() {
        SensorData data = new SensorData();
        data.recordEventTimestamp(1_000_000_000L);
        data.recordEventTimestamp(1_200_000_000L); // 200ms interval -> 5 Hz
        float rate = data.getEventRateHz();
        assertEquals(5.0f, rate, DELTA);
    }

    @Test
    public void testEventRateZeroInterval() {
        SensorData data = new SensorData();
        // No events recorded
        assertEquals(-1f, data.getEventRateHz(), DELTA);
    }

    @Test
    public void testTimingReset() {
        SensorData data = new SensorData();
        data.recordEventTimestamp(1_000_000_000L);
        data.recordEventTimestamp(1_200_000_000L);
        data.resetTiming();
        assertEquals(-1f, data.getAverageIntervalMs(), DELTA);
        assertEquals(0, data.getIntervalCount());
        // After reset, should work normally again
        data.recordEventTimestamp(2_000_000_000L);
        float avg = data.recordEventTimestamp(2_100_000_000L);
        assertEquals(100.0f, avg, DELTA);
    }

    @Test
    public void testResetAll() {
        SensorData data = new SensorData();
        data.applySmoothing(100.0f);
        data.recordEventTimestamp(1_000_000_000L);
        data.recordEventTimestamp(1_200_000_000L);
        data.resetAll();
        assertTrue(Float.isNaN(data.getSmoothedValue()));
        assertEquals(-1f, data.getAverageIntervalMs(), DELTA);
        assertEquals(0, data.getIntervalCount());
    }

    @Test
    public void testAlphaConstant() {
        // Verify the alpha constant is 0.2
        assertEquals(0.2f, SensorData.ALPHA, DELTA);
    }
}