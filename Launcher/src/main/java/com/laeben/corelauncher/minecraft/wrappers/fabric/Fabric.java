package com.laeben.corelauncher.minecraft.wrappers.fabric;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.CurseWrapper;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.minecraft.wrappers.fabric.entities.FabricVersion;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.NetUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fabric extends Wrapper<FabricVersion> {

    private static final String BASE_URL = "https://meta.fabricmc.net/v2/";

    private static final Map<String, List<FabricVersion>> cache = new HashMap<>();

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
    public String getIdentifier() {
        return "fabric";
    }

    @Override
    public FabricVersion getVersion(String id, String wrId) {
        return getVersions(id).stream().filter(x -> x.getWrapperVersion().equals(wrId)).findFirst().orElse(null);
    }

    protected String getInstaller(){
        if (cacheInstaller != null && !disableCache)
            return cacheInstaller;
        var arr = gson.fromJson(NetUtils.urlToString(getInstallerUrl()), JsonArray.class);
        return cacheInstaller = arr.get(0).getAsJsonObject().get("url").getAsString();
    }

    @Override
    public FabricVersion getVersionFromIdentifier(String identifier, String inherits){
        if (inherits == null)
            inherits = "*";
        return identifier.startsWith(getIdentifier()) ? new FabricVersion(inherits, identifier.split("-")[2]) : null;
    }

    @Override
    public List<FabricVersion> getAllVersions() {
        return null;
    }

    @Override
    public List<FabricVersion> getVersions(String id) {

        logState("acqVersionFabric - " + id);

        if (cache.containsKey(id) && !disableCache)
            return cache.get(id);
        cache.remove(id);

        try{
            String url = getBaseUrl() + "versions/loader/" + id;
            var json = gson.fromJson(NetUtils.urlToString(url), JsonArray.class);
            if (json == null)
                return List.of();

            var all = new ArrayList<FabricVersion>();
            for(var i : json){
                FabricVersion v = new FabricVersion()
                        .setWrapperVersion("." + getIdentifier() + ":" + i.getAsJsonObject().get("loader").getAsJsonObject().get("version").getAsString());
                v.id = id;

                all.add(v);
            }
            cache.put(id, all);
        }
        catch (NoConnectionException e){
            return getOfflineVersions().stream().filter(x -> x.checkId(id)).toList();
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        return cache.get(id);
    }

    @Override
    public void install(FabricVersion v) {
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
            var path = NetUtils.download(installer, temp.to("quiltinstaller.jar"), false, null);

            if (stopRequested)
                throw new StopException();

            logState(".fabric.state.install");
            try{
                var process = new ProcessBuilder()
                        .command(JavaMan.getDefault().getExecutable().toString(), "-jar", path.toString(), "client", "-dir", gameDir.toString(), "-mcversion", v.id, "-loader", v.getWrapperVersion(), "-noprofile")
                        .inheritIO()
                        .start();
                process.waitFor();
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
    public CurseWrapper.Type getType() {
        return CurseWrapper.Type.FABRIC;
    }
}
