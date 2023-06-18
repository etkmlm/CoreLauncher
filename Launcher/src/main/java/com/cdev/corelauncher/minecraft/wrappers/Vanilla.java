package com.cdev.corelauncher.minecraft.wrappers;

import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.entities.MainInfo;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import javafx.scene.image.Image;

import java.util.List;

public class Vanilla extends Wrapper<Version> {
    private static final String INFO_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static MainInfo _info;
    private static Vanilla instance;

    public Vanilla(){

    }

    public Vanilla asInstance(){
        instance = this;
        return this;
    }

    public static Vanilla getVanilla(){
        return instance;
    }

    @Override
    public Image getIcon(){
        var str = Vanilla.class.getResourceAsStream("/com/cdev/corelauncher/images/vanilla.png");
        return str == null ? null : new Image(str);
    }

    @Override
    public Version getVersion(String id, String wrId){
        logState("acqVersion" + id);
        return _info.versions.stream().filter(x -> x.id.equals(id)).findFirst().orElse(new Version());
    }


    private String getVersionString(String id){
        var v = _info.versions.stream().filter(x -> x.id.equals(id)).findFirst();
        return v.map(version -> NetUtils.urlToString(version.url)).orElse(null);
    }

    @Override
    public List<Version> getAllVersions() {
        _info = new Gson().fromJson(NetUtils.urlToString(INFO_URL), MainInfo.class);
        return _info.versions;
    }

    @Override
    public List<Version> getVersions(String id) {
        return null;
    }

    @Override
    public void install(Version v) {
        Logger.getLogger().log(LogType.INFO, "-------RETRIEVING VERSION BEGIN: " + v.id + "-------");
        if (v.id == null)
            return;
        Path verDir = getGameDir().to("versions", v.id);
        Path jsonPath = verDir.to(v.id + ".json");
        Path clientPath = verDir.to(v.id + ".jar");

        Version info;

        if (!jsonPath.exists())
        {
            String vJson = getVersionString(v.id);
            info = new Gson().fromJson(vJson, Version.class);
            jsonPath.write(vJson);
        }
        else
            info = new Gson().fromJson(jsonPath.read(), Version.class);

        if (!clientPath.exists()/* || NetUtils.getContentLength(info.downloads.client.url) != clientPath.getSize()*/){
            logState("clientDownload");
            Logger.getLogger().log(LogType.INFO, "Downloading client...");
            NetUtils.download(info.downloads.client.url, clientPath, false, this::logProgress);
        }

        Logger.getLogger().printLog(LogType.INFO, "Version up to date!");

        downloadLibraries(info);
        downloadAssets(info);

        Logger.getLogger().log(LogType.INFO, "-------RETRIEVING VERSION END: " + v.id + "-------\n");
    }

    @Override
    public String getIdentifier() {
        return "vanilla";
    }
}
