package com.laeben.corelauncher.utils;

import com.laeben.core.util.events.BaseEvent;
import javafx.application.Platform;

import java.security.PublicKey;
import java.util.function.Consumer;

public class EventHandler<T extends BaseEvent> extends com.laeben.core.util.EventHandler<T> {
    @Override
    public void execute(T e){
        handlers.keySet().forEach(x -> {
            Consumer<T> value = handlers.get(x);
            try{
                Platform.runLater(() -> value.accept(e));
            }
            catch (Exception f){
                f.printStackTrace();
            }
        });
    }

}
