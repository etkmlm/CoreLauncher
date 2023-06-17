package com.cdev.corelauncher.utils.events;

public class KeyEvent extends BaseEvent {
    private final String key;
    public KeyEvent(String key) {
        this.key = key;
    }

    public String getKey(){
        return key;
    }
}
