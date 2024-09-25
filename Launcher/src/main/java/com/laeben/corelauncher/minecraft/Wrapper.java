package com.laeben.corelauncher.minecraft;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.entity.Asset;
import com.laeben.corelauncher.minecraft.entity.AssetIndex;
import com.laeben.corelauncher.minecraft.entity.Library;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.core.entity.Path;
import com.google.gson.*;
import javafx.scene.image.Image;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.*;

public abstract class Wrapper<H extends Version> {
    private static final String ASSET_URL = "https://resources.download.minecraft.net/";

    protected EventHandler<BaseEvent> handler;
    protected boolean disableCache;
    protected boolean stopRequested;

    public Wrapper(){
        handler = new EventHandler<>();
    }

    public static List<String> getWrappers(){
        return Arrays.stream(LoaderType.values()).map(LoaderType::getIdentifier).toList();
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
            return !NetUtil.check() || NetUtil.getContentLength(url) == file.getSize();
        }
        catch (NoConnectionException e){
            return true;
        }
    }

    protected Path getGameDir(){
        return Configurator.getConfig().getGamePath();
    }

    private void downloadLibraryAsset(Asset asset, Path libDir, Path nativeDir, List<String> exclude) throws NoConnectionException, StopException, HttpException, FileNotFoundException {
        Path libPath = libDir.to(asset.path.split("/"));
        if (!libPath.exists()/* || !checkLen(asset.url, libPath)*/ || disableCache)
        {
            NetUtil.download(asset.url, libPath, false, true);
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
                try(var libStr = CoreLauncherFX.class.getResourceAsStream("libraries/" + l.fileName)){
                    assert libStr != null;
                    libStr.transferTo(new FileOutputStream(p.prepare().toFile()));
                }
                catch (Exception e){
                    Logger.getLogger().log(e);
                }
            }
        }
    }

    protected void downloadLibraries(Version v) throws StopException, NoConnectionException {
        Logger.getLogger().logDebug("Retrieving libraries...");

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
                Logger.getLogger().logDebug("LIB: " + lib.name);
                logState("lib" + lib.name);
                if (!lib.checkAvailability(CoreLauncher.SYSTEM_OS))
                {
                    Logger.getLogger().logDebug("PASS\n");
                    continue;
                }

                var mainAsset = lib.getMainAsset();
                var nativeAsset = lib.getNativeAsset();

                if (mainAsset != null)
                    downloadLibraryAsset(mainAsset, libDir, nativeDir, null);

                if (nativeAsset != null)
                    downloadLibraryAsset(nativeAsset, libDir, nativeDir, lib.extract == null ? new ArrayList<>() : lib.extract.exclude);

                Logger.getLogger().logDebug("OK\n");
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
        Logger.getLogger().logDebug("Retrieving assets...");

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
                assetFile.write(asstText = NetUtil.urlToString(vIndex.url));
            }
            else
                asstText = assetFile.read();

            var n = GsonUtil.empty().fromJson(asstText, JsonObject.class);

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
                    NetUtil.download(url, path.forceSetDir(false), false, true);

                if (vIndex.isLegacy()){
                    var f = legacyDir.to(asset.path);
                    if (!f.exists())
                        path.copy(f);
                }
                else if (vIndex.isVeryLegacy()){
                    var f = veryLegacyDir.to(asset.path);
                    if (!f.exists())
                        path.copy(f);
                }

                Logger.getLogger().logDebug((i++) + " / " + count);
                logState("asset" + i + "/" + count);
                //logProgress(i, count);
            }
            catch (NoConnectionException | StopException e){
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
            var n = Arrays.stream(LoaderType.values()).filter(a -> a.getIdentifier().equals(identifier)).findFirst();
            return (T)n.get().getCls().getDeclaredConstructors()[0].newInstance();
        } catch (Exception e) {
            //Logger.getLogger().log(e);
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
            return new JsonPrimitive(wrapper.getType().getIdentifier());
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
        var str = Wrapper.class.getResourceAsStream("/com/laeben/corelauncher/images/wrapper/" + getType().getIdentifier() + ".png");
        return str == null ? null : new Image(str);
    }

    public abstract LoaderType getType();
    public abstract H getVersionFromIdentifier(String identifier, String inherits);
    public abstract H getVersion(String id, String wrId);
    public abstract List<H> getAllVersions();
    public abstract List<H> getVersions(String id);
    public abstract void install(H v) throws NoConnectionException, StopException, PerformException;
}
