package com.cdev.corelauncher;

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
    public static Config config;
    public static Translator translator;


    public static void main(String[] args){
        config = new Config(Path.begin(java.nio.file.Path.of(System.getProperty("user.dir"))))
                .setGamePath(new Path(OSUtils.getAppFolder()));
        translator = Translator.generateTranslator();
        var mainDir = config.getGamePath();

        new Logger(mainDir.to("launcher").to("launcherlog"), false);
        new Launcher(mainDir).reload();
        new JavaManager(mainDir.to("launcher").to("java"));

        CoreLauncherFX.launchFX();

        System.exit(0);
    }
}
