package com.cdev.corelauncher;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.entities.ExecutionInfo;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.cdev.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.cdev.corelauncher.minecraft.wrappers.quilt.Quilt;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.OS;
import com.cdev.corelauncher.utils.entities.Path;

import java.util.Arrays;

public class CoreLauncher {

    public static final OS SYSTEM_OS = OS.getSystemOS();
    public static boolean OS_64;
    public static boolean GUI_INIT = false;
    public static void main(String[] args){
        var listArgs = Arrays.stream(args).toList();

        if (listArgs.contains("--offline"))
            NetUtils.setOffline(true);

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

        int index = listArgs.indexOf("--profile");
        if (index != -1 && listArgs.size() > index + 1){
            String profileName = listArgs.get(index + 1);
            var profile = Profiler.getProfiler().getProfile(profileName);

            try{
                Launcher.getLauncher().prepare(profile);
                Launcher.getLauncher().launch(ExecutionInfo.fromProfile(profile));
            }
            catch (Exception e){
                Logger.getLogger().log(LogType.INFO, "No version...");
            }

            System.exit(0);
        }

        //Authenticator t = new Authenticator();
        //var acc = t.authenticate(Account.fromUsername("EvilM3nster"));

        GUI_INIT = true;
        CoreLauncherFX.launchFX();

        Configurator.getConfig().getTemporaryFolder().getFiles().forEach(Path::delete);
        Configurator.getConfig().getLauncherPath().to("gamelog").getFiles().forEach(Path::delete);

        System.exit(0);
    }
}
