package com.laeben.corelauncher.api.socket.event;

import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.context.ValueContext;

public class CommunicatorContext extends ValueContext {
    public CommunicatorContext(String label, EventContext subContext) {
        super(label, subContext);
    }
}
