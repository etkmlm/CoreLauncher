package com.laeben.corelauncher.minecraft;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.entities.ExecutionInfo;
import com.laeben.corelauncher.minecraft.entities.VersionNotFoundException;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.CurseWrapper;
import com.laeben.corelauncher.minecraft.utils.CommandConcat;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.entities.LogType;
import com.laeben.core.entity.Path;
import com.laeben.core.util.events.KeyEvent;

import java.io.File;

public class Launcher {
    public static Launcher instance;
    private Path gameDir;
    private final EventHandler<BaseEvent> handler;


    public Launcher(){
        this.gameDir = Configurator.getConfig().getGamePath();
        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("launcher", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;

            gameDir = (Path) a.getNewValue();
        }, false);

        instance = this;
    }

    public static Launcher getLauncher(){
        return instance;
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }

    private void handleState(String key){
        handler.execute(new KeyEvent(key));
    }

    /**
     * Prepares the profile to launch.
     * @param profile target profile
     */
    public void prepare(Profile profile){
        handleState("prepare" + profile.getName());

        profile.getWrapper().install(profile.getWrapper().getVersion(profile.getVersionId(), profile.getWrapperVersion()));

        if (profile.getWrapper().getType() != CurseWrapper.Type.ANY){
            CurseForge.getForge().installModpacks(profile, profile.getModpacks());
            CurseForge.getForge().installMods(profile, profile.getMods());
        }

        CurseForge.getForge().installWorlds(profile, profile.getOnlineWorlds());
        CurseForge.getForge().installResourcepacks(profile, profile.getResources());
    }

    /**
     * Launch the game.
     * @param info info for the execution
     */
    public void launch(ExecutionInfo info)
    {
        if (info.version == null || info.version.id == null)
            return;

        if (info.dir == null)
            info.dir = gameDir;

        if (info.args == null)
            info.args = new String[0];

        try {
            var linfo = info.new LaunchInfo();

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
                            info.java = null;

                            handleState("java" + linfo.java.majorVersion);
                            Logger.getLogger().log(LogType.INFO, "Downloading Java " + linfo.java.majorVersion);
                            try{
                                JavaMan.getManager().download(linfo.java);
                                handler.execute(new KeyEvent("jvdown"));
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

            if (!info.java.getExecutable().exists()){
                JavaMan.getManager().reload();
                info.java = null;
                launch(info);
                return;
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

            // If the version lower than 1.7.2 (very legacy) then copy all textures to resources folder in profile folder
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

            handleState("sessionStart");
            // Start a new session
            var session = new Session(info.dir, finalCmds);
            session.start();

            handleState("sessionEnd" + session.getExitCode());
        }
        catch (VersionNotFoundException e){
            handleState(".error.noVersion");
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

}
