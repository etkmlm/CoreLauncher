package com.laeben.corelauncher.ui.controller;

import com.laeben.core.event.bus.EventBus;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.register.EventRegister;
import com.laeben.core.util.EventHandler;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.corelauncher.api.ui.Controller;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class HandlerController extends Controller {
    private final String keyFamily;
    private final String key;
    private final Set<EventHandler<?>> handlers;
    private final Set<EventBus<?, ?>> buses;
    public HandlerController(String key){
        this.keyFamily = key;
        this.key = this.keyFamily + hashCode();
        this.handlers = new HashSet<>();
        this.buses = new HashSet<>();
    }

    protected <T extends EventContext, H extends com.laeben.core.event.type.BaseEvent<T, H>> void registerHandler(EventBus<T, H> bus, EventRegister.Handler<T, H> con, boolean async){
        bus.addHandler(this, con, async);
        buses.add(bus);
    }

    protected <T extends BaseEvent> void registerHandler(EventHandler<T> handler, Consumer<T> m, boolean async){
        handler.addHandler(key, m, async);
        handlers.add(handler);
    }

    public String getKey() {
        return key;
    }

    public String getKeyFamily() {
        return keyFamily;
    }

    @Override
    public void dispose(){
        super.dispose();
        buses.forEach(a -> a.removeHandler(this));
        buses.clear();
        handlers.forEach(a -> a.removeHandler(key));
        handlers.clear();
    }
}
