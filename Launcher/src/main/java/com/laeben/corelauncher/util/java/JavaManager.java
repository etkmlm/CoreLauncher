package com.laeben.corelauncher.util.java;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.corelauncher.util.java.entity.JavaDownloadInfo;
import com.laeben.corelauncher.util.java.entity.JavaSourceType;

import java.util.ArrayList;
import java.util.List;

public class JavaManager {
    public static final String KEY = "jvman";

    public static final String ADD = "add";
    public static final String DELETE = "delete";
    public static final String UPDATE = "update";
    public static final String DOWNLOAD_COMPLETE = "down";

    private JavaSource source;


    private static JavaManager instance;

    private final EventHandler<KeyEvent> handler;
    private List<Java> javaVersions;

    private Path javaDir;

    public JavaManager(){
        javaDir = javaDir();

        Configurator.getConfigurator().getHandler().addHandler(KEY, (a) -> {
            if (a.getKey().equals(Configurator.GAME_PATH_CHANGE)){
                javaDir = javaDir();

                reload();
            }
            else if (a.getKey().equals(Configurator.JAVA_SOURCE_CHANGE)){
                setSourceType((JavaSourceType) a.getNewValue());
            }
        }, false);

        handler = new EventHandler<>();

        setSourceType(Configurator.getConfig().getJavaSourceType());
        instance = this;
    }

    public static JavaManager getManager(){
        return instance;
    }
    public static Path javaDir(){
        return Configurator.getConfig().getLauncherPath().to("java");
    }
    public EventHandler<KeyEvent> getHandler(){
        return handler;
    }

    private List<Java> getAll(){
        var files = javaDir.getFiles();
        var lst = new ArrayList<>(Configurator.getConfigurator().verifyAndGetCustomJavaVersions());
        for (var file : files){
            if (!file.isDirectory())
                continue;

            try {
                var j = new Java(file);
                if (j.isLoaded())
                    lst.add(j);
                else{
                    Logger.getLogger().log(LogType.WARN, "Ignoring invalid Java: " + j.getPath());
                }
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }

        return lst;
    }

    public List<Java> getAllJavaVersions(){
        return javaVersions;
    }

    public static Java getDefault(){
        return new Java(Path.begin(OSUtil.getRunningJavaDir()));
    }

    public JavaDownloadInfo getJavaInfo(Java j, String arch) throws NoConnectionException {
        if (j.majorVersion == 0)
            return null;

        try{
            return source.getJavaInfo(j, CoreLauncher.SYSTEM_OS, arch);
        }
        catch (NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return null;
        }
    }

    public Java tryGet(Java j){
        return javaVersions.stream().filter(x -> x.getName().equals(j.getName()) || x.majorVersion == j.majorVersion).findFirst().orElse(null);
    }

    public void setSourceType(JavaSourceType type){
        source = JavaSource.SOURCES.get(type);
    }

    /**
     * Download and include the Java from the network.
     * One of {@link Java} or {@link JavaDownloadInfo} needs to be null.
     * @param java version info
     * @param info download info
     */
    public void downloadAndInclude(Java java, JavaDownloadInfo info) throws NoConnectionException, StopException {
        if (info == null)
            info = getJavaInfo(java, CoreLauncher.SYSTEM_OS_ARCH);

        final String name = info.name();

        Path existsPath = null;
        for (var ver : javaVersions){
            var path = ver.getPath();
            if (!path.getName().equals(name))
                continue;

            if (!ver.isLoaded()){
                path.delete();
            }
            else{
                existsPath = path;
            }
            break;
        }

        var j = downloadAndExtract(info, existsPath);
        if (j == null)
            return;
        javaVersions.add(j);

        if (existsPath == null) // not necessary to call this in case it was overwritten on an existing java folder
            handler.execute(new ChangeEvent(ADD, null, j));
    }

    /**
     * Downloads and extracts the Java instance.
     * @param info java info to download
     * @param existsPath possible existing instance to delete after download completed
     * @return downloaded instance
     */
    public Java downloadAndExtract(JavaDownloadInfo info, Path existsPath) throws NoConnectionException, StopException {
        if (info == null)
            return null;

        try{
            Main.getMain().setPrimaryStatus(Translator.translateFormat("java.downloading", info.name()));
            var file = NetUtil.download(info.url(), javaDir, true, true);

            if (existsPath != null) // handling existing java version before extracting the file
                existsPath.delete();

            file.extract(null, null);
            file.delete();

            handler.execute(new KeyEvent(DOWNLOAD_COMPLETE));

            return new Java(info.displayName(), javaDir.to(info.name()));
        }
        catch (NoConnectionException | StopException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        return null;
    }

    public void reload(){
        javaVersions = getAll();
    }

    public void deleteJava(Java j){
        try{
            javaVersions.remove(j);

            if (Configurator.getConfig().getCustomJavaVersions().contains(j)){
                Configurator.getConfig().getCustomJavaVersions().remove(j);
                Configurator.save();
            }
            else
                j.getPath().delete();

            Profiler.getProfiler().getAllProfiles().stream().filter(x -> x.getJava() != null && x.getJava().equals(j)).forEach(x -> Profiler.getProfiler().setProfile(x.getName(), y -> y.setJava(null)));

            if (Configurator.getConfig().getDefaultJava() != null && Configurator.getConfig().getDefaultJava().equals(j)){
                Configurator.getConfig().setDefaultJava(null);
                Configurator.save();
            }

            handler.execute(new ChangeEvent(DELETE, j, null));
        }catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public boolean renameCustomJava(Java j, String name){
        if (javaVersions.stream().noneMatch(x -> x.equals(j)) || javaVersions.stream().anyMatch(a -> a.getName() == null || a.getName().equals(name)))
            return false;

        j.setName(name);
        Configurator.save();

        handler.execute(new ChangeEvent(UPDATE, null, j));
        return true;
    }

    public boolean addCustomJava(Java j){
        if (javaVersions.stream().anyMatch(x -> x.equals(j) || (x.getName() != null && x.getName().equals(j.getName()))))
            return false;

        javaVersions.add(j);

        Configurator.getConfig().getCustomJavaVersions().add(j);
        Configurator.save();

        handler.execute(new ChangeEvent(ADD, null, j));
        return true;
    }
}
