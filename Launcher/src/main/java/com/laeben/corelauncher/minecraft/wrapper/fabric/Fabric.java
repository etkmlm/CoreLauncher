package com.laeben.corelauncher.minecraft.wrapper.fabric;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.fabric.entity.BaseFabricVersion;
import com.laeben.corelauncher.minecraft.wrapper.fabric.entity.FabricVersion;
import com.laeben.corelauncher.util.JavaManager;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

public class Fabric<T extends BaseFabricVersion> extends Wrapper<T> {

    private static final String BASE_URL = "https://meta.fabricmc.net/v2/";

    private static final List<BaseFabricVersion> cache = new ArrayList<>();

    private final Gson gson;
    private String cacheInstaller;

    public Fabric(){
        gson = new Gson();
    }

    protected String getBaseUrl(){
        return BASE_URL;
    }

    protected String getInstallerUrl(){
        return getBaseUrl() + "versions/installer";
    }

    @Override
    public T getVersion(String id, String wrId) {
        return (T)getVersions(id).stream().filter(x -> x.getWrapperVersion().equals(wrId)).findFirst().orElse(null);
    }

    protected T getFabricVersion(){
        return (T)new FabricVersion();
    }

    protected T getFabricVersion(String id, String wrId) {
        return (T)new FabricVersion(id, wrId);
    }

    protected String getInstaller() throws NoConnectionException, HttpException {
        if (cacheInstaller != null && !disableCache)
            return cacheInstaller;
        var arr = gson.fromJson(NetUtil.urlToString(getInstallerUrl()), JsonArray.class);
        return cacheInstaller = arr.get(0).getAsJsonObject().get("url").getAsString();
    }

    @Override
    public T getVersionFromIdentifier(String identifier, String inherits){
        if (inherits == null)
            inherits = "*";
        return identifier.startsWith(getType().getIdentifier()) ? getFabricVersion(inherits, identifier.split("-")[2]) : null;
    }

    @Override
    public List<T> getAllVersions() {
        return null;
    }

    @Override
    public List<T> getVersions(String id) {
        logState("acqVersionFabric - " + id);

        if (!cache.isEmpty() && !disableCache)
            return (List<T>) cache;
        cache.clear();

        try{
            String url = getBaseUrl() + "versions/loader/" + id;
            var json = gson.fromJson(NetUtil.urlToString(url), JsonArray.class);
            if (json == null)
                return List.of();

            for(var i : json){
                var v = getFabricVersion()
                        .setWrapperVersion("." + getType() + ":" + i.getAsJsonObject().get("loader").getAsJsonObject().get("version").getAsString());
                v.id = id;

                cache.add((BaseFabricVersion) v);
            }
        }
        catch (NoConnectionException e){
            return getOfflineVersions().stream().filter(x -> x.checkId(id)).toList();
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        return (List<T>) cache;
    }

    @Override
    public void install(T v) throws NoConnectionException, StopException {
        Vanilla.getVanilla().install(v);

        var gameDir = Configurator.getConfig().getGamePath();
        String jsonName = v.getJsonName();
        var temp = Configurator.getConfig().getTemporaryFolder();
        var jsonPath = gameDir.to("versions", jsonName, jsonName + ".json");
        var clientPath = gameDir.to("versions", jsonName, jsonName + ".jar");
        if (clientPath.exists() && !disableCache)
            return;

        try{
            logState(".fabric.state.download");

            String installer = getInstaller();
            var path = NetUtil.download(installer, temp.to("quiltinstaller.jar"), false, false);

            if (stopRequested)
                throw new StopException();

            logState(".fabric.state.install");
            try{
                var process = new ProcessBuilder()
                        .command(JavaManager.getDefault().getWindowExecutable().toString(), "-jar", path.toString(), "client", "-dir", gameDir.toString(), "-mcversion", v.id, "-loader", v.getWrapperVersion(), "-noprofile")
                        .inheritIO()
                        .start();
                process.waitFor();
                clientPath.write("");
            }
            catch (Exception e){
                Logger.getLogger().logHyph("ERRFABRIC " + v.getWrapperVersion());
                Logger.getLogger().log(e);
                logState(".error.unknown");
            }

            path.delete();

            logState(".launch.state.finish");
        }
        catch (NoConnectionException | StopException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

    }

    @Override
    public LoaderType getType() {
        return LoaderType.FABRIC;
    }
}
