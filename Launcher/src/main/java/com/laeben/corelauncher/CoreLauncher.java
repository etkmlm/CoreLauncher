package com.laeben.corelauncher;

import com.laeben.core.LaebenApp;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.discord.Discord;
import com.laeben.corelauncher.discord.entity.Activity;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.entity.ExecutionInfo;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.util.Authenticator;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.util.APIListener;
import com.laeben.corelauncher.util.JavaManager;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import com.laeben.corelauncher.api.ui.UI;
import javafx.scene.control.Alert;
import javafx.util.Duration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

public class CoreLauncher {
    public static final String KEY = "clauncher";

    public static final OS SYSTEM_OS = OS.getSystemOS();
    public static final Path LAUNCHER_PATH = Path.begin(java.nio.file.Path.of(System.getProperty("user.dir")));
    public static final Path LAUNCHER_EXECUTE_PATH;

    public static boolean OS_64;
    public static boolean GUI_INIT = false;

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
            e.printStackTrace(System.out);
            p = null;
        }

        LAUNCHER_EXECUTE_PATH = p;
        OSUtil.setSystemOS(SYSTEM_OS);
    }

    public static Path getLogDir(){
        return Configurator.getConfig().getLauncherPath().to("logs");
    }

    public static void main(String[] args){
        var listArgs = Arrays.stream(args).toList();
        new Logger();

        if (listArgs.contains("--old")){
            try{
                Thread.sleep(2000);
                var j = OSUtil.getJavaFile(OSUtil.getRunningJavaDir().toString(), true);
                String oldJar = listArgs.get(listArgs.indexOf("--old") + 1);
                var old = LAUNCHER_PATH.to("clold.jar");
                LAUNCHER_PATH.to(oldJar).move(old);

                new ProcessBuilder()
                        .command(j.toString(), "-jar", old.toString(), "--new", oldJar)
                        .start();

                System.exit(0);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }
        else if (listArgs.contains("--new")){
            try{
                Thread.sleep(2000);
                var j = OSUtil.getJavaFile(OSUtil.getRunningJavaDir().toString(), true);
                String newJar = listArgs.get(listArgs.indexOf("--new") + 1);
                var n = LAUNCHER_PATH.to(newJar);
                LAUNCHER_PATH.to("clnew.jar").move(n);
                new ProcessBuilder()
                        .command(j.toString(), "-jar", n.toString(), "--delOld")
                        .start();

                System.exit(0);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }
        else if (listArgs.contains("--delOld")){
            try{
                Thread.sleep(3000);
                LAUNCHER_PATH.to("clold.jar").delete();
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        NetUtil.patchSSL();

        if (listArgs.contains("--offline"))
            NetUtil.setOffline(true);

        var cfg = new Configurator(LAUNCHER_PATH);

        if (!cfg.reloadConfig()){
            Logger.getLogger().log(LogType.ERROR,"FATAL: Couldn't load configuration file, launcher will close...");
            UI.shutdown();
            return;
        }

        Translator.generateTranslator();

        // Needs to be initialized after initialization of configurator

        Logger.getLogger().setLogDir(getLogDir());
        new Profiler().reload();
        new Vanilla().asInstance().getAllVersions();
        new OptiFine().asInstance();
        new Launcher();
        new JavaManager().reload();
        new CurseForge().reload();
        new Modrinth().reload();
        new Modder();
        new Authenticator();
        new Discord().startDiscordThread();
        new ExtensionWrapper().reload();
        OS_64 = OSUtil.is64BitOS(JavaManager.getDefault());


        // Launcher Web API Listener
        APIListener.start();

        /*try{
            new CLCommunicator(9845).start();
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }*/

        Discord.getDiscord().setActivity(Activity.setForIdling());

        /*CLCommunicator.getCommunicator().getHandler().addHandler("clauncher", a -> {
            if (!a.getKey().equals(CLCommunicator.EVENT_RECEIVE))
                return;
            var packet = (CLPacket)a.getValue();
            switch (packet.getType()){
                case LAUNCH, HANDSHAKE -> {

                }
                case STATUS -> {
                    var p = new CLStatusPacket(packet);
                    Discord.getDiscord().setActivity(x -> {
                        x.state = p.getType().name();
                        x.details = p.getData();
                    });
                }
            }
        }, true);*/
        Configurator.getConfigurator().getHandler().addHandler("logger", (a) -> {
            if (!a.getKey().equals(Configurator.GAME_PATH_CHANGE))
                return;

            Logger.getLogger().setLogDir(getLogDir());
        }, false);
        LaebenApp.getHandler().addHandler(KEY, a -> {
            if (a instanceof ValueEvent oe){
                if (oe.getKey().equals(LaebenApp.EXCEPTION)){
                    if (oe.getValue() instanceof BindException) // it is not necessary
                        return;
                    Logger.getLogger().log((Exception) oe.getValue());
                }
                else if (oe.getKey().equals(LaebenApp.NET_EXCEPTION)){
                    String[] spl = oe.getValue().toString().split("\\$\\$\\$");
                    Logger.getLogger().logDebug(LogType.ERROR, "Error on request to " + spl[0] + ": " + spl[1]);
                }
            }
        }, false);
        //

        boolean profileLaunch;

        int indexProfile = listArgs.indexOf("--profile");
        boolean confProfile = indexProfile != -1 && listArgs.size() > indexProfile + 1;
        int indexLaunch = listArgs.indexOf("--launch");
        boolean confLaunch = indexLaunch != -1 && listArgs.size() > indexLaunch + 1;
        String profileName= null;

        if (confLaunch){
            profileName = listArgs.get(indexLaunch + 1);
            if (!Configurator.getConfig().useNonGUIShortcut()){
                var profile = Profiler.getProfiler().getProfile(profileName);
                if (!profile.isEmpty())
                    CoreLauncherFX.fromArgs = profile;
                confProfile = false;
            }
            else
                confProfile = true;
        }

        if (confProfile){
            if (profileName == null)
                profileName = listArgs.get(indexProfile + 1);
            var profile = Profiler.getProfiler().getProfile(profileName);

            try{
                Launcher.getLauncher().prepare(profile);
                Launcher.getLauncher().launch(ExecutionInfo.fromProfile(profile));
            }
            catch (Throwable e){
                Logger.getLogger().log(LogType.INFO, "No version...");
            }

            System.exit(0);
        }

        if (Debug.DEBUG)
            Debug.run();
        else{
            /*System.setProperty("prism.lcdtext", "false");
            System.setProperty("prism.text", "t2k");*/
            System.setProperty("prism.allowhidpi", "true");
            System.setProperty("glass.win.uiScale", Configurator.getConfig().getUIScale() + "%");
            System.setProperty("glass.gtk.uiScale", Configurator.getConfig().getUIScale() + "%");
            CoreLauncherFX.launchFX();
        }

        Configurator.getConfig().getTemporaryFolder().getFiles().forEach(Path::delete);
        if (Configurator.getConfig().delGameLogs()){
            Configurator.getConfig().getLauncherPath().to("gamelog").getFiles().forEach(Path::delete);
        }

        System.exit(0);
    }

    public static void updateCheck(){
        var latest = LauncherConfig.APPLICATION.getLatest();
        if (latest != null && LauncherConfig.VERSION < latest.version() && Configurator.getConfig().isEnabledAutoUpdate()){
            var result = CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("update.title"), Translator.translateFormat("update.newVersion", latest.version()))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();

            if (result.isPresent() && result.get().result() == CMsgBox.ResultType.YES){
                var n = CoreLauncher.LAUNCHER_PATH.to("clnew.jar");
                new Thread(() -> {
                    try{
                        NetUtil.download(latest.url(), n, false, true);
                    }
                    catch (NoConnectionException | StopException | HttpException | FileNotFoundException e){
                        return;
                    }

                    try{
                        var name = CoreLauncher.LAUNCHER_EXECUTE_PATH;
                        if (name == null)
                            return;
                        new ProcessBuilder()
                                .command(JavaManager.getDefault().getWindowExecutable().toString(), "-jar", n.toString(), "--old", name.getName())
                                .start();
                        System.exit(0);
                    }
                    catch (Exception e){
                        Logger.getLogger().log(e);
                    }
                }).start();
            }
        }
    }

    public static void announcementCheck(){
        var now = Date.from(Instant.now());
        var showed = Configurator.getConfig().getShowedAnnounces();
        var locale = Configurator.getConfig().getLanguage();

        if (!LaebenApp.isOffline()){
            showed.removeIf(a -> LauncherConfig.APPLICATION.getAnnouncements().stream().noneMatch(x -> x.getId() == a));
        }

        for (var ann : LauncherConfig.APPLICATION.getAnnouncements()){
            boolean versCheck = !ann.containingVersion(String.valueOf(LauncherConfig.VERSION));
            boolean dateCheck = ann.getDate().after(now);
            if (versCheck || dateCheck){
                if (showed.remove((Object)ann.getId()))
                    Logger.getLogger().logDebug(String.format("Announcement %d removing: version check (%b) and date check (%b)", ann.getId(), versCheck, dateCheck));
                continue;
            }

            if (showed.contains(ann.getId()))
                continue;

            String title = ann.getTitle(locale);
            String content = ann.getContent(locale);

            if (Main.getMain() == null)
                continue;

            Configurator.getConfig().getShowedAnnounces().add(ann.getId());
            Main.getMain().announceLater(title, content, Announcement.AnnouncementType.BROADCAST, Duration.millis(ann.getDuration()));
        }
        Configurator.save();
    }

    public static void restart(){
        var java = JavaManager.getDefault().getExecutable();

        try {
            var jar = CoreLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            var jarFile = Paths.get(jar).toFile();

            var process = new ProcessBuilder()
                    .command(java.toString(), "-jar", jarFile.getAbsolutePath())
                    .directory(jarFile.getParentFile())
                    .start();
        } catch (IOException | URISyntaxException e) {
            Logger.getLogger().log(e);
        }
        UI.shutdown();
    }
}
