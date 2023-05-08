package com.cdev.corelauncher.minecraft;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.entities.*;
import com.cdev.corelauncher.ui.utils.EventHandler;
import com.cdev.corelauncher.utils.JavaManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.StreamUtils;
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

    public Launcher(Path gameDir){
        this.gameDir = gameDir;
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

    public Launcher reload() {
        _info = new Gson().fromJson(StreamUtils.urlToString(INFO_URL), MainInfo.class);
        return this;
    }

    private String getVersionString(String id){
        var v = _info.versions.stream().filter(x -> x.id.equals(id)).findFirst();
        return v.map(version -> StreamUtils.urlToString(version.url)).orElse(null);
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

        if (!clientPath.exists() || StreamUtils.getContentLength(info.downloads.client.url) != clientPath.getSize()){
            handleState("clientDownload");
            Logger.getLogger().log(LogType.INFO, "Downloading client...");
            StreamUtils.download(info.downloads.client.url, clientPath, false, this::handleProgress);
        }

        Logger.getLogger().printLog(LogType.INFO, "Version up to date!");

        downloadLibraries(info);
        downloadAssets(info);

        Logger.getLogger().log(LogType.INFO, "-------RETRIEVING VERSION END: " + id + "-------\n");
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
            StreamUtils.download(asset.url, libPath, false, null);
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

        String asstText;
        if (!assetFile.exists())
            assetFile.write(asstText = StreamUtils.urlToString(v.assetIndex.url));
        else
            asstText = assetFile.read();

        var n = new Gson().fromJson(asstText, JsonObject.class);

        var index = new AssetIndex();
        index.objects = new ArrayList<>();

        for (var x : n.getAsJsonObject("objects").entrySet())
            index.objects.add(new Asset(x.getValue().getAsJsonObject().get("hash").getAsString(), x.getValue().getAsJsonObject().get("size").getAsInt()));

        System.out.println();
        int count = index.objects.size();
        int i = 1;
        for (var asset : index.objects)
        {
            String hash = asset.SHA1;
            String nhash = hash.substring(0, 2);
            String url = ASSET_URL + nhash + "/" + hash;

            Path path = assetDir.to(nhash, hash);
            if (!path.exists())
                StreamUtils.download(url, path, false, null);

            Logger.getLogger().printLog(LogType.INFO, i++ + " / " + count);
            handleState("asset" + i + "/" + count);
            handleProgress(i * 1.0 / count);
        }
    }

    public boolean isGameRunning(){
        return isGameRunning;
    }

    public void launch(Profile profile){
        launch(profile.getVersionId(), "IA", "T", null, null);
    }

    public void launch(String versionId, String username, String accessToken, Java java, Path dir)
    {
        if (dir == null)
            dir = gameDir;

        try {

            var launchInfo = new LaunchInfo(this, versionId);

            if (java == null){
                java = JavaManager.getManager().tryGet(launchInfo.java);
                if (java == null){
                    java = JavaManager.getDefault();
                    if (java.majorVersion != launchInfo.java.majorVersion){
                        handler.execute(new LauncherEvent(LauncherEvent.LauncherEventType.NEED, "java" + launchInfo.java.majorVersion, launchInfo));
                        return;
                    }
                }
            }

            Logger.getLogger().log(LogType.INFO, "-------LAUNCH START: " + versionId + "-------");

            String libPath = "-Djava.library.path=" + launchInfo.nativePath;
            String c = String.valueOf(File.pathSeparatorChar);
            String cp = launchInfo.clientPath + c + String.join(c, launchInfo.libraries);

            isGameRunning = true;

            var process = new ProcessBuilder()
                    .directory(dir.toFile())
                    .inheritIO()
                    .command(
                            java.getPath().toString(),
                            "-cp", cp,
                            libPath,
                            "-Dorg.lwjgl.util.Debug=true",
                            launchInfo.mainClass,
                            "--assetIndex", launchInfo.assetVersion,
                            "--version", versionId,
                            "--gameDir", gameDir.toString(),
                            "--accessToken", accessToken,
                            "--username", username)
                    .start();
            process.waitFor();

            isGameRunning = false;

            Logger.getLogger().log(LogType.INFO, "-------LAUNCH END-------");
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

}
