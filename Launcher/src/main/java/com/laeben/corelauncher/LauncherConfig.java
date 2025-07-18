package com.laeben.corelauncher;

import com.laeben.core.LaebenApp;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.minecraft.entity.Library;

public final class LauncherConfig {
    public static final double VERSION = 1.19;
    private static final String APP_ID = "clauncher";
    public static final boolean USE_DETAILED_LOGGING = false;
    public static final LaebenApp APPLICATION;

    static {
        LaebenApp app;
        try{
            app = LaebenApp.get(APP_ID, "Core Launcher");
        }
        catch (Throwable ex){
            if (!(ex instanceof NoConnectionException))
                ex.printStackTrace();
            app = LaebenApp.offline(APP_ID, "Core Launcher");
        }
        APPLICATION = app;
    }

    public static final Library[] LAUNCHER_LIBRARIES = { new Library()
    {
        {
            name = "com.laeben:clpatcher:1.1";
            isAgent = true;
        }
    }};
}
