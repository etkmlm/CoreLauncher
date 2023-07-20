package com.laeben.corelauncher.minecraft;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.minecraft.entities.Asset;
import com.laeben.corelauncher.minecraft.entities.AssetIndex;
import com.laeben.corelauncher.minecraft.entities.Library;
import com.laeben.corelauncher.minecraft.entities.Version;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.CurseWrapper;
import com.laeben.corelauncher.minecraft.wrappers.Custom;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.laeben.corelauncher.minecraft.wrappers.forge.Forge;
import com.laeben.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.laeben.corelauncher.minecraft.wrappers.quilt.Quilt;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.corelauncher.utils.entities.LogType;
import com.laeben.core.entity.Path;
import com.google.gson.*;
import javafx.scene.image.Image;

import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Wrapper<H extends Version> {
    protected static final Map<String, Type> wrappers = new HashMap<>(){{
       put("forge", Forge.class);
       put("vanilla", Vanilla.class);
       put("optifine", OptiFine.class);
       put("fabric", Fabric.class);
       put("quilt", Quilt.class);
       put("custom", Custom.class);
    }};
    private static final String ASSET_URL = "https://resources.download.minecraft.net/";

    protected EventHandler<BaseEvent> handler;
    protected boolean disableCache;
    protected boolean stopRequested;

    public Wrapper(){
        handler = new EventHandler<>();
    }

    public static List<String> getWrappers(){
        return wrappers.keySet().stream().toList();
    }

    public boolean isStopRequested(){
        return stopRequested;
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }

    public void setStopRequested(boolean val){
        stopRequested = val;
    }

    protected void logState(String key){
        handler.execute(new KeyEvent(key));
    }

    protected boolean checkLen(String url, Path file){
        try{
            return !NetUtils.check() || NetUtils.getContentLength(url) == file.getSize();
        }
        catch (NoConnectionException e){
            return true;
        }
    }

    protected Path getGameDir(){
        return Configurator.getConfig().getGamePath();
    }

    private void downloadLibraryAsset(Asset asset, Path libDir, Path nativeDir, List<String> exclude) throws NoConnectionException, StopException, HttpException {
        Path libPath = libDir.to(asset.path.split("/"));
        if (!libPath.exists()/* || !checkLen(asset.url, libPath)*/ || disableCache)
        {
            NetUtils.download(asset.url, libPath, false, true);
        }

        if (exclude != null)
        {
            libPath.extract(nativeDir, exclude);
        }
    }

    protected Path generateProfileInfo(Path target){
        var profileInfo = target.to("launcher_profiles.json");
        profileInfo.write("{\"profiles\":{}}");

        return profileInfo;
    }

    public Wrapper<?> setDisableCache(boolean mode){
        this.disableCache = mode;

        return this;
    }

    protected void setupLauncherLibraries(){
        Path libDir = getGameDir().to("libraries");
        for (Library l : LauncherConfig.LAUNCHER_LIBRARIES){
            Path p = libDir.to(l.calculatePath());
            if (!p.exists()){
                try(var fixer = Wrapper.class.getResourceAsStream("/com/laeben/corelauncher/libraries/" + l.fileName)){
                    fixer.transferTo(new FileOutputStream(p.prepare().toFile()));
                }
                catch (Exception e){
                    Logger.getLogger().log(e);
                }
            }
        }
    }

    protected void downloadLibraries(Version v) throws StopException, NoConnectionException {
        Logger.getLogger().printLog(LogType.INFO, "Retrieving libraries...");

        Path libDir = getGameDir().to("libraries");
        Path nativeDir = getGameDir().to("versions", v.getJsonName(), "natives");

        setupLauncherLibraries();

        if (v.libraries == null)
            return;

        for(var lib : v.libraries)
        {
            if (stopRequested)
                throw new StopException();
            try{
                Logger.getLogger().printLog(LogType.INFO, "LIB: " + lib.name);
                logState("lib" + lib.name);
                if (!lib.checkAvailability(CoreLauncher.SYSTEM_OS))
                {
                    Logger.getLogger().printLog(LogType.INFO, "PASS\n");
                    continue;
                }

                var mainAsset = lib.getMainAsset();
                var nativeAsset = lib.getNativeAsset();

                if (mainAsset != null)
                    downloadLibraryAsset(mainAsset, libDir, nativeDir, null);

                if (nativeAsset != null)
                    downloadLibraryAsset(nativeAsset, libDir, nativeDir, lib.extract == null ? new ArrayList<>() : lib.extract.exclude);

                Logger.getLogger().printLog(LogType.INFO, "OK\n");
            }
            catch (NoConnectionException e){
                throw e;
            }
            catch (Exception e){
                Logger.getLogger().log(LogType.INFO, "ERRLIB: " + lib.name);
                Logger.getLogger().log(e);
            }
        }
    }

    protected void downloadAssets(Version v) throws StopException, NoConnectionException {
        Logger.getLogger().printLog(LogType.INFO, "Retrieving assets...");

        var vIndex = v.getAssetIndex();

        Path assetDir = getGameDir().to("assets", "objects");
        Path fileDir = getGameDir().to("assets", "indexes");
        Path assetFile = fileDir.to(vIndex.id + ".json");
        Path legacyDir = getGameDir().to("assets", "virtual", "legacy");
        Path veryLegacyDir = getGameDir().to("assets", "virtual", "verylegacy");


        AssetIndex index;

        if (stopRequested)
            throw new StopException();

        try{
            String asstText;
            if (!assetFile.exists()){
                if (vIndex.url == null)
                    return;
                assetFile.write(asstText = NetUtils.urlToString(vIndex.url));
            }
            else
                asstText = assetFile.read();

            var n = GsonUtils.empty().fromJson(asstText, JsonObject.class);

            index = new AssetIndex();
            index.objects = new ArrayList<>();

            for (var x : n.getAsJsonObject("objects").entrySet())
                index.objects.add(new Asset(x.getKey(), x.getValue().getAsJsonObject().get("hash").getAsString(), x.getValue().getAsJsonObject().get("size").getAsInt()));

        }
        catch (NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return;
        }

        int count = index.objects.size();
        int i = 1;
        for (var asset : index.objects)
        {
            if (stopRequested)
                throw new StopException();
            try{
                String hash = asset.SHA1;
                String nhash = hash.substring(0, 2);
                String url = ASSET_URL + nhash + "/" + hash;

                Path path = assetDir.to(nhash, hash);
                if (!path.exists() || disableCache)
                    NetUtils.download(url, path.forceSetDir(false), false, true);

                if (vIndex.isLegacy()){
                    var f = legacyDir.to(asset.path);
                    path.copy(f);
                }
                else if (vIndex.isVeryLegacy()){
                    var f = veryLegacyDir.to(asset.path);
                    path.copy(f);
                }

                Logger.getLogger().printLog(LogType.INFO, (i++) + " / " + count);
                logState("asset" + i + "/" + count);
                //logProgress(i, count);
            }
            catch (NoConnectionException e){
                throw e;
            }
            catch (Exception e){
                Logger.getLogger().log(LogType.INFO, "ERRASST: " + asset.id);
                Logger.getLogger().log(e);
            }
        }
    }

    public static <T extends Wrapper> T getWrapper(String identifier){
        try {
            return (T)((Class)wrappers.get(identifier)).getDeclaredConstructors()[0].newInstance();
        } catch (Exception e) {
            Logger.getLogger().log(e);
            return (T) new Vanilla();
        }
    }
    public static class WrapperFactory implements JsonDeserializer<Wrapper<?>>, JsonSerializer<Wrapper<?>> {

        @Override
        public Wrapper deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            Wrapper w;
            try{
                w = getWrapper(jsonElement.getAsString());
            }
            catch (Exception e){
                Logger.getLogger().log(e);
                w = new Vanilla();
            }

            return w;
        }

        @Override
        public JsonElement serialize(Wrapper wrapper, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(wrapper.getIdentifier());
        }
    }

    protected List<H> getOfflineVersions(){
        var versions = Configurator.getConfig().getGamePath().to("versions");
        var gson = new Gson();
        var all = new ArrayList<H>();
        for(var i : versions.getFiles()){
            if (!i.isDirectory())
                continue;
            try{
                var json = i.to(i.getName() + ".json");
                var read = gson.fromJson(json.read(), JsonObject.class);
                if (read == null)
                    continue;
                String id = read.get("id").getAsString();
                String inherits = read.has("inheritsFrom") ? read.get("inheritsFrom").getAsString() : null;
                var ver = getVersionFromIdentifier(id, inherits);
                if (ver != null)
                    all.add(ver);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }

        return all;
    }

    public final Image getIcon(){
        var str = Wrapper.class.getResourceAsStream("/com/laeben/corelauncher/images/" + getIdentifier() + ".png");
        return str == null ? null : new Image(str);
    }
    public abstract String getIdentifier();
    public abstract H getVersionFromIdentifier(String identifier, String inherits);
    public abstract H getVersion(String id, String wrId);
    public abstract List<H> getAllVersions();
    public abstract List<H> getVersions(String id);
    public abstract void install(H v) throws NoConnectionException, StopException;
    public abstract CurseWrapper.Type getType();
}
