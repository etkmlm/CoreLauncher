package com.cdev.corelauncher.utils.events;

public class ProgressEvent extends BaseEvent {
    public String key;
    public double progress;

    public ProgressEvent(String key, double progress) {
        this.key = key;
        this.progress = progress;
    }
}
