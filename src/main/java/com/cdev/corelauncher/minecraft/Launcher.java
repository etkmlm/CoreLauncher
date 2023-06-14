package com.cdev.corelauncher.minecraft;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.entities.*;
import com.cdev.corelauncher.minecraft.utils.CommandConcat;
import com.cdev.corelauncher.ui.utils.EventHandler;
import com.cdev.corelauncher.utils.ConditionConcat;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Launcher {
    public static Launcher instance;

    private static final String INFO_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String ASSET_URL = "https://resources.download.minecraft.net/";

    private MainInfo _info;
    private Path gameDir;
    private final EventHandler<LauncherEvent> handler;

    private boolean isGameRunning;

    public Launcher(){
        this.gameDir = Configurator.getConfig().getGamePath();
        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("launcher", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;

            setGameDir((Path) a.getNewValue());
        });

        instance = this;
    }

    public static Launcher getLauncher(){
        return instance;
    }

    public Path getGameDir(){
        return gameDir;
    }
    public Launcher setGameDir(Path path){
        gameDir = path;
        return this;
    }

    public EventHandler<LauncherEvent> getHandler(){
        return handler;
    }

    private void handleProgress(double value){
        handler.execute(new LauncherEvent(LauncherEvent.LauncherEventType.PROGRESS, "prg", value));
    }

    private void handleState(String key){
        handler.execute(new LauncherEvent(LauncherEvent.LauncherEventType.STATE, key, null));
    }

    public void reload() {
        _info = new Gson().fromJson(NetUtils.urlToString(INFO_URL), MainInfo.class);
    }

    private String getVersionString(String id){
        var v = _info.versions.stream().filter(x -> x.id.equals(id)).findFirst();
        return v.map(version -> NetUtils.urlToString(version.url)).orElse(null);
    }

    public void downloadVersion(String id)
    {
        Logger.getLogger().log(LogType.INFO, "-------RETRIEVING VERSION BEGIN: " + id + "-------");

        Path verDir = gameDir.to("versions", id);
        Path jsonPath = verDir.to(id + ".json");
        Path clientPath = verDir.to(id + ".jar");

        Version info;

        if (!jsonPath.exists())
        {
            String vJson = getVersionString(id);
            info = new Gson().fromJson(vJson, Version.class);
            jsonPath.write(vJson);
        }
        else
            info = new Gson().fromJson(jsonPath.read(), Version.class);

        if (!clientPath.exists() || NetUtils.getContentLength(info.downloads.client.url) != clientPath.getSize()){
            handleState("clientDownload");
            Logger.getLogger().log(LogType.INFO, "Downloading client...");
            NetUtils.download(info.downloads.client.url, clientPath, false, this::handleProgress);
        }

        Logger.getLogger().printLog(LogType.INFO, "Version up to date!");

        downloadLibraries(info);
        downloadAssets(info);

        Logger.getLogger().log(LogType.INFO, "-------RETRIEVING VERSION END: " + id + "-------\n");
    }

    public List<Version> getAllVersions(){
        return _info.versions;
    }

    private void downloadLibraries(Version v)
    {
        Logger.getLogger().printLog(LogType.INFO, "Retrieving libraries...");

        Path libDir = gameDir.to("libraries");
        Path nativeDir = gameDir.to("versions", v.id, "natives");

        for(var lib : v.libraries)
        {
            Logger.getLogger().log(LogType.INFO, "LIB: " + lib.name);
            handleState("lib" + lib.name);
            if (lib.rules != null && !lib.rules.stream().anyMatch(x -> x.action.equals("allow") && (x.os == null || x.checkOS(CoreLauncher.SYSTEM_OS))))
            {
                Logger.getLogger().log(LogType.INFO, "PASS\n");
                continue;
            }

            var artifactAsset = lib.downloads.artifact;
            var nativeAsset = lib.downloads.classifiers != null ? lib.downloads.classifiers.getNatives(CoreLauncher.SYSTEM_OS) : null;

            if (artifactAsset != null)
                downloadLibraryAsset(artifactAsset, libDir, nativeDir, null);

            if (nativeAsset != null)
                downloadLibraryAsset(nativeAsset, libDir, nativeDir, lib.extract == null ? new ArrayList<>() : lib.extract.exclude);

            Logger.getLogger().log(LogType.INFO, "OK\n");
        }
    }

    private void downloadLibraryAsset(Asset asset, Path libDir, Path nativeDir, List<String> exclude)
    {
        Path libPath = libDir.to(asset.path.split("/"));
        if (!libPath.exists())
        {
            NetUtils.download(asset.url, libPath, false, null);
        }

        if (exclude != null)
        {
            libPath.extract(nativeDir, exclude);
        }
    }

    private void downloadAssets(Version v)
    {
        Logger.getLogger().log(LogType.INFO, "Retrieving assets...");

        Path assetDir = gameDir.to("assets", "objects");
        Path fileDir = gameDir.to("assets", "indexes");
        Path assetFile = fileDir.to(v.assetIndex.id + ".json");
        Path legacyDir = gameDir.to("assets", "virtual", "legacy");
        Path veryLegacyDir = gameDir.to("assets", "virtual", "verylegacy");

        String asstText;
        if (!assetFile.exists())
            assetFile.write(asstText = NetUtils.urlToString(v.assetIndex.url));
        else
            asstText = assetFile.read();

        var n = new Gson().fromJson(asstText, JsonObject.class);

        var index = new AssetIndex();
        index.objects = new ArrayList<>();

        for (var x : n.getAsJsonObject("objects").entrySet())
            index.objects.add(new Asset(x.getKey(), x.getValue().getAsJsonObject().get("hash").getAsString(), x.getValue().getAsJsonObject().get("size").getAsInt()));

        int count = index.objects.size();
        int i = 1;
        for (var asset : index.objects)
        {
            String hash = asset.SHA1;
            String nhash = hash.substring(0, 2);
            String url = ASSET_URL + nhash + "/" + hash;

            Path path = assetDir.to(nhash, hash);
            if (!path.exists())
                NetUtils.download(url, path.forceSetDir(false), false, null);

            if (v.assetIndex.isLegacy()){
                var f = legacyDir.to(asset.path);
                path.copy(f);
            }
            else if (v.assetIndex.isVeryLegacy()){
                var f = veryLegacyDir.to(asset.path);
                path.copy(f);
            }

            Logger.getLogger().printLog(LogType.INFO, i++ + " / " + count);
            handleState("asset" + i + "/" + count);
            handleProgress(i * 1.0 / count);
        }
    }

    public boolean isGameRunning(){
        return isGameRunning;
    }

    public void launch(ExecutionInfo info)
    {
        if (info.versionId == null)
            return;
        if (info.dir == null)
            info.dir = gameDir;
        if (info.args == null)
            info.args = new String[0];

        var linfo = info.new LaunchInfo();

        try {

            if (info.account == null)
                info.account = Configurator.getConfig().getUser();

            if (info.java == null){
                info.java = Configurator.getConfig().getDefaultJava();

                if (info.java == null || info.java.majorVersion != linfo.java.majorVersion){
                    info.java = JavaMan.getManager().tryGet(linfo.java);
                    if (info.java == null){
                        info.java = JavaMan.getDefault();
                        if (info.java.majorVersion != linfo.java.majorVersion){
                            handler.execute(new LauncherEvent(LauncherEvent.LauncherEventType.NEED, "java" + linfo.java.majorVersion, info));
                            return;
                        }
                    }
                }

            }

            handleState("gameStart");
            Logger.getLogger().log(LogType.INFO, "-------LAUNCH START: " + info.versionId + "-------");

            String libPath = "-Djava.library.path=" + linfo.nativePath;
            String c = String.valueOf(File.pathSeparatorChar);
            String cp = linfo.clientPath + c + String.join(c, linfo.libraries);

            String[] rootCmds = {
                    info.java.getExecutable().toString(),
                    "-cp", cp,
                    libPath,
                    "-Dorg.lwjgl.util.Debug=true"
            };

            String[] gameCmds;

            if (linfo.assets.isVeryLegacy()){
                gameCmds = new String[] {
                        linfo.mainClass,
                        info.account.getUsername(),
                        "verylegacy",
                        //"--assetsDir", gameDir.to("assets", "virtual", "verylegacy").toString(),
                        "--gameDir", info.dir.toString(),
                };
                var resources = info.dir.to("resources");
                gameDir.to("assets", "virtual", "verylegacy").copy(resources);
            }
            else if (linfo.assets.isLegacy())
                gameCmds = new String[] {
                        linfo.mainClass,
                        "--assetIndex", linfo.assets.id,
                        "--assetsDir", gameDir.to("assets", "virtual", "legacy").toString(),
                        "--version", info.versionId,
                        "--gameDir", info.dir.toString(),
                        "--username", info.account.getUsername()
                };
            else
                gameCmds = new String[] {
                        linfo.mainClass,
                        "--assetIndex", linfo.assets.id,
                        "--assetsDir", gameDir.to("assets").toString(),
                        "--version", info.versionId,
                        "--gameDir", info.dir.toString(),
                        "--accessToken", "T",
                        "--username", info.account.getUsername()
                };



            isGameRunning = true;

            var process = new ProcessBuilder()
                    .directory(info.dir.toFile())
                    .inheritIO()
                    .command(new CommandConcat().add(rootCmds).add(info.args).add(gameCmds).generate())
                    .start();
            process.waitFor();

            isGameRunning = false;

            Logger.getLogger().log(LogType.INFO, "-------LAUNCH END-------");
            handleState("gameEnd");
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

}
