package com.cdev.corelauncher.ui.utils;

import com.cdev.corelauncher.minecraft.entities.LauncherEvent;
import javafx.event.Event;

import java.util.HashMap;
import java.util.Map;

public class EventHandler<T extends Event> {
    private final Map<String, javafx.event.EventHandler<T>> handlers;

    public EventHandler(){
        handlers = new HashMap<>();
    }

    public void addHandler(String key, javafx.event.EventHandler<T> handler){
        handlers.put(key, handler);
    }

    public void removeHandler(String key){
        handlers.remove(key);
    }

    public void execute(T e){
        handlers.values().forEach(x -> x.handle(e));
    }
}
