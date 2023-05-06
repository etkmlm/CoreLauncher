package com.cdev.corelauncher.minecraft.entities;

import javafx.event.Event;
import javafx.event.EventType;

public class LauncherEvent extends Event {

    public enum LauncherEventType{
        PROGRESS, STATE, NEED
    }

    private final LauncherEventType type;
    private final String key;
    private final Object value;

    public LauncherEvent(LauncherEventType type, String key, Object value) {
        super(EventType.ROOT);

        this.type = type;
        this.key = key;
        this.value = value;
    }

    public LauncherEventType getType(){
        return type;
    }

    public String getKey(){
        return key;
    }

    public Object getValue(){
        return value;
    }
}
