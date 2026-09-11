package com.laeben.corelauncher.minecraft.modding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.core.network.Network;
import com.laeben.core.network.entity.NetworkToken;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.event.bus.UIEventBus;
import com.laeben.corelauncher.event.context.BasicActionContext;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.token.LaunchToken;
import com.laeben.corelauncher.minecraft.loader.entity.LoaderVersion;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.loader.optifine.OptiFine;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.corelauncher.api.ui.UI;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Modder {
    public static final String RESOURCE_INSTALL = "resIns";

    public enum IncludeMode{
        /**
         * Restrictions will be applied, all safety rules are considered.
         */
        DEFAULT,
        /**
         * Ignore the mismatching between the resources and the profile's game version, loader type and loader version.
         * All resources will be included without any restriction.
         */
        IGNORE_PROFILE,
        /**
         * Only works with single configuration.
         * Allow to change the profile's game version, loader type and loader version if any mismatching occurs with the resources being included.
         */
        OVERWRITE_PROFILE
    }
    public record ModInfo(String name, LoaderType type, String versionId, String loaderVer, String version){}

    private static Modder instance;
    private final EventHandler<BaseEvent> handler;

    public Modder(){
        handler = new EventHandler<>();

        instance = this;
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }

    public static Modder getModder(){
        return instance;
    }

    public List<Modpack> getModpackUpdates(Profile p, List<Modpack> resources) throws NoConnectionException, HttpException, StopException, IOException {
        var all = new ArrayList<Modpack>();
        var opt = ModSource.Options.create(p);
        for (var mp : resources){
            //var vers = getLastVersions(p, mp, true);
            var vers = mp.getSource().getCoreResource(mp.getId(), opt);
            if (vers == null)
                continue;
            var nmp = (Modpack) vers.get(0);
            if (!nmp.equals(mp))
                all.add(nmp);
        }
        return all;
    }

    public Map<Object, List<CResource>> getUpdates(Profile p, List<CResource> resources) throws NoConnectionException, HttpException, StopException, IOException {
        //var forgeResources = resources.stream().filter(a -> !(a instanceof Modpack) && a.isForge() && (!(a instanceof ModpackContent mpc) || mpc.getModpackId() == null)).map(CResource::getIntId).toList();
        //var rinthResources = resources.stream().filter(a -> !(a instanceof Modpack) && a.isModrinth() && (!(a instanceof ModpackContent mpc) || mpc.getModpackId() == null)).toList();

        var forgeResources = new ArrayList<>();
        var rinthResources = new ArrayList<>();

        for (var a : resources){
            if (a instanceof Modpack || (a instanceof ModpackContent mpc && mpc.getModpackId() != null))
                continue;
            if (a.getSourceType() == ModSource.Type.CURSEFORGE)
                forgeResources.add(a.getIntId());
            else if (a.getSourceType() == ModSource.Type.MODRINTH)
                rinthResources.add(a.getId());
        }

        var all = new ArrayList<CResource>();

        var mr = ModSource.Type.MODRINTH.getSource().getCoreResources(rinthResources, ModSource.Options.create(p).dependencies(true));
        var fr = ModSource.Type.CURSEFORGE.getSource().getCoreResources(forgeResources, ModSource.Options.create(p).dependencies(true));
        if (fr != null)
            all.addAll(fr);
        if (mr != null)
            all.addAll(mr);


        return all.stream().filter(a -> !resources.contains(a)).collect(Collectors.groupingBy(CResource::getId));
    }

    public List<CResource> getUpdate(Profile p, CResource m) throws NoConnectionException, HttpException, StopException, IOException {
        if (m instanceof ModpackContent mc && mc.getModpackId() != null)
            return null;

        var opt = ModSource.Options.create(p);
        if (!(m instanceof Modpack))
            opt.dependencies(true);

        return m.getSource().getCoreResource(m.getId(), opt);
    }

    public CResource fill(Profile p, CResource m) throws NoConnectionException, HttpException, StopException, IOException {
        if (m.id == null)
            return m;

        var all = m.getSource().getCoreResource(m.getId(), ModSource.Options.create(p));

        return all == null ? m : all.get(0);
    }

    public void installMods(LaunchToken token, List<Mod> mods) throws NoConnectionException, StopException {
        final var profile = token.getProfile();
        final var path = token.getProfile().getPath().to("mods");
        int i = 0;
        int size = mods.size();

        for (var a : mods.stream().toList()){
            if (token.shouldStop())
                throw new StopException();

            //handler.execute(new KeyEvent(".!" + a.name + "$,resource.progress;" + (++i) + ";" + size));
            handler.execute(new KeyEvent(String.format("%s %d/%d", a.name, ++i, size)));

            if (a.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install mod '" + a.name + "' because it has no file name.");
                continue;
            }

            var pxx = path.to(a.fileName);
            if (!token.getRedownloadSettings().hasMods() && a.checkFile(pxx, token.getFileCheckMode()))
                continue;

            if (a.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install mod '" + a.name + "' because it has no file url.");
                continue;
            }

            try{
                String url = a.fileUrl;
                if (url.startsWith("OptiFine")){
                    var f = OptiFine.getOptiFine().getVersion(profile.getVersionId(), url, token.getRedownloadSettings());
                    if (f == null){
                        Logger.getLogger().logDebug("ERR");
                        continue;
                    }
                    OptiFine.installForge(f, path, token);
                    continue;
                }

                if (Network.download(token.toNetworkToken(url, path.to(a.fileName), false)) != null)
                    continue;

                var up = fill(profile, a);
                if (url.equals(up.fileUrl))
                    continue;

                UI.runAsync(() -> {
                    remove(profile, a);
                    try {
                        include(profile, List.of(up));
                    } catch (NoConnectionException | HttpException e) {
                        Logger.getLogger().log(e);
                    }
                    catch (StopException ignored){

                    }
                });

                if (Network.download(token.toNetworkToken(up.fileUrl, path.to(up.fileName), false)) == null){
                    var x = Modrinth.getModrinth().getProjectVersions(a.name, List.of(profile.getVersionId()), List.of(profile.getLoader().getType()));
                    var s = x.stream().flatMap(f -> f.getFiles().stream()).filter(f -> f.filename != null && f.filename.equals(a.fileName)).findFirst();

                    if (s.isEmpty()){
                        Logger.getLogger().logDebug("ERR");
                        continue;
                    }
                    Network.download(token.toNetworkToken(s.get().url, path.to(a.fileName), false));
                }
            }
            catch (HttpException | IOException e){
                Logger.getLogger().log("Error while installing mod: " + a.name, e);
            }
        }
    }

    public ModInfo getModFromJarFile(Path p) throws IOException, StopException {
        LoaderType type = null;
        String versionId = null;
        String name = null;
        String version = null;
        String loader = null;

        if (!p.getExtension().equals("jar"))
            return null;

        var rd = p.tryReadZipEntry("mcmod.info", "META-INF/mods.toml", "fabric.mod.json", "META-INF/neoforge.mods.toml");

        if (rd == null)
            return null;

        if (rd.getOrder() == 0){ // Minecraft Forge <= 1.12.2
            var read = GsonUtil.DEFAULT_GSON.fromJson(rd.getValue(), JsonElement.class);
            if (read == null)
                return null;
            JsonObject item;

            if (read.isJsonArray()){
                if (read.getAsJsonArray().isEmpty())
                    return null;
                item = read.getAsJsonArray().get(0).getAsJsonObject();
            }
            else if (read.isJsonObject()){
                var obj = read.getAsJsonObject();
                if (obj.has("modList")){
                    var list = obj.get("modList").getAsJsonArray();
                    if (list.isEmpty())
                        return null;
                    item = list.get(0).getAsJsonObject();
                }
                else
                    return null;
            }
            else
                return null;

            name = item.has("name") ? item.get("name").getAsString() : null;
            if (name == null || name.startsWith("$"))
                return null;
            versionId = item.has("mcversion") ? item.get("mcversion").getAsString() : null;
            if (versionId != null && versionId.startsWith("$"))
                versionId = null;
            version = item.has("version") ? item.get("version").getAsString() : null;
            if (version != null && version.startsWith("$"))
                version = null;

            type = LoaderType.FORGE;
        }
        else if (rd.getOrder() == 1 || rd.getOrder() == 3){ // Minecraft Forge > 1.12.2  or NeoForge
            final var pattern = Pattern.compile(".*loaderVersion[^\"]*\"([^\"]*)\".*\\[\\s*\\[mods]\\s*](?=[^\\[]*displayName[^\"]*\"([^\"]*)\")(?=[^\\[]*version[^\"]*\"([^\"]*)\")?", Pattern.DOTALL);
            var matcher = pattern.matcher(rd.getValue());
            if (!matcher.find())
                return null;

            loader = matcher.group(1);
            name = matcher.group(2);
            version = matcher.group(3);

            if (version != null && version.startsWith("$"))
                version = null;
            if (name == null || name.startsWith("$"))
                return null;
            type = rd.getOrder() == 1 ? LoaderType.FORGE : LoaderType.NEOFORGE;
        }
        else if (rd.getOrder() == 2){ // Fabric
            var read = GsonUtil.DEFAULT_GSON.fromJson(rd.getValue(), JsonObject.class);
            if (read == null || read.isEmpty())
                return null;

            if (read.has("version"))
                version = read.get("version").getAsString();
            if (!read.has("name"))
                return null;
            name = read.get("name").getAsString();

            if (read.has("depends")){
                var depends = read.get("depends").getAsJsonObject();
                if (depends.has("minecraft"))
                    versionId = depends.get("minecraft").getAsString().replace(".x", "");
                if (depends.has("fabricloader"))
                    loader = URLDecoder.decode(depends.get("fabricloader").getAsString(), StandardCharsets.UTF_8);
            }
            type = LoaderType.FABRIC;
        }

        if (type == null)
            return null;

        return new ModInfo(name, type, versionId, loader, version);
    }

    public void installResourcepacks(LaunchToken token, List<Resourcepack> rs) throws NoConnectionException, StopException {
        var path = token.getProfile().getPath().to("resourcepacks");
        int i = 0;
        int size = rs.size();
        for (var pack : rs){
            if (token.shouldStop())
                throw new StopException();

            handler.execute(new KeyEvent(String.format("%s %d/%d", pack.name, ++i, size)));

            if (pack.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install resourcepack '" + pack.name + "' because it has no file name.");
                continue;
            }
            var px = path.to(pack.fileName);
            if (!token.getRedownloadSettings().hasResourcePacks() && pack.checkFile(px, token.getFileCheckMode()))
                continue;

            if (pack.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install resourcepack '" + pack.name + "' because it has no file url.");
                continue;
            }

            try{
                Network.download(token.toNetworkToken(pack.fileUrl, px, false));
            }
            catch (HttpException | IOException e){
                Logger.getLogger().log("Error while installing resourcepack: " + pack.name, e);
            }
        }
    }

    public void installWorlds(LaunchToken token, List<World> ws) throws NoConnectionException, StopException {
        var worlds = token.getProfile().getPath().to("saves");

        int i = 0;
        int size = ws.size();

        for (var w : ws){
            if (token.shouldStop())
                throw new StopException();

            handler.execute(new KeyEvent(String.format("%s %d/%d", w.name, ++i, size)));

            if (w.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install world '" + w.name + "' because it has no file name.");
                continue;
            }
            if (worlds.to(w.name).exists())
                continue;

            if (w.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install world '" + w.name + "' because it has no file url.");
                continue;
            }

            try{
                Path zip = Network.download(token.toNetworkToken(w.fileUrl, worlds.to(w.fileName), false));

                var folder = zip.getZipMainFolder();

                if (folder == null){
                    w.name = w.fileName.split("\\.")[0];
                    zip.extract(worlds.to(w.name), null);
                }
                else{
                    zip.extract(worlds, null);
                    w.name = StrUtil.pure(folder);
                }

                zip.delete();
            }
            catch (HttpException | IOException e){
                Logger.getLogger().log("Error while installing world: " + w.name, e);
            }

            //World.fromGzip(w, worlds.to(w.name, "level.dat"));
        }

        Profiler.getProfiler().setProfile(token.getProfile().getName(), null);
    }

    public void installModpacks(LaunchToken token, List<Modpack> mps) throws NoConnectionException, StopException {
        var path = token.getProfile().getPath();

        for (var mp : mps){
            if (token.shouldStop())
                throw new StopException();

            handler.execute(new KeyEvent(RESOURCE_INSTALL + mp.name));
            try {
                mp.getSource().extractModpack(mp, path, false, token.getOnProgress(), token);
            } catch (HttpException | IOException e) {
                Logger.getLogger().log("Error while installing modpack: " + mp.name, e);
            }
            /*if (mp.isForge())
                CurseForge.getForge().extractModpack(path, mp);
            else if (mp.isModrinth())
                Modrinth.getModrinth().extractModpack(p.getPath(), mp);*/
        }
    }

    /**
     * Extracts the modpack and configures the profile.
     * <br/><br/>
     * <b>Profile have to be saved after this function.</b>
     */
    public int includeModpack(Profile p, Modpack mp, IncludeMode mode, ProgressFunction onProgress) throws NoConnectionException, HttpException, StopException, IOException {
        var path = p.getPath();

        var oldMp = p.getAllResources().stream().filter(a -> a.isSameResource(mp)).findFirst();
        oldMp.ifPresent(a -> remove(p, a));

        mp.getSource().applyModpack(mp, path, ModSource.Options.create(p).useProgressLogging(onProgress));

        boolean check = mode == IncludeMode.OVERWRITE_PROFILE || mode == IncludeMode.DEFAULT && checkModpackOverride(p, mp);

        UIEventBus.disable();

        int total = 0;

        total += include(p, mp.mods, IncludeMode.IGNORE_PROFILE, onProgress);
        total += include(p, mp.resources, IncludeMode.IGNORE_PROFILE, onProgress);
        total += include(p, mp.shaders, IncludeMode.IGNORE_PROFILE, onProgress);

        UIEventBus.enable();

        onProgress.onContext(BasicActionContext.STOP);

        if (mp.logoUrl != null && !mp.logoUrl.isEmpty() && p.getIcon() == null){
            ImageCacheManager.remove(p);
            p.setIcon(NetUtil.downloadImage(Configurator.getConfig().getImagePath(), mp.logoUrl).setUrl(mp.logoUrl));
        }

        if (check){
            p.setVersionId(mp.targetVersionId);
            p.setLoader(mp.wr);
            p.setLoaderVersion(mp.wrId);
        }
        p.getAllResources().add(mp);
        return total + 1;
    }
    private boolean checkModpackOverride(Profile profile, Modpack modpack) throws StopException {
        final var result = UI.runSync(() -> {
            if (modpack.targetVersionId != null && !modpack.targetVersionId.equals(profile.getVersionId())){
                var k = Main.getMain().showMsg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.sure"), Translator.translate("mods.ask.version"))
                        .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO, CMsgBox.ResultType.CANCEL)
                        .executeForResult();

                if (k.isEmpty() || k.get().result() == CMsgBox.ResultType.CANCEL)
                    return null;

                return k.get().result() == CMsgBox.ResultType.YES;
            } else if (!Configurator.getConfig().isAutoChangeLoader() && (modpack.wr.getType() != profile.getLoader().getType() || !modpack.wrId.equals(profile.getLoaderVersion()))){
                var k = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.sure"), Translator.translate("mods.ask.loader"))
                        .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO, CMsgBox.ResultType.CANCEL)
                        .executeForResult();

                if (k.isEmpty() || k.get().result() == CMsgBox.ResultType.CANCEL)
                    return null;

                return k.get().result() == CMsgBox.ResultType.YES;
            }

            return true;
        });
        if (result == null){
            handler.execute(new KeyEvent(EventHandler.STOP));
            throw new StopException();
        }

        return result;
    }

    public void installShaders(LaunchToken token, List<Shader> shs) throws NoConnectionException, StopException {
        var path = token.getProfile().getPath().to("shaderpacks");
        int i = 0;
        int size = shs.size();
        for (var shader : shs){
            if (token.shouldStop())
                throw new StopException();

            handler.execute(new KeyEvent(String.format("%s %d/%d", shader.name, ++i, size)));

            if (shader.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install shader '" + shader.name + "' because it has no file name.");
                continue;
            }
            var pxx = path.to(shader.fileName);
            if (!token.getRedownloadSettings().hasShaders() && shader.checkFile(pxx, token.getFileCheckMode()))
                continue;

            if (shader.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install shader '" + shader.name + "' because it has no file url.");
                continue;
            }

            try{
                Network.download(NetworkToken.create(shader.fileUrl, pxx, false).withLogging(token.getOnProgress()).syncWith(token));
            }
            catch (HttpException | IOException e){
                Logger.getLogger().log("Error while installing shader: " + shader.name, e);
            }

        }
    }

    /**
     * Includes the given resources into the profile.
     * Force include is false.
     * @param p target profile
     * @param resources resources to be included
     * @return count of included resources
     */
    public <T extends CResource> int include(Profile p, List<T> resources) throws NoConnectionException, HttpException, StopException{
        return include(p, resources, null);
    }

    /**
     * Includes the given resources into the profile.
     * Force include is false.
     * @param p target profile
     * @param resources resources to be included
     * @param onProgress progress function
     * @return count of included resources
     */
    public <T extends CResource> int include(Profile p, List<T> resources, ProgressFunction onProgress) throws NoConnectionException, HttpException, StopException{
        return include(p, resources, IncludeMode.DEFAULT, onProgress);
    }

    /**
     * Includes the given resources into the profile.
     * @param p target profile
     * @param resources resources to be included
     * @param mode include behavior
     * @return count of included resources
     */
    public <T extends CResource> int include(Profile p, List<T> resources, IncludeMode mode, ProgressFunction onProgress) throws NoConnectionException, HttpException, StopException {
        int count = 0;

        String newVersionId = null;
        LoaderType newLoaderType = null;
        boolean newPropertiesAreValid = false;
        boolean ignoreNewProperties = false;

        for (var r : resources){
            if (mode == IncludeMode.DEFAULT && (r.isMeta() || (p.getLoader().getType().isNative() && !r.getType().isGlobal())))
                continue;

            if (r.getType() == ResourceType.MODPACK){
                try {
                    count += includeModpack(p, (Modpack)r, mode, onProgress);
                } catch (IOException e) {
                    Logger.getLogger().logHyph("Error while including modpack: " + r.name);
                    Logger.getLogger().log(e);
                }
                continue;
            }

            if (r.getType() != ResourceType.WORLD){
                removeAll(p, List.of(r), true, false);
            }
            else if (p.getAllResources().stream().anyMatch(a -> a instanceof World w && w.equals(r)))
                continue;

            // if more than one resources are mismatching with the profile, only apply the first one
            if (
                    !newPropertiesAreValid &&
                    mode == IncludeMode.OVERWRITE_PROFILE &&
                    !r.isMeta() &&
                    !r.getType().isGlobal() &&
                    !ignoreNewProperties &&
                    (!p.getVersionId().equals(r.targetVersionId) || !p.getLoader().getType().getIdentifier().equals(r.targetLoader))
            ) {
                newVersionId = r.targetVersionId;
                newLoaderType = LoaderType.fromIdentifier(r.targetLoader);

                newPropertiesAreValid = true;
            }

            count++;
            p.getAllResources().add(r);
        }

        if (newPropertiesAreValid){
            p.setVersionId(newVersionId);
            var loader = Loader.getLoader(newLoaderType.getIdentifier());
            p.setLoader(loader);
            if (!newLoaderType.isNative()){
                String loaderVersion = null;
                var versions = ((Loader<LoaderVersion>)loader).getVersions(newVersionId);
                if (versions != null && !versions.isEmpty())
                    loaderVersion = versions.get(0).getLoaderVersion();

                if (loaderVersion == null){
                    throw new RuntimeException("There are no versions found for loader with identifier '" + newLoaderType + "'");
                }

                p.setLoaderVersion(loaderVersion);
            }
        }

        Profiler.getProfiler().setProfile(p.getName(), null);

        return count;
    }

    public void removeAll(Profile profile, List<CResource> resources){
        removeAll(profile, resources, false, true);
    }

    public void remove(Profile profile, CResource resource){
        removeAll(profile, List.of(resource));
    }

    public void removeAll(Profile profile, List<CResource> resources, boolean useExistingResource, boolean triggerSet){
        var path = profile.getPath();
        for (var r : resources){
            CResource finalR;

            if (r instanceof Modpack mp){
                if (useExistingResource){
                    var found = profile.getAllResources().stream().filter(a -> a.isSameResource(r)).findFirst();
                    if (found.isEmpty())
                        return;
                    else
                        mp = (Modpack) found.get();
                }
                finalR = mp;

                for (var res : profile.getAllResources().stream().filter(x -> x instanceof ModpackContent mpc && mpc.belongs((Modpack) finalR)).toList()){
                    if (res.fileName == null)
                        continue;

                    path.to(res.getType().getStoringFolder(), res.fileName).delete();
                }
                var manifest = path.to("manifest-" + finalR.name + ".json");
                manifest.delete();
            }
            else{
                var res = r;

                if (useExistingResource){
                    var found = profile.getAllResources().stream().filter(a -> a.isSameResource(r)).findFirst();
                    if (found.isEmpty())
                        return;
                    else
                        res = found.get();
                }

                finalR = res;

                if (finalR.getType() == ResourceType.WORLD) {
                    //path.to(finalR.getType().getStoringFolder(), finalR.name).delete();
                } else if (finalR.fileName != null)
                    path.to(finalR.getType().getStoringFolder(), finalR.fileName).delete();
            }

            profile.removeResource(finalR);
        }

        if (triggerSet)
            Profiler.getProfiler().setProfile(profile.getName(), null);
    }
}
