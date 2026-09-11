package com.laeben.corelauncher.minecraft.loader.fabric;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.core.network.entity.NetworkToken;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.minecraft.loader.fabric.entity.BaseFabricVersion;
import com.laeben.corelauncher.minecraft.loader.fabric.entity.FabricVersion;
import com.laeben.corelauncher.minecraft.token.VersionToken;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.api.entity.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Fabric<T extends BaseFabricVersion> extends Loader<T> {

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
    public T getVersion(String id, String wrId, RedownloadSettings redownloadSettings) {
        return (T)getVersions(id, redownloadSettings).stream().filter(x -> x.getLoaderVersion().equals(wrId)).findFirst().orElse(null);
    }

    protected T getFabricVersion(){
        return (T)new FabricVersion();
    }

    protected T getFabricVersion(String id, String wrId) {
        return (T)new FabricVersion(id, wrId);
    }

    protected String getInstaller(RedownloadSettings redownloadSettings) throws NoConnectionException, HttpException, IOException, StopException {
        if (cacheInstaller != null && !redownloadSettings.hasClient())
            return cacheInstaller;
        var arr = gson.fromJson(Network.urlToString(getInstallerUrl()), JsonArray.class);
        return cacheInstaller = arr.get(0).getAsJsonObject().get("url").getAsString();
    }

    @Override
    public T getVersionFromIdentifier(String identifier, String inherits){
        if (inherits == null)
            inherits = "*";
        return identifier.startsWith(getType().getIdentifier()) ? getFabricVersion(inherits, identifier.split("-")[2]) : null;
    }

    @Override
    public List<T> getAllVersions(RedownloadSettings redownloadSettings) {
        return null;
    }

    @Override
    public List<T> getVersions(String id, RedownloadSettings redownloadSettings) {
        logState("acqVersionFabric - " + id);

        // version id does not matter now

        if (!cache.isEmpty() && !redownloadSettings.hasClient()){
            cache.forEach(a -> a.id = id);
            return (List<T>)cache;
        }
        cache.clear();

        try{
            String url = getBaseUrl() + "versions/loader/" + id;
            var json = gson.fromJson(Network.urlToString(url), JsonArray.class);
            if (json == null)
                return List.of();

            for(var i : json){
                var v = getFabricVersion()
                        .setLoaderVersion("." + getType() + ":" + i.getAsJsonObject().get("loader").getAsJsonObject().get("version").getAsString());
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
    public void install(VersionToken<T> token) throws NoConnectionException, StopException {
        Vanilla.getVanilla().install(token);

        final T version = token.getVersion();

        var gameDir = Configurator.getConfig().getGamePath();
        String jsonName = version.getJsonName();
        var temp = Configurator.getConfig().getTemporaryFolder();
        var jsonPath = gameDir.to("versions", jsonName, jsonName + ".json");
        var clientPath = gameDir.to("versions", jsonName, jsonName + ".jar");
        if (clientPath.exists() && !token.getRedownloadSettings().hasClient())
            return;

        try{
            logState(".fabric.state.download");

            String installer = getInstaller(token.getRedownloadSettings());
            var path = Network.download(NetworkToken.create(installer, temp.to("quiltinstaller.jar"), false).syncWith(token));

            if (token.shouldStop())
                throw new StopException();

            logState(".fabric.state.install");
            try{
                var process = new ProcessBuilder()
                        .command(JavaManager.getDefault().getWindowExecutable().toString(), "-jar", path.toString(), "client", "-dir", gameDir.toString(), "-mcversion", version.id, "-loader", version.getLoaderVersion(), "-noprofile")
                        .inheritIO()
                        .start();
                process.waitFor();
                clientPath.write("");
            }
            catch (Exception e){
                Logger.getLogger().logHyph("ERRFABRIC " + version.getLoaderVersion());
                Logger.getLogger().log(e);
                logState(UNKNOWN_ERROR);
            }

            path.delete();

            logState(LAUNCH_FINISH);
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
