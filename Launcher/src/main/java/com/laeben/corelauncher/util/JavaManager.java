package com.laeben.corelauncher.util;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.google.gson.JsonArray;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaManager {
    public record JavaDownloadInfo(String name, String url, int major){

    }
    private static final String ADOPTIUM = "https://api.adoptium.net/v3/assets/latest/";

    private static JavaManager instance;

    private final EventHandler<ChangeEvent> handler;
    private List<Java> javaVersions;

    private Path javaDir;

    public JavaManager(){
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

    public static JavaManager getManager(){
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
        return new Java(Path.begin(OSUtil.getRunningJavaDir()));
    }

    public static JavaDownloadInfo getJavaInfo(Java j, boolean is64Bit) throws NoConnectionException {
        if (j.majorVersion == 0)
            return null;

        var os = CoreLauncher.SYSTEM_OS;
        String url = ADOPTIUM + j.majorVersion + "/hotspot?os=" + os.getName() + "&image_type=jdk&architecture=" + (is64Bit ? "x64" : "x86");
        try{
            var object = GsonUtil.empty().fromJson(NetUtil.urlToString(url), JsonArray.class).get(0);
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

    public void downloadAndInclude(Java java) throws NoConnectionException, StopException {
        var info = getJavaInfo(java, CoreLauncher.OS_64);
        var j = download(info);
        if (j == null)
            return;
        javaVersions.add(j);
        handler.execute(new ChangeEvent("add", null, j));
    }

    public Java download(JavaDownloadInfo info) throws NoConnectionException, StopException {
        if (info == null)
            return null;

        try{
            var file = NetUtil.download(info.url, javaDir, true, true);
            file.extract(null, null);
            file.delete();

            return new Java(javaDir.to(info.name));
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

            handler.execute(new ChangeEvent("delete", j, null));
        }catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public boolean renameCustomJava(Java j, String name){
        if (javaVersions.stream().noneMatch(x -> x.equals(j)) || javaVersions.stream().anyMatch(a -> a.getName() == null || a.getName().equals(name)))
            return false;

        j.setName(name);
        Configurator.save();

        handler.execute(new ChangeEvent("update", null, j));
        return true;
    }

    public boolean addCustomJava(Java j){
        if (javaVersions.stream().anyMatch(x -> x.equals(j) || (x.getName() != null && x.getName().equals(j.getName()))))
            return false;

        javaVersions.add(j);

        Configurator.getConfig().getCustomJavaVersions().add(j);
        Configurator.save();

        handler.execute(new ChangeEvent("add", null, j));
        return true;
    }
}
