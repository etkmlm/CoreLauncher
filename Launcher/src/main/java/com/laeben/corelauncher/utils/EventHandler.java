package com.laeben.corelauncher.utils;

import com.laeben.core.entity.Register;
import com.laeben.core.util.events.BaseEvent;
import javafx.application.Platform;

import java.security.PublicKey;
import java.util.function.Consumer;

public class EventHandler<T extends BaseEvent> extends com.laeben.core.util.EventHandler<T> {
    @Override
    public void executeReg(Register<T> reg, T event){
        if (reg.isAsync())
            Platform.runLater(() -> reg.getEx().accept(event));
        else
            reg.getEx().accept(event);
    }
}
