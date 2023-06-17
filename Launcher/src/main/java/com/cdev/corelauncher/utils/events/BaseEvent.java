package com.cdev.corelauncher.utils.events;

import javafx.event.Event;
import javafx.event.EventType;

public class BaseEvent extends Event {
    public BaseEvent() {
        super(EventType.ROOT);
    }

    public BaseEvent setSource(Object source){
        this.source = source;

        return this;
    }
}
