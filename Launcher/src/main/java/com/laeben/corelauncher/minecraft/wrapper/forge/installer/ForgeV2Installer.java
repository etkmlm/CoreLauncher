package com.laeben.corelauncher.minecraft.wrapper.forge.installer;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ForgeV2Installer implements ForgeInstaller{
    @Override
    public void install(URLClassLoader loader, File target, Consumer<String> logState) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        var loadInstallProfile = loader.loadClass("net.minecraftforge.installer.json.Util").getMethod("loadInstallProfile");

        var v1 = loadInstallProfile.invoke(null);

        var cls = loader.loadClass("net.minecraftforge.installer.actions.ClientInstall");
        var callbackInterface = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback");
        var noopProgress = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback$ProgressBar").getDeclaredField("NOOP").get(null);
        var monitor = Proxy.newProxyInstance(callbackInterface.getClassLoader(), new Class[]{callbackInterface}, (a, b, c) -> onProgress(a, b, c, logState));

        //var monitor2 = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback").getDeclaredField("TO_STD_OUT").get(null);
        var clientInstall = cls.getConstructors()[0].newInstance(v1,monitor);

        var method = cls.getMethod("run", File.class, Predicate.class, File.class);
        method.invoke(clientInstall, target, (Predicate<String>) a -> true, new File(loader.getURLs()[0].getFile()));
    }

    private String step;

    private Object onProgress(Object proxy, Method method, Object[] args, Consumer<String> logState) throws Throwable {
        if (method.getName().equals("message") && args.length > 0)
            logState.accept("," + args[0].toString() + ":." + "forge.state.install");
        if (method.getName().equals("getCurrentStep"))
            return step;
        if (method.getName().equals("setCurrentStep"))
            step = args[0].toString();

        if (method.isDefault()){
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        else
            return method.getDefaultValue();
    }
}
