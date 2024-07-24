package com.laeben.corelauncher.minecraft.wrapper.forge.installer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.function.Consumer;

public interface ForgeInstaller {
    ForgeInstaller[] INSTALLERS = { new ForgeV1Installer(), new ForgeV2Installer(), new ForgeV3Installer() };

    void install(URLClassLoader loader, File target, Consumer<String> logState) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException;
}
