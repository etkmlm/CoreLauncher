package com.cdev.corelauncher.utils;

import javafx.application.Platform;
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
        handlers.keySet().forEach(x -> {
            var value = handlers.get(x);
            try{
                Platform.runLater(() -> value.handle(e));
            }
            catch (Exception f){
                Logger.getLogger().logHyph("ERROR HANDLING " + x);
                Logger.getLogger().log(f);
            }
        });
    }
}
