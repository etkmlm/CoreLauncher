package com.laeben.corelauncher.utils;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.utils.entities.Java;
import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.core.util.events.ProgressEvent;
import com.google.gson.JsonArray;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaMan {
    static class JavaDownloadInfo{
        public String name;
        public String url;
        public int majorVersion;

        public JavaDownloadInfo(String name, String url, int majorVersion){
            this.name = name;
            this.url = url;
            this.majorVersion = majorVersion;
        }
    }
    private static final String ADOPTIUM = "https://api.adoptium.net/v3/assets/latest/";

    private static JavaMan instance;

    private final EventHandler<ChangeEvent> handler;
    private List<Java> javaVersions;

    private Path javaDir;

    public JavaMan(){
        javaDir = javaDir();

        Configurator.getConfigurator().getHandler().addHandler("jvman", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;

            javaDir = javaDir();

            reload();
        }, false);

        handler = new EventHandler<>();

        instance = this;
    }

    public static JavaMan getManager(){
        return instance;
    }
    public static Path javaDir(){
        return Configurator.getConfig().getLauncherPath().to("java");
    }
    public EventHandler<ChangeEvent> getHandler(){
        return handler;
    }

    private List<Java> getAll(){
        try{
            var files = javaDir.getFiles();
            var own = files.stream().filter(Path::isDirectory).map(Java::new);
            var cst = Configurator.getConfig().getCustomJavaVersions();
            return (cst != null ? Stream.concat(own, cst.stream()) : own).collect(Collectors.toList());
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return List.of();
        }
    }

    public List<Java> getAllJavaVersions(){
        return javaVersions;
    }

    public static Java getDefault(){
        return new Java(Path.begin(OSUtils.getRunningJavaDir()));
    }

    private static JavaDownloadInfo getJavaInfo(Java j, boolean is64Bit){
        try{
            var os = CoreLauncher.SYSTEM_OS;
            String url = ADOPTIUM + j.majorVersion + "/hotspot?os=" + os.getName() + "&image_type=jdk&architecture=" + (is64Bit ? "x64" : "x86");
            var object = GsonUtils.empty().fromJson(NetUtils.urlToString(url), JsonArray.class).get(0);
            if (object == null)
                return null;
            var obj = object.getAsJsonObject();
            return new JavaDownloadInfo(obj.get("release_name").getAsString(), obj.getAsJsonObject("binary").getAsJsonObject("package").get("link").getAsString(), j.majorVersion);
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

    public void download(Java java){
        if (java.majorVersion == 0)
            return;

        var info = getJavaInfo(java, CoreLauncher.OS_64);

        if (info == null)
            return;

        try{
            var file = NetUtils.download(info.url, javaDir, true, true);
            file.extract(null, null);
            file.delete();

            var j = new Java(javaDir.to(info.name));
            javaVersions.add(j);
            handler.execute(new ChangeEvent("addJava", null, j));
        }
        catch (NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
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

            handler.execute(new ChangeEvent("delJava", j, null));
        }catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public boolean addCustomJava(Java j){
        if (javaVersions.stream().anyMatch(x -> x.equals(j) || (x.getName() != null && x.getName().equals(j.getName()))))
            return false;
        try{
            javaVersions.add(j);

            Configurator.getConfig().getCustomJavaVersions().add(j);
            Configurator.save();

            handler.execute(new ChangeEvent("addJava", null, j));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return false;
        }

        return true;
    }
}
