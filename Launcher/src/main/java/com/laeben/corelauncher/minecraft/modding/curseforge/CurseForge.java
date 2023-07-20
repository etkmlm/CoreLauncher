package com.laeben.corelauncher.minecraft.modding.curseforge;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.core.util.RequesterFactory;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.*;
import com.laeben.corelauncher.minecraft.modding.entities.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.core.entity.Path;
import com.google.gson.*;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CurseForge {

    private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.curseforge.com";
    private static final int GAME_ID = 432;
    private static CurseForge instance;

    private final Gson gson;

    private List<Category> categories;
    private final RequesterFactory factory;
    private final EventHandler<BaseEvent> handler;


    public CurseForge(){
        gson = GsonUtils.empty();

        factory = new RequesterFactory(BASE_URL);
        handler = new EventHandler<>();

        instance = this;
    }

    public static CurseForge getForge(){
        return instance;
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }

    public SearchResponse search(Search s){
        String a = get("/v1/mods/search", RequestParameter.classToParams(s, Search.class));
        return gson.fromJson(a, SearchResponse.class);
    }

    public Modpack getModpack(String vId, String loader, int id){
        return Modpack.fromResource(vId, loader, getResource(id));
    }

    public World getWorld(String vId, String loader, int id){
        return World.fromResource(vId, loader, getResource(id));
    }
    public Resourcepack getResourcepack(String vId, int id){
        return Resourcepack.fromResource(vId, null, getResource(id));
    }
    public Mod getMod(String vId, String loader, int id){
        return Mod.fromResource(vId, loader, getResource(id));
    }

    public Resource getResource(int id){
        String a = get("/v1/mods/" + id, null);
        var f = gson.fromJson(a, JsonObject.class);
        return gson.fromJson(f.get("data"), Resource.class);
    }

    public List<CResource> getResources(String vId, List<Integer> ids, Profile p, boolean useFullResource){
        if (ids.size() == 0)
            return List.of();
        var request = new ModsRequest();
        request.modIds = ids.toArray(Integer[]::new);
        String json = gson.toJson(request);
        var f = post("/v1/mods", json);
        if (f == null)
            return List.of();
        var data = gson.fromJson(f, JsonObject.class).get("data").getAsJsonArray();

        var type = p == null ? null : p.getWrapper().getType();
        var iden = p == null ? null : p.getWrapper().getIdentifier();

        return data.asList().stream().map(x -> {
            var res = gson.fromJson(x, Resource.class);
            if (type != null && useFullResource)
                getFullResource(vId, type, res);
            return (CResource) CResource.fromResourceGeneric(vId, iden, res);
        }).toList();
    }
    public List<CResource> getDependencies(List<Mod> mods, Profile p){
        var mds = new ArrayList<CResource>();
        String vId = p.getVersionId();

        for (var m : mods){
            if (m.fileUrl == null)
                continue;
            mds.add(m);

            var ms = getResources(vId, m.dependencies.stream().map(x -> x.modId).toList(), p, true);
            mds.addAll(ms);
            mds.addAll(getDependencies(ms.stream().filter(x -> x instanceof Mod).map(x -> (Mod)x).toList(), p));
        }

        return mds;
    }

    public void include(Profile p, CResource r) throws NoConnectionException, HttpException {
        //var r = CResource.fromResourceGeneric(p.getVersionId(), p.getWrapper().getIdentifier(), res);
        if (r instanceof Mod m)
            includeMods(p, List.of(m), true);
        else if (r instanceof Modpack mp)
            includeModpack(p, mp);
        else if (r instanceof Resourcepack rp)
            includeResourcepacks(p, List.of(rp));
        else if (r instanceof World w)
            includeWorld(p, w);
    }

    public Resource getFullResource(String vId, CurseWrapper.Type type, Resource r){

        var params = new ArrayList<RequestParameter>();
        params.add(new RequestParameter("gameVersion", vId));
        if (type != CurseWrapper.Type.ANY)
            params.add(new RequestParameter("modLoaderType", type.ordinal()));

        String g = get("/v1/mods/" + r.id + "/files", params);
        var rs = gson.fromJson(g, JsonObject.class);
        r.latestFiles = rs.get("data").getAsJsonArray().asList().stream().map(x -> gson.fromJson(x, File.class)).toList();
        return r;
    }

    public void remove(Profile profile, CResource r){
        var path = profile.getPath();
        var modsPath = path.to("mods");
        var savesPath = path.to("saves");
        var resourcesPath = path.to("resourcepacks");

        if (r instanceof Mod m){
            if (m.fileName != null){
                var pth = modsPath.to(m.fileName);
                if (pth.exists())
                    pth.delete();
            }
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getMods().removeIf(s -> s.id == m.id));
        }
        else if (r instanceof Modpack mp){
            var mods = profile.getMods().stream().filter(x -> x.mpId == mp.id).toList();
            var packs = profile.getResources().stream().filter(x -> x.mpId == mp.id).toList();
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
                a.getMods().removeIf(x -> x.mpId == mp.id);
                a.getResources().removeIf(x -> x.mpId == mp.id);
                a.getModpacks().removeIf(x -> x.id == mp.id);
            });
        }
        else if (r instanceof Resourcepack p){
            if (p.fileName != null){
                var pth = resourcesPath.to(p.fileName);
                if (pth.exists())
                    pth.delete();
            }
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getResources().removeIf(x -> x.id == p.id));
        }
        else if (r instanceof World w){
            savesPath.to(w.name).delete();
            Profiler.getProfiler().setProfile(profile.getName(), a -> a.getOnlineWorlds().removeIf(x -> x.id == w.id));
        }
    }

    public List<File> getFiles(List<Integer> ids){
        var obj = new JsonObject();
        var arr = new JsonArray();
        arr.asList().addAll(ids.stream().map(JsonPrimitive::new).toList());
        obj.add("fileIds", arr);
        String pst = gson.toJson(obj);
        String ans = post("/v1/mods/files", pst);
        var abc = gson.fromJson(ans, JsonObject.class).get("data").getAsJsonArray().asList().stream().map(x -> gson.fromJson(x, File.class));
        return abc.distinct().toList();
    }

    public void includeModpack(Profile p, Modpack mp) throws NoConnectionException, HttpException {
        var path = p.getPath();
        String vId = p.getVersionId();

        if (p.getModpacks().stream().anyMatch(x -> x.id == mp.id))
            return;

        var manifest = gson.fromJson(extractModpack(path, mp).read(), Manifest.class);
        mp.applyManifest(manifest);

        var resources = getResources(vId, mp.getProjectIds(), null, false);
        mp.applyResources(resources, getFiles(mp.getFileIds()));

        includeMods(p, resources.stream().filter(x -> x instanceof Mod).map(x -> (Mod)x).toList(), false);
        includeResourcepacks(p, resources.stream().filter(x -> x instanceof Resourcepack).map(x -> (Resourcepack)x).toList());

        Profiler.getProfiler().setProfile(p.getName(), pxt -> {
            pxt.setWrapper(mp.wr);
            pxt.setWrapperVersion(mp.wrId);
            pxt.getModpacks().add(mp);
        });
    }

    public void includeWorld(Profile p, World w){
        if (p.getOnlineWorlds().stream().anyMatch(x -> x.id == w.id))
            return;

        Profiler.getProfiler().setProfile(p.getName(), a -> {
           a.getOnlineWorlds().add(w);
        });
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

            var zip = download(w.fileUrl, worlds.to(w.fileName), false);
            if (zip == null)
                continue;
            zip.extract(worlds, null);
            zip.delete();

            World.fromGzip(w, worlds.to(w.name, "level.dat"));
        }

        Profiler.getProfiler().setProfile(p.getName(), null);
    }

    public void installModpacks(Profile p, List<Modpack> mps) throws NoConnectionException, HttpException {
        for (var mp : mps)
            extractModpack(p.getPath(), mp);
    }
    private Path extractModpack(Path path, Modpack mp) throws NoConnectionException, HttpException {
        var zip = path.to("mpInfo.zip");
        String name = StringUtils.pure(mp.name);
        var tempDir = path.to(name);
        var manifest = path.to("manifest-" + name + ".json");
        if (!manifest.exists()){
            var ppp = download(mp.fileUrl, zip, false);
            handler.execute(new KeyEvent("stop"));
            zip.extract(tempDir, null);
            ppp.delete();
            tempDir.to("manifest.json").move(manifest);
            tempDir.to("overrides").move(path);
            tempDir.delete();
        }

        return manifest;
    }

    public void includeResourcepacks(Profile p, List<Resourcepack> r){
        for (var pack : r){
            if (p.getResources().stream().anyMatch(s -> s.id == pack.id))
                return;

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
                return;

            handler.execute(new KeyEvent("," + pack.name + ":.resource.progress;" + (++i) + ";" + size));

            download(pack.fileUrl, px, false);
        }
    }

    public void includeMods(Profile p, List<Mod> mods, boolean includeDependencies){
        var dependencies = includeDependencies ? getDependencies(mods, p) : mods;
        for (var d : dependencies){
            if (p.getMods().stream().anyMatch(x -> x.id == d.id))
                continue;
            if (d instanceof Resourcepack rp)
                p.getResources().add(rp);
            else if (d instanceof Mod m)
                p.getMods().add(m);
        }
        Profiler.getProfiler().setProfile(p.getName(), null);
    }
    public void installMods(Profile p, List<Mod> mods) throws NoConnectionException, StopException, HttpException {
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


            if (download(url, path.to(a.fileName), false) == null){
                String x = Modrinth.getModrinth().getMod(a.name, a.fileName);
                if (x == null){
                    System.out.println("ERR");
                    continue;
                }
                download(x, path.to(a.fileName), false);
            }
        }
    }

    private Path download(String url, Path path, boolean uon) throws NoConnectionException, HttpException {
        try{
            return NetUtils.download(url, path, uon, true);
        }
        catch (StopException ignored){

        }
        catch (RuntimeException ex){
            if (ex.getMessage().equals("fo")){
                return null;
            }
        }

        return path;
    }
    private String get(String api, List<RequestParameter> params){
        try{
            var r = factory.create().to(api)
                    .withHeader(new RequestParameter("x-api-key", API_KEY))
                    .withParam(new RequestParameter("gameId", GAME_ID));
            if (params != null)
                r.withParams(params);
            return r.getString();
            //return NetUtils.urlToString(BASE_URL + api, "x-api-key=" + API_KEY);
        }
        catch (NoConnectionException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
        return null;
    }
    private String post(String api, String body){
        try{
            var r = factory.create().to(api)
                    .withHeader(new RequestParameter("x-api-key", API_KEY))
                    .withHeader(RequestParameter.contentType("application/json"));
            return r.post(body);
            //return NetUtils.urlToString(BASE_URL + api, "x-api-key=" + API_KEY);
        }
        catch (NoConnectionException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
        return null;
    }

    public void reload(){
        String get = get("/v1/categories", null);
        if (get == null){
            categories = new ArrayList<>();
            return;
        }

        var data = gson.fromJson(get, JsonObject.class).get("data").getAsJsonArray();

        categories = data.asList().stream().map(x -> gson.fromJson(x, Category.class)).toList();

    }

    public List<Category> getCategories(){
        return categories;
    }

}
