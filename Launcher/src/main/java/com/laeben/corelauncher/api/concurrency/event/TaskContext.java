package com.laeben.corelauncher.api.concurrency.event;

import com.laeben.core.event.context.EventContext;

public class TaskContext extends EventContext {
    public static final TaskContext ADDED = new TaskContext("added task");
    public static final TaskContext REMOVED = new TaskContext("removed task");

    public TaskContext(String label) {
        super(label);
    }
}
