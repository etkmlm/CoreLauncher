package com.cdev.corelauncher.minecraft;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.LauncherConfig;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.minecraft.entities.Asset;
import com.cdev.corelauncher.minecraft.entities.AssetIndex;
import com.cdev.corelauncher.minecraft.entities.Library;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.cdev.corelauncher.minecraft.wrappers.forge.Forge;
import com.cdev.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.cdev.corelauncher.minecraft.wrappers.quilt.Quilt;
import com.cdev.corelauncher.utils.EventHandler;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.NoConnectionException;
import com.cdev.corelauncher.utils.entities.Path;
import com.cdev.corelauncher.utils.events.KeyEvent;
import com.cdev.corelauncher.utils.events.ProgressEvent;
import com.google.gson.*;
import javafx.event.Event;
import javafx.scene.image.Image;

import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class Wrapper<H extends Version> {
    private static final Map<String, Type> wrappers = new HashMap<>(){{
       put("forge", Forge.class);
       put("vanilla", Vanilla.class);
       put("optifine", OptiFine.class);
       put("fabric", Fabric.class);
       put("quilt", Quilt.class);
    }};
    private static final String ASSET_URL = "https://resources.download.minecraft.net/";

    protected EventHandler<Event> handler;
    protected boolean disableCache;

    public Wrapper(){
        handler = new EventHandler<>();
    }

    public EventHandler<Event> getHandler(){
        return handler;
    }

    protected void logState(String key){
        handler.execute(new KeyEvent(key));
    }

    protected void logProgress(double progress){
        handler.execute(new ProgressEvent("wrapperProgress", progress));
    }

    protected Path getGameDir(){
        return Configurator.getConfig().getGamePath();
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
                try(var fixer = Wrapper.class.getResourceAsStream("/com/cdev/corelauncher/libraries/" + l.fileName)){
                    fixer.transferTo(new FileOutputStream(p.prepare().toFile()));
                }
                catch (Exception e){
                    Logger.getLogger().log(e);
                }
            }
        }
    }

    protected void downloadLibraries(Version v)
    {
        Logger.getLogger().printLog(LogType.INFO, "Retrieving libraries...");

        Path libDir = getGameDir().to("libraries");
        Path nativeDir = getGameDir().to("versions", v.getJsonName(), "natives");

        setupLauncherLibraries();

        try{


            for(var lib : v.libraries)
            {
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
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    protected void downloadAssets(Version v)
    {
        Logger.getLogger().printLog(LogType.INFO, "Retrieving assets...");

        try{
            Path assetDir = getGameDir().to("assets", "objects");
            Path fileDir = getGameDir().to("assets", "indexes");
            Path assetFile = fileDir.to(v.assetIndex.id + ".json");
            Path legacyDir = getGameDir().to("assets", "virtual", "legacy");
            Path veryLegacyDir = getGameDir().to("assets", "virtual", "verylegacy");

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
                try{
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
                    logState("asset" + i + "/" + count);
                    logProgress(i * 1.0 / count);
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
        catch (Exception e){
            Logger.getLogger().log(e);
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
        var str = Wrapper.class.getResourceAsStream("/com/cdev/corelauncher/images/" + getIdentifier() + ".png");
        return str == null ? null : new Image(str);
    }
    public abstract String getIdentifier();
    public abstract H getVersionFromIdentifier(String identifier, String inherits);
    public abstract H getVersion(String id, String wrId);
    public abstract List<H> getAllVersions();
    public abstract List<H> getVersions(String id);
    public abstract void install(H v);
}
