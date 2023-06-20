package com.cdev.corelauncher.minecraft;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.entities.ExecutionInfo;
import com.cdev.corelauncher.minecraft.utils.CommandConcat;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.EventHandler;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.NoConnectionException;
import com.cdev.corelauncher.utils.entities.Path;
import com.cdev.corelauncher.utils.events.KeyEvent;
import com.cdev.corelauncher.utils.events.ProgressEvent;
import javafx.event.Event;

import java.io.File;

public class Launcher {
    public static Launcher instance;
    private Path gameDir;
    private final EventHandler<Event> handler;


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

            // Checkup for Java
            if (info.java == null){
                // If profile Java is null, try to get default Java from config
                info.java = Configurator.getConfig().getDefaultJava();
                if (info.java == null || info.java.majorVersion != linfo.java.majorVersion){
                    // If this Java is unusable, try to get any Java that fits to version from all
                    info.java = JavaMan.getManager().tryGet(linfo.java);
                    if (info.java == null){
                        // If no result, use launcher's Java
                        info.java = JavaMan.getDefault();
                        if (info.java.majorVersion != linfo.java.majorVersion){
                            // Last solution, download new Java and relaunch (if there is internet here of course...)
                            handleState("java" + linfo.java.majorVersion);
                            try{
                                JavaMan.getManager().download(linfo.java, (b) -> handler.execute(new ProgressEvent("javaDownload", b)));
                            }
                            catch (NoConnectionException e){
                                handleState(".error.launch.java");
                                return;
                            }
                            launch(info);
                            return;
                        }
                    }
                }

            }

            String libPath = "-Djava.library.path=" + linfo.nativePath;
            String c = String.valueOf(File.pathSeparatorChar);
            String cp = linfo.clientPath + c + String.join(c, linfo.libraries);

            // Static commands
            String[] rootCmds = {
                    info.java.getExecutable().toString(),
                    libPath,
                    "-javaagent:" + linfo.agentPath, // Don't worry (yup), this contains launcher's patches to mc like 1.16.4 multiplayer bug
                    "-Dorg.lwjgl.util.Debug=true",
                    "-Dfml.ignoreInvalidMinecraftCertificates=true"
            };

            // If version lower than 1.7.2 (very legacy) then copy all textures to resources folder in profile folder
            if (linfo.assets.isVeryLegacy()){
                var resources = info.dir.to("resources");
                gameDir.to("assets", "virtual", "verylegacy").copy(resources);
            }

            String[] gameCmds = linfo.getGameArguments();

            // Final commands
            var finalCmds = new CommandConcat()
                    .add(rootCmds)
                    .add(linfo.getJvmArguments())
                    .add(info.args)
                    .add("-cp", cp)
                    .add(linfo.mainClass)
                    .add(gameCmds)
                    .generate();

            boolean hide = Configurator.getConfig().hideAfter();
            if (hide)
                FXManager.getManager().hideAll();

            handleState("sessionStart");

            // Start new session
            new Session(info.dir, finalCmds).start();

            if (hide)
                FXManager.getManager().showAll();
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

}
