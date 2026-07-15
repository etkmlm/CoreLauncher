package com.laeben.corelauncher.ui.entity.monitor;

import javafx.scene.Node;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class MonitorData{
    private final Object key;
    private boolean stopRequested;
    private Consumer<Double> onPercentageChanged;

    protected MonitorData(Object key){
        this.key = key;
    }

    public void stop(){
        stopRequested = true;
    }

    public void setOnPercentageChanged(Consumer<Double> onPercentageChanged){
        this.onPercentageChanged = onPercentageChanged;
    }
    public boolean setPercentage(double percentage) {
        if (onPercentageChanged != null)
            onPercentageChanged.accept(percentage);
        return !stopRequested;
    }

    public abstract Node serializeMetadata();

    public Object getKey(){
        return key;
    }

    @Override
    public boolean equals(Object o){
        return this == o || o == key || o instanceof MonitorData md && md.key.equals(key);
    }

    @Override
    public int hashCode(){
        return Objects.hash(key);
    }
}