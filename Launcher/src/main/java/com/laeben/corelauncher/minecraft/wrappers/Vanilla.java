package com.laeben.corelauncher.minecraft.wrappers;

import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entities.MainInfo;
import com.laeben.corelauncher.minecraft.entities.Version;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.CurseWrapper;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.corelauncher.utils.entities.LogType;
import com.laeben.corelauncher.utils.entities.NoConnectionException;
import com.laeben.corelauncher.utils.entities.Path;

import java.util.List;

public class Vanilla extends Wrapper<Version> {
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
        logState("acqVersion" + id);
        return getAllVersions().stream().filter(x -> x.checkId(id)).findFirst().orElse(new Version());
    }


    private String getVersionString(String id){
        var v = getAllVersions().stream().filter(x -> x.checkId(id)).findFirst();
        return v.map(version -> NetUtils.urlToString(version.url)).orElse(null);
    }

    @Override
    public Version getVersionFromIdentifier(String identifier, String inherits){
        boolean f = false;
        String idLower = identifier.toLowerCase();
        for (String i : wrappers.keySet()){
            if (idLower.contains(i) && !i.equals(getIdentifier()))
                f = true;
        }
        return !f ? new Version(identifier) : null;
    }

    @Override
    public List<Version> getAllVersions() {
        if (_info == null || disableCache){
            try{
                _info = GsonUtils.empty().fromJson(NetUtils.urlToString(INFO_URL), MainInfo.class);
            }
            catch (NoConnectionException e){
                return getOfflineVersions();
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }
        return _info.versions;
    }

    @Override
    public List<Version> getVersions(String id) {
        return null;
    }

    @Override
    public void install(Version v) {
        if (v.id == null)
            return;

        Path verDir = getGameDir().to("versions", v.id);
        Path jsonPath = verDir.to(v.id + ".json");
        Path clientPath = verDir.to(v.id + ".jar");

        setupLauncherLibraries();

        Version info;

        try{
            if (!jsonPath.exists() || disableCache)
            {
                String vJson = getVersionString(v.id);
                info = GsonUtils.empty().fromJson(vJson, Version.class);
                jsonPath.write(vJson);
            }
            else
                info = GsonUtils.empty().fromJson(jsonPath.read(), Version.class);

            if (!clientPath.exists() || disableCache/* || NetUtils.getContentLength(info.downloads.client.url) != clientPath.getSize()*/){
                logState("clientDownload");
                Logger.getLogger().printLog(LogType.INFO, "Downloading client " + v.id + "...");
                NetUtils.download(info.downloads.client.url, clientPath, false, handler::execute);
            }

            Logger.getLogger().printLog(LogType.INFO, "Vanilla Version " + v.id + " up to date!");
        }
        catch (NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return;
        }

        downloadLibraries(info);
        downloadAssets(info);
    }

    @Override
    public String getIdentifier() {
        return "vanilla";
    }


    @Override
    public CurseWrapper.Type getType() {
        return CurseWrapper.Type.ANY;
    }
}
