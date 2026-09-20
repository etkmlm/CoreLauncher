package com.laeben.corelauncher.event.bus;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.event.bus.IndirectEventBus;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.register.EventRegister;
import com.laeben.core.event.type.BaseEvent;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.ui.UI;

public class FrequentEventBus<T extends EventContext, H extends BaseEvent<T, H>> extends IndirectEventBus<T, H> {
    private static boolean overrideExecution = false;

    public static final int FLAG_ASYNC_UI = 0b10;

    @Override
    protected boolean onEvent(Object clazz, EventRegister<T, H> register, H event, CancellableToken<?> cancellableToken) {
        if (register.checkFlag(FLAG_ASYNC_UI)) UI.runAsync(() -> handle(clazz, register, event, cancellableToken));
        else return super.onEvent(clazz, register, event, cancellableToken);

        return true;
    }

    @Override
    public void execute(H event){
        if (overrideExecution) return;

        super.execute(event);
    }

    public static void disable(){
        overrideExecution = true;
    }

    public static void enable(){
        overrideExecution = false;
    }

    @Override
    protected void onExceptionThrown(Object clazz, EventRegister<T, H> register, H event, Throwable e) {
        Logger.getLogger().logHyph("Exception thrown on a class (%s) for event '%s' [flags: %d]".formatted(clazz.getClass().getName() + "@" + clazz.hashCode(), event.getContext().toString(), register.flags()));
        Logger.getLogger().log(e);
    }
}
