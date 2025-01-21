package com.laeben.corelauncher.ui.controller;

import com.laeben.core.util.EventHandler;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.corelauncher.api.ui.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class HandlerController extends Controller {
    private final String key;
    private final List<EventHandler<?>> handlers;
    public HandlerController(String key){
        this.key = key;
        this.handlers = new ArrayList<>();
    }

    protected <T extends BaseEvent> void registerHandler(EventHandler<T> handler, Consumer<T> m, boolean async){
        handler.addHandler(key, m, async);
        if (!handlers.contains(handler))
            handlers.add(handler);
    }

    protected void markRootNode(){

    }

    @Override
    public void dispose(){
        super.dispose();
        handlers.forEach(a -> a.removeHandler(key));
        handlers.clear();
    }
}
