package com.laeben.corelauncher.minecraft.modding;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ForgeModpack;
import com.laeben.corelauncher.minecraft.modding.entities.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.DependencyInfo;
import com.laeben.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.NetUtils;

import java.io.FileNotFoundException;
import java.util.List;

public class Modder {

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

    public void installMods(Profile p, List<Mod> mods) throws NoConnectionException, StopException, HttpException, FileNotFoundException {
        var path = p.getPath().to("mods");
        int i = 0;
        int size = mods.size();
        for (var a : mods){
            if (a.fileUrl == null)
                continue;

            var pxx = path.to(a.fileName);
            if (pxx.exists())
                continue;

            handler.execute(new KeyEvent("," + a.name + ":.resource.progress;" + (++i) + ";" + size));

            String url = a.fileUrl;
            if (url.startsWith("OptiFine")){
                var f = OptiFine.getOptiFine().getVersion(p.getVersionId(), url);
                if (f == null){
                    System.out.println("ERR");
                    continue;
                }
                OptiFine.installForge(f, path);
                continue;
            }


            if (NetUtils.download(url, path.to(a.fileName), false) == null){
                var x = Modrinth.getModrinth().getProjectVersions(a.name, p.getVersionId(), p.getWrapper().getIdentifier(), DependencyInfo.noDependencies());
                var s = x.stream().flatMap(f -> f.getFiles().stream()).filter(f -> f.filename != null && f.filename.equals(a.fileName)).findFirst();

                if (s.isEmpty()){
                    System.out.println("ERR");
                    continue;
                }
                NetUtils.download(s.get().url, path.to(a.fileName), false);
            }
        }
    }
    public void includeMods(Profile p, List<Mod> mods){
        for (var m : mods){
            if (p.getMods().stream().anyMatch(x -> x.equals(m)))
                continue;
            p.getMods().add(m);
        }
        Profiler.getProfiler().setProfile(p.getName(), null);
    }

    public void includeResourcepacks(Profile p, List<Resourcepack> r){
        for (var pack : r){
            if (p.getResources().stream().anyMatch(s -> s.equals(pack)))
                continue;

            p.getResources().add(pack);
        }

        Profiler.getProfiler().setProfile(p.getName(), null);
    }
    public void installResourcepacks(Profile p, List<Resourcepack> rs) throws NoConnectionException, HttpException {
        var path = p.getPath().to("resourcepacks");
        int i = 0;
        int size = rs.size();
        for (var pack : rs){
            var px = path.to(pack.fileName);
            if (px.exists())
                continue;

            handler.execute(new KeyEvent("," + pack.name + ":.resource.progress;" + (++i) + ";" + size));

            NetUtils.download(pack.fileUrl, px, false);
        }
    }

    public void installWorlds(Profile p, List<World> ws) throws NoConnectionException, HttpException {
        var worlds = p.getPath().to("saves");

        int i = 0;
        int size = ws.size();

        for (var w : ws){
            if (worlds.to(w.name).exists())
                continue;

            if (w.fileUrl == null)
                continue;

            handler.execute(new KeyEvent("," + w.name + ":.resource.progress;" + (++i) + ";" + size));

            var zip = NetUtils.download(w.fileUrl, worlds.to(w.fileName), false);
            if (zip == null)
                continue;
            zip.extract(worlds, null);
            zip.delete();

            World.fromGzip(w, worlds.to(w.name, "level.dat"));
        }

        Profiler.getProfiler().setProfile(p.getName(), null);
    }
    public void includeWorld(Profile p, World w){
        if (p.getOnlineWorlds().stream().anyMatch(x -> x.equals(w)))
            return;

        Profiler.getProfiler().setProfile(p.getName(), a -> a.getOnlineWorlds().add(w));
    }

    public void installModpacks(Profile p, List<Modpack> mps) throws NoConnectionException, HttpException {
        var path = p.getPath();
        for (var mp : mps){
            if (mp.isForge())
                CurseForge.getForge().extractModpack(path, new ForgeModpack(mp));
            else if (mp.isModrinth())
                Modrinth.getModrinth().extractModpack(p.getPath(), mp);
        }
    }
    public void includeModpack(Profile p, Modpack mp) throws NoConnectionException, HttpException {
        var path = p.getPath();
        String vId = p.getVersionId();

        if (p.getModpacks().stream().anyMatch(x -> x.equals(mp)))
            return;

        if (mp.isForge())
            CurseForge.getForge().applyModpack(vId, path, mp);
        else if (mp.isModrinth())
            Modrinth.getModrinth().applyModpack(mp, path, p.getWrapper());

        includeMods(p, mp.mods);
        includeResourcepacks(p, mp.resources);

        Profiler.getProfiler().setProfile(p.getName(), pxt -> {
            pxt.setWrapper(mp.wr);
            pxt.setWrapperVersion(mp.wrId);
            pxt.getModpacks().add(mp);
        });
    }

    public void installShaders(Profile p, List<Shader> shs) throws NoConnectionException, FileNotFoundException, HttpException, StopException {
        var path = p.getPath().to("shaderpacks");
        int i = 0;
        int size = shs.size();
        for (var shader : shs){
            if (shader.fileUrl == null)
                continue;
            var pxx = path.to(shader.fileName);
            if (pxx.exists())
                continue;

            handler.execute(new KeyEvent("," + shader.name + ":.resource.progress;" + (++i) + ";" + size));

            NetUtils.download(shader.fileUrl, pxx, false, true);
        }
    }
    public void includeShader(Profile p, Shader s){
        if (p.getShaders().stream().anyMatch(x -> x.equals(s)))
            return;

        Profiler.getProfiler().setProfile(p.getName(), a -> a.getShaders().add(s));
    }

    public void include(Profile p, CResource r) throws NoConnectionException, HttpException {
        if (r instanceof Mod m)
            includeMods(p, List.of(m));
        else if (r instanceof Modpack mp)
            includeModpack(p, mp);
        else if (r instanceof Resourcepack rs)
            includeResourcepacks(p, List.of(rs));
        else if (r instanceof World w)
            includeWorld(p, w);
        else if (r instanceof Shader s)
            includeShader(p, s);
    }
    public void includeAll(Profile p, List<CResource> rs) throws NoConnectionException, HttpException {
        for (var r : rs)
            include(p, r);
    }
    public void remove(Profile profile, CResource r){
        var path = profile.getPath();
        var modsPath = path.to("mods");
        var savesPath = path.to("saves");
        var resourcesPath = path.to("resourcepacks");
        var shadersPath = path.to("shaderpacks");

        if (r instanceof Mod m){
            if (m.fileName != null){
                var pth = modsPath.to(m.fileName);
                if (pth.exists())
                    pth.delete();
            }
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getMods().removeIf(s -> s.equals(m)));
        }
        else if (r instanceof Modpack mp){
            var mods = profile.getMods().stream().filter(x -> x.belongs(mp)).toList();
            var packs = profile.getResources().stream().filter(x -> x.belongs(mp)).toList();
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
            var manifest = path.to("manifest-" + mp.name + ".json");
            manifest.delete();
            Profiler.getProfiler().setProfile(profile.getName(), a -> {
                a.getMods().removeIf(x -> x.belongs(mp));
                a.getResources().removeIf(x -> x.belongs(mp));
                a.getModpacks().removeIf(x -> x.equals(mp));
            });
        }
        else if (r instanceof Resourcepack p){
            if (p.fileName != null){
                var pth = resourcesPath.to(p.fileName);
                if (pth.exists())
                    pth.delete();
            }
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getResources().removeIf(x -> x.equals(p)));
        }
        else if (r instanceof World w){
            savesPath.to(w.name).delete();
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getOnlineWorlds().removeIf(x -> x.equals(w)));
        }
        else if (r instanceof Shader s){
            shadersPath.to(s.name).delete();
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getShaders().removeIf(x -> x.equals(s)));
        }
    }
}
