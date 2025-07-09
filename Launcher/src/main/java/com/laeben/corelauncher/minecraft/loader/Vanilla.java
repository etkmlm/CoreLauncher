package com.laeben.corelauncher.minecraft.loader;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.entity.MainInfo;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.entity.Path;

import java.util.List;

public class Vanilla extends Loader<Version> {
    private static final String INFO_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static MainInfo _info;
    private static Vanilla instance;

    public Vanilla(){

    }

    public Vanilla asInstance(){
        instance = this;
        setupLauncherLibraries();
        return this;
    }

    public static Vanilla getVanilla(){
        return instance;
    }

    @Override
    public Version getVersion(String id, String wrId){
        logState(Loader.ACQUIRE_VERSION + id);
        return getAllVersions().stream().filter(x -> x.checkId(id)).findFirst().orElse(null);
    }

    private String getVersionString(String id){
        var v = getAllVersions().stream().filter(x -> x.checkId(id)).findFirst();
        return v.map(version -> {
            try{
                return NetUtil.urlToString(version.url);
            }
            catch (NoConnectionException | HttpException e){
                return null;
            }
        }).orElse(null);
    }

    @Override
    public Version getVersionFromIdentifier(String identifier, String inherits){
        boolean f = false;
        String idLower = identifier.toLowerCase();
        for (var x : LoaderType.values()){
            var i = x.getIdentifier();
            if (idLower.contains(i) && !i.equals(getType().getIdentifier()))
                f = true;
        }
        return !f ? new Version(identifier) : null;
    }

    @Override
    public List<Version> getAllVersions() {
        if (_info == null || disableCache)
            reload();

        return _info == null ? getOfflineVersions() : _info.versions;
    }

    public String getLatestRelease(){
        if (_info == null || disableCache)
            reload();

        return _info == null ? null : _info.latest.release;
    }

    @Override
    public List<Version> getVersions(String id) {
        return null;
    }

    @Override
    public void install(Version v) throws NoConnectionException, StopException {
        if (v.id == null)
            return;

        Path verDir = getGameDir().to("versions", v.id);
        Path jsonPath = verDir.to(v.id + ".json");
        Path clientPath = verDir.to(v.id + ".jar");
        Path mappingPath = verDir.to("client.txt");

        setupLauncherLibraries();

        Version info;

        try{

            if (stopRequested)
                return;
            if (!jsonPath.exists() || disableCache)
            {
                String vJson = getVersionString(v.id);
                info = GsonUtil.EMPTY_GSON.fromJson(vJson, Version.class);
                jsonPath.write(vJson);
            }
            else
                info = GsonUtil.EMPTY_GSON.fromJson(jsonPath.read(), Version.class);

            if (!clientPath.exists() || disableCache || !checkLen(info.downloads.client.url, clientPath)){
                logState(Loader.CLIENT_DOWNLOAD);
                Logger.getLogger().logDebug("Downloading client " + v.id + "...");
                NetUtil.download(info.downloads.client.url, clientPath, false, true);
            }

            try{
                if ((!mappingPath.exists() || disableCache) && info.downloads.client_mappings != null){
                    NetUtil.download(info.downloads.client_mappings.url, mappingPath, false, false);
                }
            }
            catch (Exception ignored){

            }

            Logger.getLogger().logDebug("Vanilla Version " + v.id + " up to date!");
        }
        catch (StopException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return;
        }

        downloadLibraries(info);
        downloadAssets(info);
    }

    public void reload(){
        _info = null;
        try{
            _info = GsonUtil.EMPTY_GSON.fromJson(NetUtil.urlToString(INFO_URL), MainInfo.class);
        }
        catch (NoConnectionException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }


    @Override
    public LoaderType getType() {
        return LoaderType.VANILLA;
    }
}
