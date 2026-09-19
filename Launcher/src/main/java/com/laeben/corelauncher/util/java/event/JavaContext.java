package com.laeben.corelauncher.util.java.event;

import com.laeben.core.event.context.EventContext;

public class JavaContext extends EventContext {
    public static final JavaContext ADD = new JavaContext("Add");
    public static final JavaContext DELETE = new JavaContext("Delete");
    public static final JavaContext UPDATE = new JavaContext("Update");
    public static final JavaContext DOWNLOAD_COMPLETE = new JavaContext("Download Complete");
    public JavaContext(String label) {
        super(label);
    }
}
