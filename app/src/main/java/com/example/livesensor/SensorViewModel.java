package com.example.livesensor;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class SensorViewModel extends AndroidViewModel {

    // Observations list (survives rotation)
    private final MutableLiveData<List<Observation>> observations = new MutableLiveData<>(new ArrayList<>());

    // Selected condition (survives rotation)
    private final MutableLiveData<Integer> selectedConditionIndex = new MutableLiveData<>(0);

    // Simulated mode (survives rotation)
    private final MutableLiveData<Boolean> simulatedMode = new MutableLiveData<>(false);

    // Sensor availability (survives rotation)
    private final MutableLiveData<Boolean> sensorAvailable = new MutableLiveData<>(true);

    // Register/unregister counters (survive rotation)
    private final MutableLiveData<Integer> registerCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> unregisterCount = new MutableLiveData<>(0);

    // Listener state (survives rotation)
    private final MutableLiveData<Boolean> listenerRegistered = new MutableLiveData<>(false);

    public SensorViewModel(@NonNull Application application) {
        super(application);
    }

    // Observations
    public MutableLiveData<List<Observation>> getObservations() {
        return observations;
    }

    public void addObservation(Observation obs) {
        List<Observation> current = observations.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(0, obs);
        observations.setValue(current);
    }

    public void setObservations(List<Observation> list) {
        observations.setValue(new ArrayList<>(list));
    }

    // Selected condition
    public MutableLiveData<Integer> getSelectedConditionIndex() {
        return selectedConditionIndex;
    }

    public void setSelectedConditionIndex(int index) {
        selectedConditionIndex.setValue(index);
    }

    // Simulated mode
    public MutableLiveData<Boolean> getSimulatedMode() {
        return simulatedMode;
    }

    public void setSimulatedMode(boolean simulated) {
        simulatedMode.setValue(simulated);
    }

    // Sensor availability
    public MutableLiveData<Boolean> getSensorAvailable() {
        return sensorAvailable;
    }

    public void setSensorAvailable(boolean available) {
        sensorAvailable.setValue(available);
    }

    // Register/unregister counters
    public MutableLiveData<Integer> getRegisterCount() {
        return registerCount;
    }

    public void incrementRegisterCount() {
        registerCount.setValue(registerCount.getValue() + 1);
    }

    public MutableLiveData<Integer> getUnregisterCount() {
        return unregisterCount;
    }

    public void incrementUnregisterCount() {
        unregisterCount.setValue(unregisterCount.getValue() + 1);
    }

    // Listener state
    public MutableLiveData<Boolean> getListenerRegistered() {
        return listenerRegistered;
    }

    public void setListenerRegistered(boolean registered) {
        listenerRegistered.setValue(registered);
    }
}