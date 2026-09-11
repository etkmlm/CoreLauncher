package com.laeben.corelauncher.minecraft;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.util.CargoNet;
import com.laeben.corelauncher.api.util.entity.NetParcel;
import com.laeben.corelauncher.minecraft.entity.Asset;
import com.laeben.corelauncher.minecraft.entity.AssetIndex;
import com.laeben.corelauncher.minecraft.entity.Library;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.minecraft.token.VersionToken;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.core.entity.Path;
import com.google.gson.*;
import javafx.scene.image.Image;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public abstract class Loader<H extends Version> {
    public static final String CLIENT_DOWNLOAD = "clientDown";
    public static final String SERVER_DOWNLOAD = "serverDown";
    public static final String ACQUIRE_VERSION = "acqVersion";
    public static final String LIBRARY_LOAD = "libLoad";
    public static final String LAUNCH_FINISH = "launchFinish";
    public static final String UNKNOWN_ERROR = "errUnknown";
    //public static final String ASSET = "asset";
    public static final String ASSET_LOAD = "assetLoad";

    private static final String ASSET_URL = "https://resources.download.minecraft.net/";

    protected EventHandler<BaseEvent> handler;

    // not null

    public Loader(){
        handler = new EventHandler<>();
    }

    public static List<String> getLoaders(){
        return Arrays.stream(LoaderType.values()).map(LoaderType::getIdentifier).toList();
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }



    protected void logState(String key){
        handler.execute(new KeyEvent(key));
    }

    protected boolean checkLen(String url, Path file) throws StopException {
        try{
            return !Network.check() || Network.getContentLength(url) == file.getSize();
        }
        catch (NoConnectionException e){
            return true;
        }
        catch (IOException e){
            Logger.getLogger().log(e);
            return true;
        }
    }

    protected Path getGameDir(){
        return Configurator.getConfig().getGamePath();
    }

    private void downloadLibraryAsset(Asset asset, Path libDir, Path nativeDir, List<String> exclude, CargoNet cargo, VersionToken token) throws StopException {
        Path libPath = libDir.to(asset.path.split("/"));
        if (token.getRedownloadSettings().hasLibraries() || !asset.checkAsset(libPath, token.getFileCheckMode()))
        {
            var parcel = NetParcel.create(asset.url, libPath, false).setState(asset);
            if (exclude != null)
                parcel.setOnFinish(() -> {
                    try {
                        libPath.extract(nativeDir, exclude);
                    } catch (StopException ignored) {

                    } catch (IOException e) {
                        Logger.getLogger().log("Library " + libPath + " was failed to extract.", e);
                    }
                });
            cargo.add(parcel);
            //Network.download(asset.url, libPath, false, true);
        }
        else if (exclude != null)
        {
            try {
                libPath.extract(nativeDir, exclude);
            } catch (IOException e) {
                Logger.getLogger().log("Library " + libPath + " was failed to extract.", e);
            }
        }
    }

    protected Path generateProfileInfo(Path target) throws StopException, IOException {
        var profileInfo = target.to("launcher_profiles.json");
        profileInfo.write("{\"profiles\":{}}");

        return profileInfo;
    }

    protected void setupLauncherLibraries(){
        Path libDir = getGameDir().to("libraries");
        for (Library l : LauncherConfig.LAUNCHER_LIBRARIES){
            Path p = libDir.to(l.calculatePath());
            try(var libStr = CoreLauncherFX.class.getResourceAsStream("libraries/" + l.fileName)){
                assert libStr != null;
                if (!p.exists() || p.getSize() != libStr.available()){
                    libStr.transferTo(new FileOutputStream(p.prepare().toFile()));
                }
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }

        }
    }

    protected void downloadLibraries(VersionToken<H> token) throws StopException, NoConnectionException {
        Logger.getLogger().logDebug("Retrieving libraries...");

        final Version version = token.getVersion();

        Path libDir = getGameDir().to("libraries");
        Path nativeDir = getGameDir().to("versions", version.getJsonName(), "natives");

        logState(LIBRARY_LOAD);

        setupLauncherLibraries();

        var cargo = new CargoNet(Configurator.getConfig().getDownloadThreadsCount(), token.getOnProgress(), token) {
            @Override
            public void onParcelDone(NetParcel p, Path path, int done, int total) {
                var lib = p.<Asset>getState();

                if ((!p.isSuccessful() && p.getException() instanceof StopException) || token.shouldStop()){
                    terminate();
                }

                logState(lib.path + " " + done + "/" + total);
            }
        };

        if (version.libraries == null)
            return;

        for(var lib : version.libraries)
        {
            if (token.shouldStop())
                throw new StopException();
            try{
                Logger.getLogger().logDebug("LIB: " + lib.name);
                logState(lib.name);
                if (!lib.checkAvailability(CoreLauncher.SYSTEM_OS))
                {
                    Logger.getLogger().logDebug("PASS\n");
                    continue;
                }

                var mainAsset = lib.getMainAsset();
                var nativeAsset = lib.getNativeAsset();

                if (mainAsset != null)
                    downloadLibraryAsset(mainAsset, libDir, nativeDir, null, cargo, token);

                if (nativeAsset != null)
                    downloadLibraryAsset(nativeAsset, libDir, nativeDir, lib.extract == null ? new ArrayList<>() : lib.extract.exclude, cargo, token);

                Logger.getLogger().logDebug("OK\n");
            }
            catch (Exception e){
                Logger.getLogger().log(LogType.INFO, "ERRLIB: " + lib.name);
                Logger.getLogger().log(e);
            }
        }

        try{

            if (!cargo.await()){
                boolean nc;
                for (var p : cargo.getParcels()){
                    if (p.isSuccessful())
                        continue;
                    var asset = p.<Asset>getState();
                    var ex = p.getException();
                    if (ex instanceof StopException || ex instanceof NoConnectionException){
                        throw ex;
                    }

                    Logger.getLogger().log(LogType.INFO, "ERRLIB: " + asset.path);
                    Logger.getLogger().log(ex);
                }
                throw new PerformException("Exception while completing the cargo request.");
            }
        }
        catch (StopException | NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    protected void downloadAssets(VersionToken<H> token) throws StopException, NoConnectionException {
        Logger.getLogger().logDebug("Retrieving assets...");

        var vIndex = token.getVersion().getAssetIndex();

        Path assetDir = getGameDir().to("assets", "objects");
        Path fileDir = getGameDir().to("assets", "indexes");
        Path assetFile = fileDir.to(vIndex.id + ".json");
        Path legacyDir = getGameDir().to("assets", "virtual", "legacy");
        Path veryLegacyDir = getGameDir().to("assets", "virtual", "verylegacy");

        AssetIndex index;

        if (token.shouldStop())
            throw new StopException();

        try{
            String asstText;
            if (!assetFile.exists()){
                if (vIndex.url == null)
                    return;
                assetFile.write(asstText = Network.urlToString(vIndex.url));
            }
            else
                asstText = assetFile.read();

            var n = GsonUtil.EMPTY_GSON.fromJson(asstText, JsonObject.class);

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

        logState(ASSET_LOAD);

        long stamp = System.currentTimeMillis();

        var cargo = new CargoNet(Configurator.getConfig().getDownloadThreadsCount(), token.getOnProgress(), token) {
            @Override
            public void onParcelDone(NetParcel p, Path path, int done, int size) throws StopException {
                var asset = p.<Asset>getState();
                logState(asset.path + " " + done + "/" + size);

                if (!p.isSuccessful()){
                    if (p.getException() instanceof StopException)
                        terminate();
                    return;
                }

                try {
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
                }
                catch (IOException e){
                    Logger.getLogger().logHyph("Could not move asset from url: " + p.getUrl());
                    Logger.getLogger().log(e);
                }
            }
        };

        int count = index.objects.size();
        int i = 1;
        for (var asset : index.objects)
        {
            if (token.shouldStop())
                throw new StopException();

            String hash = asset.SHA1;
            String nhash = hash.substring(0, 2);
            String url = ASSET_URL + nhash + "/" + hash;

            Path path = assetDir.to(nhash, hash);
            if (token.getRedownloadSettings().hasAssets() || !asset.checkAsset(path, token.getFileCheckMode())){
                cargo.add(NetParcel.create(url, path.forceSetDir(false), false).setState(asset));
                //Network.download(url, path.forceSetDir(false), false, true);
                continue;
            }

            try{
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
            }
            catch (IOException e) {
                Logger.getLogger().log("Could not move asset from path: " + path, e);
            }

            //Logger.getLogger().logDebug((i++) + " / " + count);
            logState(i + "/" + count);
        }

        try{

            if (!cargo.await()){
                boolean nc;
                for (var p : cargo.getParcels()){
                    if (p.isSuccessful())
                        continue;
                    var asset = p.<Asset>getState();

                    var ex = p.getException();
                    if (ex instanceof StopException || ex instanceof NoConnectionException){
                        throw ex;
                    }

                    Logger.getLogger().log(LogType.INFO, "ERRASST: " + asset.id);
                    Logger.getLogger().log(ex);
                }
                throw new PerformException("Exception while completing the cargo request.");
            }
        }
        catch (StopException | NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }


        stamp = System.currentTimeMillis() - stamp;
        Logger.getLogger().log(LogType.INFO, "Assets loaded in " + stamp + " milliseconds.");
    }

    public static <T extends Loader> T getLoader(String identifier){
        try {
            var n = Arrays.stream(LoaderType.values()).filter(a -> a.getIdentifier().equals(identifier)).findFirst();
            return (T)n.get().getCls().getDeclaredConstructors()[0].newInstance();
        } catch (Exception e) {
            //Logger.getLogger().log(e);
            return (T) new Vanilla();
        }
    }
    public static class LoaderFactory implements JsonDeserializer<Loader<?>>, JsonSerializer<Loader<?>> {

        @Override
        public Loader deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            Loader w;
            try{
                w = getLoader(jsonElement.getAsString());
            }
            catch (Exception e){
                Logger.getLogger().log(e);
                w = new Vanilla();
            }

            return w;
        }

        @Override
        public JsonElement serialize(Loader loader, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(loader.getType().getIdentifier());
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
                if (ver != null )
                    all.add(ver);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }

        return all;
    }

    public final Image getIcon(){
        var str = Loader.class.getResourceAsStream("/com/laeben/corelauncher/images/loader/" + getType().getIdentifier() + ".png");
        return str == null ? null : new Image(str);
    }

    public abstract LoaderType getType();
    public abstract H getVersionFromIdentifier(String identifier, String inherits);
    public abstract H getVersion(String id, String wrId, RedownloadSettings redownloadSettings);
    public H getVersion(String id, String wrId) {
        return getVersion(id, wrId, RedownloadSettings.none());
    }
    public abstract List<H> getAllVersions(RedownloadSettings redownloadSettings);
    public List<H> getAllVersions() {
        return getAllVersions(RedownloadSettings.none());
    }
    public abstract List<H> getVersions(String id, RedownloadSettings redownloadSettings);
    public List<H> getVersions(String id) {
        return getVersions(id, RedownloadSettings.none());
    }
    public abstract void install(VersionToken<H> token) throws NoConnectionException, StopException, PerformException;
}
