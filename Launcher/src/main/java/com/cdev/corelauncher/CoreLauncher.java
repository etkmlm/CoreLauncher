package com.cdev.corelauncher;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.utils.Authenticator;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.minecraft.wrappers.forge.Forge;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.OS;
import com.cdev.corelauncher.utils.entities.Path;

public class CoreLauncher {

    public static final OS SYSTEM_OS = OS.getSystemOS();
    public static boolean OS_64;

    public static void main(String[] args){
        var configPath = Path.begin(java.nio.file.Path.of(System.getProperty("user.dir")));

        new Configurator(configPath).reloadConfig();

        Translator.generateTranslator();

        // Needs to be initialized after initialization of configurator
        new Logger();
        new Profiler().reload();
        new Vanilla().asInstance().getAllVersions();
        new Launcher();
        new JavaMan().reload();
        OS_64 = OSUtils.is64BitOS();
        //



        //Authenticator t = new Authenticator();
        //var acc = t.authenticate(Account.fromUsername("EvilM3nster"));

        CoreLauncherFX.launchFX();

        System.exit(0);
    }
}
