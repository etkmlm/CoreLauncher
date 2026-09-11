package com.laeben.corelauncher.minecraft.loader;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.core.network.entity.NetworkToken;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.entity.MainInfo;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.token.VersionToken;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
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
    public Version getVersion(String id, String wrId, RedownloadSettings redownloadSettings){
        logState(Loader.ACQUIRE_VERSION + id);
        return getAllVersions(redownloadSettings).stream().filter(x -> x.checkId(id)).findFirst().orElse(null);
    }

    private String getVersionString(String id, RedownloadSettings redownloadSettings){
        var v = getAllVersions(redownloadSettings).stream().filter(x -> x.checkId(id)).findFirst();
        return v.map(version -> {
            try{
                return Network.urlToString(version.url);
            }
            catch (Exception e){
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
    public List<Version> getAllVersions(RedownloadSettings redownloadSettings) {
        if (_info == null || redownloadSettings.hasClient())
            reload();

        return _info == null ? getOfflineVersions() : _info.versions;
    }

    public String getLatestRelease(RedownloadSettings redownloadSettings){
        if (_info == null || redownloadSettings.hasClient())
            reload();

        return _info == null ? null : _info.latest.release;
    }

    @Override
    public List<Version> getVersions(String id, RedownloadSettings redownloadSettings) {
        return null;
    }

    @Override
    public void install(VersionToken token) throws NoConnectionException, StopException {
        final Version version = token.getVersion();

        if (version.id == null)
            return;

        Path verDir = getGameDir().to("versions", version.id);
        Path jsonPath = verDir.to(version.id + ".json");
        Path clientPath = verDir.to(version.id + ".jar");
        Path mappingPath = verDir.to("client.txt");

        setupLauncherLibraries();

        Version info;

        try{
            if (token.shouldStop())
                throw new StopException();

            if (!jsonPath.exists() || token.getRedownloadSettings().hasClient())
            {
                String vJson = getVersionString(version.id, token.getRedownloadSettings());
                info = GsonUtil.EMPTY_GSON.fromJson(vJson, Version.class);
                jsonPath.write(vJson);
            }
            else
                info = GsonUtil.EMPTY_GSON.fromJson(jsonPath.read(), Version.class);

            if (!clientPath.exists() || token.getRedownloadSettings().hasClient() || !checkLen(info.downloads.client.url, clientPath)){
                logState(Loader.CLIENT_DOWNLOAD);
                Logger.getLogger().logDebug("Downloading client " + version.id + "...");
                Network.download(NetworkToken.create(info.downloads.client.url, clientPath, false).withLogging(token.getOnProgress()).syncWith(token));
            }

            try{
                if ((!mappingPath.exists() || token.getRedownloadSettings().hasClient()) && info.downloads.client_mappings != null){
                    Network.download(NetworkToken.create(info.downloads.client_mappings.url, mappingPath, false).syncWith(token));
                }
            }
            catch (Exception ignored){

            }

            Logger.getLogger().logDebug("Vanilla Version " + version.id + " up to date!");
        }
        catch (StopException | NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return;
        }

        final var newToken = VersionToken.create(info, token.getFileCheckMode(), token.getRedownloadSettings(), token.getOnProgress()).syncWith(token);
        downloadLibraries(newToken);
        downloadAssets(newToken);
    }

    public void reload(){
        _info = null;
        try{
            _info = GsonUtil.EMPTY_GSON.fromJson(Network.urlToString(INFO_URL), MainInfo.class);
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
