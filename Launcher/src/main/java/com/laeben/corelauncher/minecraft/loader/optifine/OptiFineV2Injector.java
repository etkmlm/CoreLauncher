package com.laeben.corelauncher.minecraft.loader.optifine;

import java.io.File;
import java.lang.reflect.Method;

public class OptiFineV2Injector{
    public static void main(String[] args) throws Exception {
        File gameDir = new File(args[0]);

        Class<?> installerClass = Class.forName("optifine.Installer");
        Method m = installerClass.getMethod("doInstall", File.class);

        m.invoke(null, gameDir);
    }
}