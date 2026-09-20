package com.laeben.corelauncher.ui.controller;

import com.laeben.core.event.bus.EventBus;
import com.laeben.core.event.bus.IndirectEventBus;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.register.EventRegister;
import com.laeben.core.util.EventHandler;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.event.bus.FrequentEventBus;

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

    /**
     * Registers a new controlled event listener.
     * @param bus target bus
     * @param handler event handler function
     * @param flags task flags
     * @param <T> event context type
     * @param <H> event type
     */
    protected <T extends EventContext, H extends com.laeben.core.event.type.BaseEvent<T, H>> void registerHandler(EventBus<T, H> bus, EventRegister.Handler<T, H> handler, int flags){
        bus.addHandler(this, handler, flags);
        buses.add(bus);
    }

    /**
     * Registers a new controlled event listener for background operations.
     * <br>
     * May throw an exception if it is marked as async and any UI-thread function is used in it.
     * @param bus target bus
     * @param handler event handler function
     * @param async is async or not
     * @param <T> event context type
     * @param <H> event type
     */
    protected <T extends EventContext, H extends com.laeben.core.event.type.BaseEvent<T, H>> void registerHandler(IndirectEventBus<T, H> bus, EventRegister.Handler<T, H> handler, boolean async){
        bus.addHandler(this, handler, async ? IndirectEventBus.FLAG_ASYNC : 0);
        buses.add(bus);
    }

    /**
     * Registers a new controlled event listener for UI operations.
     * @param bus target bus
     * @param handler event handler function
     * @param async is UI async or not
     * @param <T> event context type
     * @param <H> event type
     */
    protected <T extends EventContext, H extends com.laeben.core.event.type.BaseEvent<T, H>> void registerUIHandler(FrequentEventBus<T, H> bus, EventRegister.Handler<T, H> handler, boolean async){
        bus.addHandler(this, handler, async ? FrequentEventBus.FLAG_ASYNC_UI : 0);
        buses.add(bus);
    }

    /**
     * Registers a new controlled event listener.
     * @deprecated use new {@link EventBus} instead.
     * @param handler target handler
     * @param m event handler function
     * @param async is UI async or not
     * @param <T> event type
     */
    @Deprecated
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
