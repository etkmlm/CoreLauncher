package com.cdev.corelauncher.utils.events;

import javafx.event.Event;
import javafx.event.EventType;

public class ChangeEvent extends KeyEvent {
    private final Object oldValue;
    private final Object newValue;

    public ChangeEvent(String key, Object oldValue, Object newValue) {
        super(key);

        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getOldValue(){
        return oldValue;
    }
    public Object getNewValue(){
        return newValue;
    }
}
