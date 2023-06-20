package com.cdev.corelauncher.minecraft.wrappers;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.entities.MainInfo;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.NoConnectionException;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
                _info = new Gson().fromJson(NetUtils.urlToString(INFO_URL), MainInfo.class);
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
                info = new Gson().fromJson(vJson, Version.class);
                jsonPath.write(vJson);
            }
            else
                info = new Gson().fromJson(jsonPath.read(), Version.class);

            if (!clientPath.exists() || disableCache/* || NetUtils.getContentLength(info.downloads.client.url) != clientPath.getSize()*/){
                logState("clientDownload");
                Logger.getLogger().printLog(LogType.INFO, "Downloading client " + v.id + "...");
                NetUtils.download(info.downloads.client.url, clientPath, false, this::logProgress);
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
}
