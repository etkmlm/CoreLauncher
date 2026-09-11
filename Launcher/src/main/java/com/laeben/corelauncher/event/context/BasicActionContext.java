package com.laeben.corelauncher.event.context;

import com.laeben.core.event.context.EventContext;

public class BasicActionContext extends EventContext {
    public static BasicActionContext RELOAD = new BasicActionContext("reload");
    public static BasicActionContext STOP = new BasicActionContext("stop");
    public static BasicActionContext START = new BasicActionContext("start");

    public BasicActionContext(String label) {
        super(label);
    }
}
