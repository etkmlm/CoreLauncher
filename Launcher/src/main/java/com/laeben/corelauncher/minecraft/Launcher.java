package com.laeben.corelauncher.minecraft;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.entity.ExecutionInfo;
import com.laeben.corelauncher.minecraft.entity.VersionNotFoundException;
import com.laeben.corelauncher.minecraft.mapping.PGMapper;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.util.CommandConcat;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.core.util.events.KeyEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

public class Launcher {
    public static final String SESSION_START = "sessionStart";
    public static final String SESSION_END = "sessionEnd";
    public static final String SESSION_RECEIVE = "sessionReceive";
    public static final String AUTH_FAIL = "authFail";
    public static final String PREPARE = "prepare";
    public static final String JAVA = "java";
    public static final String JAVA_DOWNLOAD_ERROR = "errJava";


    public static Launcher instance;
    private final EventHandler<BaseEvent> handler;

    private Function<ValueEvent, Boolean> onAuthFail;

    public Launcher(){
        handler = new EventHandler<>();

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

    public void setOnAuthFail(Function<ValueEvent, Boolean> onAuthFail){
        this.onAuthFail = onAuthFail;
    }

    /**
     * Prepares the profile to launch.
     * @param profile target profile
     */
    public void prepare(Profile profile) throws NoConnectionException, StopException, PerformException, HttpException, FileNotFoundException, VersionNotFoundException {
        handleState(PREPARE + profile.getName());

        var version = profile.getLoader().getVersion(profile.getVersionId(), profile.getLoaderVersion());
        if (version == null)
            throw new VersionNotFoundException(profile.getLoaderVersion());
        profile.getLoader().install(version);

        if (!profile.getLoader().getType().isNative()){
            Modder.getModder().installModpacks(profile, profile.getModpacks());
            Modder.getModder().installMods(profile, profile.getMods());
        }

        Modder.getModder().installWorlds(profile, profile.getOnlineWorlds());
        Modder.getModder().installResourcepacks(profile, profile.getResourcepacks());
        Modder.getModder().installShaders(profile, profile.getShaders());

        var op1 = Configurator.getConfig().getLauncherPath().to("options.txt");
        var op2 = profile.getPath().to("options.txt");
        if (!op2.exists()){
            if (op1.exists())
                op1.copy(op2);
            else {
                var lang = Configurator.getConfig().getLanguage();
                op2.write("lang:" + lang);
            }
        }
    }

    /**
     * Launch the game.
     * @param info info for the execution
     */
    public void launch(ExecutionInfo info) throws StopException, VersionNotFoundException {
        if (info.version == null || info.version.id == null)
            return;

        if (info.dir == null)
            info.dir = Configurator.getConfig().getGamePath();

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
                    info.java = JavaManager.getManager().tryGet(linfo.java);
                    if (info.java == null){
                        // If no result, use launcher's Java
                        info.java = JavaManager.getDefault();
                        if (info.java.majorVersion != linfo.java.majorVersion){
                            // Last solution, download new Java and relaunch (if there is internet here of course...)
                            info.java = null;

                            handleState(JAVA + linfo.java.majorVersion);
                            Logger.getLogger().log(LogType.INFO, "Downloading Java " + linfo.java.majorVersion);
                            try{
                                JavaManager.getManager().downloadAndInclude(linfo.java, null);
                                //handler.execute(new KeyEvent("jvdown"));
                            } catch (NoConnectionException e){
                                handleState(JAVA_DOWNLOAD_ERROR);
                                return;
                            }
                            launch(info);
                            return;
                        }
                    }
                }
            }

            Logger.getLogger().log(LogType.INFO, "Checking Java executable...");
            if (!info.java.getWindowExecutable().exists()){
                Logger.getLogger().log(LogType.INFO, "Java executable '" + info.java.getWindowExecutable() + "' does not exist, invalidating versions.");
                JavaManager.getManager().reload();
                info.java = null;
                launch(info);
                return;
            }

            Logger.getLogger().log(LogType.INFO, "Java Path: " + info.java.getPath());

            String libPath = "-Djava.library.path=" + linfo.nativePath;
            String c = String.valueOf(File.pathSeparatorChar);

            var client = linfo.wrappedClientPath.exists() && linfo.wrappedClientPath.getSize() > 0 ? linfo.wrappedClientPath : linfo.clientPath;

            //String cp = String.join(c, linfo.libraries) + c + linfo.clientPath;
            String cp = String.join(c, linfo.libraries) + c + client;

            var locale = Locale.getDefault();

            // Static commands
            String[] rootCmds = {
                    info.java.getWindowExecutable().toString(),
                    libPath,
                    //"-javaagent:" + linfo.agentPath, // Don't worry (yup), this contains launcher's patches to mc like 1.16.4 multiplayer bug
                    "-Dorg.lwjgl.util.Debug=true",
                    "-Dfml.ignoreInvalidMinecraftCertificates=true",
                    "-Duser.language=" + locale.getLanguage(),
                    "-Duser.country=" + locale.getCountry(),
            };

            // If the version lower than 1.7.2 (very legacy) then copy all textures to resources folder in profile folder
            if (linfo.assets.isVeryLegacy()){
                var resources = info.dir.to("resources");
                Configurator.getConfig().getGamePath().to("assets", "virtual", "verylegacy").copy(resources, false);
            }

            boolean authSuccess = false;
            try{
                info.account.cacheToken();
                authSuccess = true;
            }
            catch (PerformException e){
                Logger.getLogger().log(LogType.ERROR, "Authentication failed: " + e.getMessage() + " XErr " + e.getValue());
                if (onAuthFail != null && !onAuthFail.apply((ValueEvent) new ValueEvent(AUTH_FAIL, e.getValue()).setSource(info))){
                    throw new StopException();
                }
                Logger.getLogger().log(LogType.ERROR, "Ignoring authentication failure...");
            }

            if (authSuccess){
                String username = info.account.getTokener().getMicrosoftUsername();
                if (username != null && !username.equals(info.account.getUsername())){
                    var acc = info.account.copyAs(username);
                    if (info.account.equals(Configurator.getConfig().getUser())){
                        Configurator.getConfigurator().setDefaultAccount(acc);
                        Logger.getLogger().log(LogType.INFO, "Microsoft XUID username mismatched with the local username, changing config to use Microsoft's.");
                    }
                    else{
                        Logger.getLogger().log(LogType.INFO, "Microsoft XUID username mismatched with the profile's username, using Microsoft's.");
                    }
                    info.account = acc;
                }
            }

            String[] gameCmds = linfo.getGameArguments();

            String argsss = null;
            var mptxt = linfo.clientPath.parent().to("client.txt");
            if (mptxt.exists() && Configurator.getConfig().isEnabledInGameRPC()){
                String txt = getMappingArgs(mptxt.read());
                argsss = "-Dcom.laeben.clpatcher.args=" + txt;
            }

            // Final commands
            var finalCmds = new CommandConcat()
                    .add(rootCmds)
                    .add(2, linfo.agents)
                    .add(linfo.getJvmArguments())
                    .add(info.args)
                    .add(argsss)
                    .add("-cp", cp)
                    .add(linfo.mainClass)
                    .add(gameCmds)
                    .generate();


            // Due to some reasons, authentication process does not complete without requesting to the certificate URL, so we are requesting here.
            info.account.validate();

            // Start a new session
            var session = new Session(info.dir, finalCmds);
            session.setOnPacketReceived(a -> handler.execute(new ValueEvent(SESSION_RECEIVE, a)));

            handleState(SESSION_START + info.executor);
            session.start();

            handler.execute(new ValueEvent(SESSION_END + info.executor, session.getExitCode()));
        }
        catch (StopException | VersionNotFoundException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    private String getMappingArgs(String txt){
        var mapper = new PGMapper(txt);

        String handle = "net.minecraft.client.multiplayer.ClientPacketListener";
        String title = "net.minecraft.client.gui.screens.TitleScreen";
        String mc = "net.minecraft.client.Minecraft";
        String multi = "net.minecraft.client.multiplayer.ServerData";
        String single = "net.minecraft.server.MinecraftServer";

        var hCls = mapper.getClass(handle);
        var handleLogin = hCls.getMethod("handleLogin");

        var mcCls = mapper.getClass(mc);
        var getInstance = mcCls.getMethod("getInstance");
        var getSingleplayerServer = mcCls.getMethod("getSingleplayerServer");

        var msCls = mapper.getClass(single);
        var getMotd = msCls.getMethod("getMotd");

        var getCurrentServer = mcCls.getMethod("getCurrentServer");

        var sdCls = mapper.getClass(multi);
        var ipField = sdCls.getField("ip");

        var tCls = mapper.getClass(title);
        var init = tCls.getMethod("init");

        return String.join(",", Stream.of(hCls, mcCls, getInstance, getSingleplayerServer, getMotd, getCurrentServer, ipField, tCls, init, handleLogin).map(Object::toString).toList());
    }

}
