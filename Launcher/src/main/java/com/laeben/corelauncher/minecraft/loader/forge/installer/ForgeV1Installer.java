package com.laeben.corelauncher.minecraft.loader.forge.installer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.function.Consumer;

public class ForgeV1Installer implements ForgeInstaller{
    @Override
    public void install(URLClassLoader loader, File target, Consumer<String> logState) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        var cls = loader.loadClass("net.minecraftforge.installer.ClientInstall");
        var clientInstall = cls.getConstructors()[0].newInstance();

        var srv = loader.loadClass("net.minecraftforge.installer.ServerInstall");
        var serverInstall = srv.getConstructors()[0].newInstance();

        var actType = loader.loadClass("net.minecraftforge.installer.ActionType");

        var predicates = loader.loadClass("com.google.common.base.Predicates");
        var predicate = loader.loadClass("com.google.common.base.Predicate");

        var always = predicates.getMethod("alwaysTrue").invoke(null);
        var method = actType.getMethod("run", File.class, predicate);

        method.invoke(clientInstall, target, always);
        method.invoke(serverInstall, target, always);
    }
}
