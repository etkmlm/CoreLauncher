package com.laeben.corelauncher;

import com.laeben.core.LaebenApp;
import com.laeben.corelauncher.minecraft.entities.Library;

public final class LauncherConfig {
    public static final double VERSION = 1.0;
    private static final String APP_ID = "clauncher";
    public static final LaebenApp APPLICATION;

    static {
        LaebenApp app;
        try{
            app = LaebenApp.get(APP_ID, "Core Launcher");
        }
        catch (Exception ex){
            ex.printStackTrace();
            app = LaebenApp.offline(APP_ID, "Core Launcher");
        }
        APPLICATION = app;
    }

    public static final Library[] LAUNCHER_LIBRARIES = { new Library(){
        {
            name = "com.laeben:clfixer:1.0";
        }
    } };
}
