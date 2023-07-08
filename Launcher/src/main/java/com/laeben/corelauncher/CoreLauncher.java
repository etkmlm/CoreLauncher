package com.laeben.corelauncher;

import com.laeben.core.LaebenApp;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.events.ObjectEvent;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.entities.ExecutionInfo;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.utils.Authenticator;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.corelauncher.utils.OSUtils;
import com.laeben.corelauncher.utils.entities.LogType;
import com.laeben.corelauncher.utils.entities.OS;
import com.laeben.core.entity.Path;

import java.util.Arrays;

public class CoreLauncher {

    public static final OS SYSTEM_OS = OS.getSystemOS();
    public static final Path LAUNCHER_PATH = Path.begin(java.nio.file.Path.of(System.getProperty("user.dir")));
    public static final Path LAUNCHER_EX_PATH;
    static {
        Path p;
        try{
            String s = CoreLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            if (SYSTEM_OS == OS.WINDOWS)
                s = s.substring(1);
            p = Path.begin(java.nio.file.Path.of(s));
            if (p.isDirectory())
                p = null;
        }
        catch (Exception e){
            e.printStackTrace();
            p = null;
        }

        LAUNCHER_EX_PATH = p;
    }
    public static boolean OS_64;
    public static boolean GUI_INIT = false;
    public static void main(String[] args){
        var listArgs = Arrays.stream(args).toList();

        if (listArgs.contains("--old")){
            try{
                Thread.sleep(2000);
                var j = OSUtils.getJavaFile(OSUtils.getRunningJavaDir().toString());
                String oldJar = listArgs.get(listArgs.indexOf("--old") + 1);
                var old = LAUNCHER_PATH.to("clold.jar");
                LAUNCHER_PATH.to(oldJar).move(old);

                new ProcessBuilder()
                        .command(j.toString(), "-jar", old.toString(), "--new", oldJar)
                        .start();

                System.exit(0);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (listArgs.contains("--new")){
            try{
                Thread.sleep(2000);
                var j = OSUtils.getJavaFile(OSUtils.getRunningJavaDir().toString());
                String newJar = listArgs.get(listArgs.indexOf("--new") + 1);
                var n = LAUNCHER_PATH.to(newJar);
                LAUNCHER_PATH.to("clnew.jar").move(n);
                new ProcessBuilder()
                        .command(j.toString(), "-jar", n.toString(), "--delOld")
                        .start();

                System.exit(0);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (listArgs.contains("--delOld")){
            try{
                Thread.sleep(3000);
                LAUNCHER_PATH.to("clold.jar").delete();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        System.setProperty("java.net.preferIPv4Stack", "true");

        if (listArgs.contains("--offline"))
            NetUtils.setOffline(true);

        new Configurator(LAUNCHER_PATH).reloadConfig();

        Translator.generateTranslator();

        // Needs to be initialized after initialization of configurator
        new Logger();
        new Profiler().reload();
        new Vanilla().asInstance().getAllVersions();
        new OptiFine().asInstance();
        new Launcher();
        new JavaMan().reload();
        new CurseForge().reload();
        new Modrinth();
        new Authenticator();
        OS_64 = OSUtils.is64BitOS();

        LaebenApp.getHandler().addHandler("clauncher", a -> {
            if (a instanceof ObjectEvent oe){
                if (oe.getKey().equals("exception"))
                    Logger.getLogger().log((Exception) oe.getValue());
                else if (oe.getKey().equals("netException")){
                    String[] spl = oe.getValue().toString().split("\\$\\$\\$");
                    Logger.getLogger().printLog(LogType.ERROR, "Error on request to " + spl[0] + ": " + spl[1]);
                }
            }
        });
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

        CoreLauncherFX.launchFX();

        Configurator.getConfig().getTemporaryFolder().getFiles().forEach(Path::delete);
        if (Configurator.getConfig().delGameLogs())
            Configurator.getConfig().getLauncherPath().to("gamelog").getFiles().forEach(Path::delete);

        System.exit(0);
    }
}
