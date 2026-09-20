package com.laeben.corelauncher.api.socket.event;

import com.laeben.core.event.context.EventContext;

public class CommunicatorContext extends EventContext {
    public static final CommunicatorContext RECEIVE = new CommunicatorContext("Receive", null);
    public CommunicatorContext(String label, EventContext subContext) {
        super(label, subContext);
    }
}
