package com.laeben.corelauncher.minecraft.loader.forge.installer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.function.Consumer;

public class ForgeV3Installer implements ForgeInstaller{
    @Override
    public void install(URLClassLoader loader, File target, Consumer<String> logState) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        var loadInstallProfile = loader.loadClass("net.minecraftforge.installer.json.Util").getMethod("loadInstallProfile");

        var v1 = loadInstallProfile.invoke(null);

        var cls = loader.loadClass("net.minecraftforge.installer.actions.ClientInstall");
        var callbackInterface = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback");
        var monitor = Proxy.newProxyInstance(callbackInterface.getClassLoader(), new Class[]{callbackInterface}, (proxy, method, args) -> {
            if (method.getName().equals("message") && args.length > 0)
                logState.accept("," + args[0].toString() + ":." + "forge.state.install");
            return null;
        });
        //var monitor = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback").getDeclaredField("TO_STD_OUT").get(null);
        var clientInstall = cls.getConstructors()[0].newInstance(v1,monitor );

        var method = cls.getMethod("run", File.class, File.class);
        method.invoke(clientInstall, target, new File(loader.getURLs()[0].getFile()));
    }
}
