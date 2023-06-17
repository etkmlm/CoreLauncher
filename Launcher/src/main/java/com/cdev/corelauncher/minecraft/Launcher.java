package com.cdev.corelauncher.minecraft;

import com.cdev.corelauncher.LauncherConfig;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.entities.*;
import com.cdev.corelauncher.minecraft.utils.CommandConcat;
import com.cdev.corelauncher.utils.EventHandler;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.Path;
import com.cdev.corelauncher.utils.events.KeyEvent;
import com.cdev.corelauncher.utils.events.ProgressEvent;
import javafx.event.Event;

import java.io.File;

public class Launcher {
    public static Launcher instance;
    private Path gameDir;
    private final EventHandler<Event> handler;
    private boolean isGameRunning;


    public Launcher(){
        this.gameDir = Configurator.getConfig().getGamePath();
        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("launcher", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;

            gameDir = (Path) a.getNewValue();
        });

        instance = this;
    }

    public static Launcher getLauncher(){
        return instance;
    }

    public EventHandler<Event> getHandler(){
        return handler;
    }

    private void handleState(String key){
        handler.execute(new KeyEvent(key));
    }

    public boolean isGameRunning(){
        return isGameRunning;
    }

    public void prepare(Profile profile){
        handleState("prepare" + profile.getName());
        profile.getWrapper().install(profile.getWrapper().getVersion(profile.getVersionId(), profile.getWrapperVersion()));
    }

    public void launch(ExecutionInfo info)
    {
        if (info.version == null || info.version.id == null)
            return;
        if (info.dir == null)
            info.dir = gameDir;
        if (info.args == null)
            info.args = new String[0];

        var linfo = info.new LaunchInfo();

        try {

            if (info.account == null)
                info.account = Configurator.getConfig().getUser();

            if (info.java == null){
                info.java = Configurator.getConfig().getDefaultJava();

                if (info.java == null || info.java.majorVersion != linfo.java.majorVersion){
                    info.java = JavaMan.getManager().tryGet(linfo.java);
                    if (info.java == null){
                        info.java = JavaMan.getDefault();
                        if (info.java.majorVersion != linfo.java.majorVersion){
                            handleState("java" + linfo.java.majorVersion);
                            JavaMan.getManager().download(linfo.java, (b) -> handler.execute(new ProgressEvent("javaDownload", b)));
                            launch(info);
                            //handler.execute(new KeyEvent("needJava" + linfo.java.majorVersion).setSource(info));
                            return;
                        }
                    }
                }

            }

            handleState("gameStart");
            Logger.getLogger().log(LogType.INFO, "-------LAUNCH START: " + info.version.id + "-------");

            ///

            String libPath = "-Djava.library.path=" + linfo.nativePath;
            String c = String.valueOf(File.pathSeparatorChar);
            String cp = linfo.clientPath + c + String.join(c, linfo.libraries);

            String[] rootCmds = {
                    info.java.getExecutable().toString(),
                    libPath,
                    "-javaagent:" + linfo.agentPath,
                    "-Dorg.lwjgl.util.Debug=true",
                    "-Dfml.ignoreInvalidMinecraftCertificates=true"
            };
            if (linfo.assets.isVeryLegacy()){
                var resources = info.dir.to("resources");
                gameDir.to("assets", "virtual", "verylegacy").copy(resources);
            }

            String[] gameCmds = linfo.getGameArguments();

            isGameRunning = true;

            var finalCmds = new CommandConcat()
                    .add(rootCmds)
                    .add(linfo.getJvmArguments())
                    .add(info.args)
                    .add("-cp", cp)
                    .add(linfo.mainClass)
                    .add(gameCmds)
                    .generate();
            var process = new ProcessBuilder()
                    .directory(info.dir.toFile())
                    .inheritIO()
                    .command(finalCmds)
                    .start();
            process.waitFor();

            ///

            /*String libPath = "-Djava.library.path=" + linfo.nativePath;
            String c = String.valueOf(File.pathSeparatorChar);
            String cp = linfo.clientPath + c + String.join(c, linfo.libraries);

            String[] rootCmds = {
                    info.java.getExecutable().toString(),
                    "-cp", cp,
                    libPath,
                    "-Dorg.lwjgl.util.Debug=true"
            };

            String[] gameCmds;

            if (linfo.assets.isVeryLegacy()){
                gameCmds = new String[] {
                        linfo.mainClass,
                        info.account.getUsername(),
                        "verylegacy",
                        //"--assetsDir", gameDir.to("assets", "virtual", "verylegacy").toString(),
                        "--gameDir", info.dir.toString(),
                };
                var resources = info.dir.to("resources");
                gameDir.to("assets", "virtual", "verylegacy").copy(resources);
            }
            else if (linfo.assets.isLegacy())
                gameCmds = new String[] {
                        linfo.mainClass,
                        "--assetIndex", linfo.assets.id,
                        "--assetsDir", gameDir.to("assets", "virtual", "legacy").toString(),
                        "--version", info.version.id,
                        "--gameDir", info.dir.toString(),
                        "--username", info.account.getUsername()
                };
            else
                gameCmds = new String[] {
                        linfo.mainClass,
                        "--assetIndex", linfo.assets.id,
                        "--assetsDir", gameDir.to("assets").toString(),
                        "--version", info.version.id,
                        "--gameDir", info.dir.toString(),
                        "--accessToken", "T",
                        "--username", info.account.getUsername()
                };



            isGameRunning = true;

            var process = new ProcessBuilder()
                    .directory(info.dir.toFile())
                    .inheritIO()
                    .command(new CommandConcat().add(rootCmds).add(info.args).add(gameCmds).generate())
                    .start();
            process.waitFor();*/

            isGameRunning = false;

            Logger.getLogger().log(LogType.INFO, "-------LAUNCH END-------");
            handleState("gameEnd");
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

}
