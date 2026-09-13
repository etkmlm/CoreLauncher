package com.laeben.corelauncher.event.bus;

import com.laeben.core.event.bus.IndirectEventBus;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.register.EventRegister;
import com.laeben.core.event.type.BaseEvent;
import com.laeben.corelauncher.api.entity.Logger;

public class FrequentEventBus<T extends EventContext, H extends BaseEvent<T, H>> extends IndirectEventBus<T, H> {
    @Override
    protected void onExceptionThrown(Object clazz, EventRegister<T, H> register, H event, Throwable e) {
        Logger.getLogger().logHyph("Exception thrown on a class (%s) for event '%s' [isAsync: %b]".formatted(clazz.getClass().getName() + "@" + clazz.hashCode(), event.getContext().toString(), register.isAsync()));
        Logger.getLogger().log(e);
    }
}
