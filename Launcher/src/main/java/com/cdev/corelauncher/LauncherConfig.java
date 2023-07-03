package com.cdev.corelauncher;

import com.cdev.corelauncher.minecraft.entities.Library;

public final class LauncherConfig {
    public static final double VERSION = 1;
    public static final String LAUNCHER_NAME = "CoreLauncher";

    public static final Library[] LAUNCHER_LIBRARIES = { new Library(){
        {
            name = "com.cdev:clfixer:1.0";
        }
    } };
}
