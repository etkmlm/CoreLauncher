package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.minecraft.entities.LauncherEvent;
import javafx.event.Event;
import javafx.event.EventType;

public class ChangeEvent extends Event {

    private final String key;
    private final Object oldValue;
    private final Object newValue;
    private final Object extra;

    public ChangeEvent(String key, Object oldValue, Object newValue, Object extra) {
        super(EventType.ROOT);

        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.extra = extra;
    }

    public String getKey(){
        return key;
    }

    public Object getOldValue(){
        return oldValue;
    }
    public Object getNewValue(){
        return newValue;
    }
    public Object getExtra(){
        return extra;
    }
}
