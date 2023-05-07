package com.cdev.corelauncher;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.utils.JavaManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.OS;
import com.cdev.corelauncher.utils.entities.Path;

public class CoreLauncher {

    public static final OS SYSTEM_OS = OS.getSystemOS();

    public static void main(String[] args){
        var configPath = Path.begin(java.nio.file.Path.of(System.getProperty("user.dir")));

        new Configurator(configPath).reloadConfig();

        Translator.generateTranslator();
        var mainDir = Configurator.getConfig().getGamePath();

        new Logger(mainDir.to("launcher").to("launcherlog"), false);
        new Launcher(mainDir).reload();
        new JavaManager(mainDir.to("launcher").to("java"));

        CoreLauncherFX.launchFX();

        System.exit(0);
    }
}
