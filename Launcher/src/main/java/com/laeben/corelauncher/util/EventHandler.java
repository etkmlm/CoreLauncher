package com.laeben.corelauncher.util;

import com.laeben.core.util.NetUtils;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.corelauncher.api.ui.UI;

public class EventHandler<T extends BaseEvent> extends com.laeben.core.util.EventHandler<T> {
    public static final String RELOAD = "reload";
    public static final String STOP = "stop";
    public static final String START = "start";

    private static boolean overrideExecution = false;

    static {
        NetUtils.getHandler().setExecuteReg(EventHandler::er);
    }

    private static <T extends BaseEvent> void er(ExReg<T> reg){
        if (reg.reg().isAsync())
            UI.runAsync(() -> reg.reg().getEx().accept(reg.event()));
        else
            reg.reg().getEx().accept(reg.event());
    }

    @Override
    public void executeReg(ExReg<T> reg){
        er(reg);
    }

    @Override
    public void execute(T t){
        if (!overrideExecution)
            super.execute(t);
    }

    public static void disable(){
        overrideExecution = true;
    }

    public static void enable(){
        overrideExecution = false;
    }
}
