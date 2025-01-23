package com.laeben.corelauncher.minecraft.modding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.corelauncher.api.ui.UI;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Modder {

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

    public List<Modpack> getModpackUpdates(Profile p, List<Modpack> resources) throws NoConnectionException, HttpException {
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

    public Map<Object, List<CResource>> getUpdates(Profile p, List<CResource> resources) throws NoConnectionException, HttpException {
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

    public List<CResource> getUpdate(Profile p, CResource m) throws NoConnectionException, HttpException {
        if (m instanceof ModpackContent mc && mc.getModpackId() != null)
            return null;

        var opt = ModSource.Options.create(p);
        if (!(m instanceof Modpack))
            opt.dependencies(true);

        return m.getSource().getCoreResource(m.getId(), opt);
    }

    public CResource fill(Profile p, CResource m) throws NoConnectionException, HttpException {
        if (m.id == null)
            return m;

        var all = m.getSource().getCoreResource(m.getId(), ModSource.Options.create(p));

        return all == null ? m : all.get(0);
    }

    public void installMods(Profile p, List<Mod> mods) throws NoConnectionException, StopException, HttpException, FileNotFoundException {
        var path = p.getPath().to("mods");
        int i = 0;
        int size = mods.size();
        for (var a : mods.stream().toList()){
            handler.execute(new KeyEvent("," + a.name + ":.resource.progress;" + (++i) + ";" + size));

            if (a.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install mod '" + a.name + "' because it has no file name.");
                continue;
            }

            var pxx = path.to(a.fileName);
            if (pxx.exists())
                continue;

            if (a.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install mod '" + a.name + "' because it has no file url.");
                continue;
            }

            String url = a.fileUrl;
            if (url.startsWith("OptiFine")){
                var f = OptiFine.getOptiFine().getVersion(p.getVersionId(), url);
                if (f == null){
                    Logger.getLogger().logDebug("ERR");
                    continue;
                }
                OptiFine.installForge(f, path);
                continue;
            }

            if (NetUtil.download(url, path.to(a.fileName), false) != null)
                continue;

            var up = fill(p, a);
            if (url.equals(up.fileUrl))
                continue;

            UI.runAsync(() -> {
                remove(p, a);
                try {
                    include(p, up);
                } catch (NoConnectionException | HttpException e) {
                    Logger.getLogger().log(e);
                }
                catch (StopException ignored){

                }
            });

            try{
                if (NetUtil.download(up.fileUrl, path.to(up.fileName), false) == null){
                    var x = Modrinth.getModrinth().getProjectVersions(a.name, p.getVersionId(), p.getWrapper().getType());
                    var s = x.stream().flatMap(f -> f.getFiles().stream()).filter(f -> f.filename != null && f.filename.equals(a.fileName)).findFirst();

                    if (s.isEmpty()){
                        Logger.getLogger().logDebug("ERR");
                        continue;
                    }
                    NetUtil.download(s.get().url, path.to(a.fileName), false);
                }
            }
            catch (HttpException e){
                Logger.getLogger().log( LogType.ERROR, "Error while installing mod: " + up.name);
            }
        }
    }
    public void includeMods(Profile p, List<Mod> mods){
        for (var m : mods){
            remove(p, m, true, false);
            p.getMods().add(m);
        }
        Profiler.getProfiler().setProfile(p.getName(), null);
    }

    public ModInfo getModFromJarFile(Path p){
        LoaderType type = null;
        String versionId = null;
        String name = null;
        String version = null;
        String loader = null;

        if (!p.getExtension().equals("jar"))
            return null;

        var rd = p.tryReadZipEntry("mcmod.info", "META-INF/mods.toml", "fabric.mod.json");

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
        else if (rd.getOrder() == 1){ // Minecraft Forge > 1.12.2
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
            type = LoaderType.FORGE;
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

    public void includeResourcepacks(Profile p, List<Resourcepack> r){
        for (var pack : r){
            remove(p, pack, true, false);
            p.getResources().add(pack);
        }

        Profiler.getProfiler().setProfile(p.getName(), null);
    }
    public void installResourcepacks(Profile p, List<Resourcepack> rs) throws NoConnectionException, HttpException, StopException {
        var path = p.getPath().to("resourcepacks");
        int i = 0;
        int size = rs.size();
        for (var pack : rs){
            handler.execute(new KeyEvent("," + pack.name + ":.resource.progress;" + (++i) + ";" + size));

            if (pack.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install resourcepack '" + pack.name + "' because it has no file name.");
                continue;
            }
            var px = path.to(pack.fileName);
            if (px.exists())
                continue;

            if (pack.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install resourcepack '" + pack.name + "' because it has no file url.");
                continue;
            }

            try{
                NetUtil.download(pack.fileUrl, px, false);
            }
            catch (HttpException e){
                Logger.getLogger().log( LogType.ERROR, "Error while installing resourcepack: " + pack.name);
            }
        }
    }

    public void installWorlds(Profile p, List<World> ws) throws NoConnectionException, StopException {
        var worlds = p.getPath().to("saves");

        int i = 0;
        int size = ws.size();

        for (var w : ws){
            handler.execute(new KeyEvent("," + w.name + ":.resource.progress;" + (++i) + ";" + size));

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

            Path zip = null;
            try{
                zip = NetUtil.download(w.fileUrl, worlds.to(w.fileName), false);
            }
            catch (HttpException e){
                Logger.getLogger().log( LogType.ERROR, "Error while installing world: " + w.name);
            }
            if (zip == null)
                continue;

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

            //World.fromGzip(w, worlds.to(w.name, "level.dat"));
        }

        Profiler.getProfiler().setProfile(p.getName(), null);
    }
    public void includeWorld(Profile p, World w){
        if (p.getOnlineWorlds().stream().anyMatch(x -> x.equals(w)))
            return;

        Profiler.getProfiler().setProfile(p.getName(), a -> a.getOnlineWorlds().add(w));
    }

    public void installModpacks(Profile p, List<Modpack> mps) throws NoConnectionException, HttpException, StopException {
        var path = p.getPath();

        for (var mp : mps){
            handler.execute(new KeyEvent(".resource.install;" + mp.name));
            mp.getSource().extractModpack(mp, path, false);
            /*if (mp.isForge())
                CurseForge.getForge().extractModpack(path, mp);
            else if (mp.isModrinth())
                Modrinth.getModrinth().extractModpack(p.getPath(), mp);*/
        }
    }
    public void includeModpack(Profile p, Modpack mp) throws NoConnectionException, HttpException, StopException {
        var path = p.getPath();

        var oldMp = p.getModpacks().stream().filter(a -> a.isSameResource(mp)).findFirst();
        oldMp.ifPresent(a -> remove(p, a));

        mp.getSource().applyModpack(mp, path, ModSource.Options.create(p));

        boolean check = checkModpackOverride(p, mp);

        EventHandler.disable();

        includeMods(p, mp.mods);
        includeResourcepacks(p, mp.resources);
        includeShaders(p, mp.shaders);

        EventHandler.enable();

        handler.execute(new KeyEvent(EventHandler.STOP));

        Profiler.getProfiler().setProfile(p.getName(), pxt -> {
            if (mp.logoUrl != null && !mp.logoUrl.isEmpty() && pxt.getIcon() == null){
                ImageCacheManager.remove(pxt);
                pxt.setIcon(NetUtil.downloadImage(Configurator.getConfig().getImagePath(), mp.logoUrl).setUrl(mp.logoUrl));
            }

            if (check){
                pxt.setVersionId(mp.targetVersionId);
                pxt.setWrapper(mp.wr);
                pxt.setWrapperVersion(mp.wrId);
            }
            pxt.getModpacks().add(mp);
        });
    }
    private boolean checkModpackOverride(Profile profile, Modpack modpack) throws StopException {
        var task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                if (modpack.targetVersionId != null && !modpack.targetVersionId.equals(profile.getVersionId())){
                    var k = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.sure"), Translator.translate("mods.ask.version"))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO, CMsgBox.ResultType.CANCEL)
                            .executeForResult();

                    if (k.isEmpty() || k.get().result() == CMsgBox.ResultType.CANCEL)
                        throw new StopException();

                    return k.get().result() == CMsgBox.ResultType.YES;
                } else if (!Configurator.getConfig().isAutoChangeWrapper() && (modpack.wr.getType() != profile.getWrapper().getType() || !modpack.wrId.equals(profile.getWrapperVersion()))){
                    var k = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.sure"), Translator.translate("mods.ask.wrapper"))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO, CMsgBox.ResultType.CANCEL)
                            .executeForResult();

                    if (k.isEmpty() || k.get().result() == CMsgBox.ResultType.CANCEL)
                        throw new StopException();

                    return k.get().result() == CMsgBox.ResultType.YES;
                }

                return true;
            }
        };

        UI.runAsync(task);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            handler.execute(new KeyEvent(EventHandler.STOP));
            throw new StopException();
        }
    }


    public void installShaders(Profile p, List<Shader> shs) throws NoConnectionException, FileNotFoundException, HttpException, StopException {
        var path = p.getPath().to("shaderpacks");
        int i = 0;
        int size = shs.size();
        for (var shader : shs){
            handler.execute(new KeyEvent("," + shader.name + ":.resource.progress;" + (++i) + ";" + size));

            if (shader.fileName == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install shader '" + shader.name + "' because it has no file name.");
                continue;
            }
            var pxx = path.to(shader.fileName);
            if (pxx.exists())
                continue;

            if (shader.fileUrl == null){
                Logger.getLogger().log(LogType.ERROR, "Cannot install shader '" + shader.name + "' because it has no file url.");
                continue;
            }

            try{
                NetUtil.download(shader.fileUrl, pxx, false, true);
            }
            catch (HttpException e){
                Logger.getLogger().log( LogType.ERROR, "Error while installing shader: " + shader.name);
            }

        }
    }
    public void includeShaders(Profile p, List<Shader> s){
        for (var pack : s){
            remove(p, pack, true, false);
            p.getShaders().add(pack);
        }

        Profiler.getProfiler().setProfile(p.getName(), null);
    }

    public void include(Profile p, CResource r) throws NoConnectionException, HttpException, StopException {
        if (r.isMeta() || (p.getWrapper().getType().isNative() && !r.getType().isGlobal()))
            return;

        if (r instanceof Mod m)
            includeMods(p, List.of(m));
        else if (r instanceof Modpack mp)
            includeModpack(p, mp);
        else if (r instanceof Resourcepack rs)
            includeResourcepacks(p, List.of(rs));
        else if (r instanceof World w)
            includeWorld(p, w);
        else if (r instanceof Shader s)
            includeShaders(p, List.of(s));
    }
    public void includeAll(Profile p, List<CResource> rs) throws NoConnectionException, HttpException, StopException {
        for (var r : rs)
            include(p, r);
    }

    public void remove(Profile profile, CResource r){
        remove(profile, r, false, true);
    }
    public void remove(Profile profile, CResource r, boolean useExistingResource, boolean triggerSet){
        var path = profile.getPath();
        var modsPath = path.to("mods");
        //var savesPath = path.to("saves");
        var resourcesPath = path.to("resourcepacks");
        var shadersPath = path.to("shaderpacks");

        CResource finalR;

        if (r instanceof Mod m){
            if (useExistingResource){
                var found = profile.getMods().stream().filter(a -> a.isSameResource(r)).findFirst();
                if (found.isEmpty())
                    return;
                else
                    m = found.get();
            }
            finalR = m;

            if (m.fileName != null){
                var pth = modsPath.to(m.fileName);
                if (pth.exists())
                    pth.delete();
            }
        }
        else if (r instanceof Modpack mp){
            if (useExistingResource){
                var found = profile.getModpacks().stream().filter(a -> a.isSameResource(r)).findFirst();
                if (found.isEmpty())
                    return;
                else
                    mp = found.get();
            }
            finalR = mp;

            var mods = profile.getMods().stream().filter(x -> x.belongs((Modpack) finalR)).toList();
            var packs = profile.getResources().stream().filter(x -> x.belongs((Modpack) finalR)).toList();
            var shaders = profile.getShaders().stream().filter(x -> x.belongs((Modpack) finalR)).toList();
            for (var md : mods){
                if (md.fileName == null)
                    continue;
                var pth = modsPath.to(md.fileName);
                if (pth.exists())
                    pth.delete();
            }
            for (var pk : packs){
                if (pk.fileName == null)
                    continue;
                var pth = resourcesPath.to(pk.fileName);
                if (pth.exists())
                    pth.delete();
            }
            for (var s : shaders){
                if (s.fileName == null)
                    continue;
                var pth = shadersPath.to(s.fileName);
                if (pth.exists())
                    pth.delete();
            }
            var manifest = path.to("manifest-" + finalR.name + ".json");
            manifest.delete();
        }
        else if (r instanceof Resourcepack p){
            if (useExistingResource){
                var found = profile.getResources().stream().filter(a -> a.isSameResource(r)).findFirst();
                if (found.isEmpty())
                    return;
                else
                    p = found.get();
            }
            finalR = p;

            if (finalR.fileName != null){
                var pth = resourcesPath.to(finalR.fileName);
                if (pth.exists())
                    pth.delete();
            }
        }
        else if (r instanceof World w){
            //savesPath.to(w.name).delete();
            finalR = w;
        }
        else if (r instanceof Shader s){
            if (useExistingResource){
                var found = profile.getShaders().stream().filter(a -> a.isSameResource(r)).findFirst();
                if (found.isEmpty())
                    return;
                else
                    s = found.get();
            }
            finalR = s;

            shadersPath.to(finalR.name).delete();
        }
        else
            finalR = r;

        if (triggerSet)
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.removeSource(finalR));
        else
            profile.removeSource(finalR);
    }
}
