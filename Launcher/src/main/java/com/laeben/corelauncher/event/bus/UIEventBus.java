package com.laeben.corelauncher.event.bus;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.bus.EventBus;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.register.EventRegister;
import com.laeben.core.event.type.BaseEvent;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.ui.UI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UIEventBus<T extends EventContext, H extends BaseEvent<T, H>> extends EventBus<T, H> {
    private static boolean overrideExecution = false;

    private CancellableToken<?> cancellationToken;
    private final ExecutorService executor;

    public UIEventBus() {
        executor = Executors.newFixedThreadPool(5);
    }

    public void stop(){
        cancellationToken.stop();
        cancellationToken = new CancellableToken<>();
    }

    @Override
    protected void onExceptionThrown(Object clazz, EventRegister<T, H> register, H event, Throwable e) {
        Logger.getLogger().logHyph("Exception thrown on a class (%s) for event '%s' [isAsync: %b]".formatted(clazz.getClass().getName() + "@" + clazz.hashCode(), event.getContext().toString(), register.isAsync()));
        Logger.getLogger().log(e);
    }

    @Override
    public void execute(H event) {
        if (overrideExecution) return;

        executor.submit(() -> {
            for(final var register : getRegisters().entrySet()){
                if (cancellationToken.shouldStop()) {
                    onExceptionThrown(register.getKey(), register.getValue(), event, new StopException());
                    return;
                }

                if (register.getValue().isAsync()) UI.runAsync(() -> {
                    try {
                        register.getValue().getHandler().handle(event);
                    } catch (Throwable e) {
                        onExceptionThrown(register.getKey(), register.getValue(), event, e);
                    }
                });
                else{
                    try {
                        register.getValue().getHandler().handle(event);
                    } catch (Throwable e) {
                        onExceptionThrown(register.getKey(), register.getValue(), event, e);
                    }
                }
            }
        });
    }

    public static void disable(){
        overrideExecution = true;
    }

    public static void enable(){
        overrideExecution = false;
    }
}
