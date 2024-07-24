package com.laeben.corelauncher.minecraft.modding.curseforge;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.util.RequesterFactory;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.core.entity.Path;
import com.google.gson.*;
import com.laeben.corelauncher.ui.controller.browser.ForgeSearch;
import com.laeben.corelauncher.ui.controller.browser.Search;
import com.laeben.corelauncher.util.GsonUtil;

import java.util.ArrayList;
import java.util.List;

public class CurseForge implements ModSource {
    private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.curseforge.com";
    private static final int GAME_ID = 432;
    private static CurseForge instance;

    private final Gson gson;

    private List<ForgeCategory> categories;
    private final RequesterFactory factory;


    public CurseForge(){
        gson = GsonUtil.empty();

        factory = new RequesterFactory(BASE_URL);

        instance = this;
    }

    public static CurseForge getForge(){
        return instance;
    }

    public SearchResponseForge search(SearchForge s) throws NoConnectionException, HttpException {
        String a = get("/v1/mods/search", RequestParameter.classToParams(s, SearchForge.class));
        return gson.fromJson(a, SearchResponseForge.class);
    }

    public List<ResourceForge> getResources(List<Object> ids) throws NoConnectionException {
        if (ids.isEmpty())
            return null;
        var request = new ModsRequest();
        request.modIds = ids.toArray(Integer[]::new);
        String json = gson.toJson(request);
        var f = post("/v1/mods", json);
        if (f == null)
            return null;
        var data = gson.fromJson(f, JsonObject.class).get("data").getAsJsonArray();

        return data.asList().stream().map(a -> gson.fromJson(a, ResourceForge.class)).toList();
    }
    public ResourceForge getFullResource(ResourceForge r, Options opt) throws NoConnectionException, HttpException {
        var params = new ArrayList<RequestParameter>();
        params.add(new RequestParameter("gameVersion", opt.getVersionId()));
        if (opt.hasLoaderType())
            params.add(new RequestParameter("modLoaderType", opt.getLoaderType().getIdentifier()));

        String g = get("/v1/mods/" + r.id + "/files", params);
        var rs = gson.fromJson(g, JsonObject.class);
        r.latestFiles = rs.get("data").getAsJsonArray().asList().stream().map(x -> gson.fromJson(x, ForgeFile.class)).toList();
        return r;
    }

    public List<ForgeFile> getFiles(List<Integer> ids) throws NoConnectionException {
        var obj = new JsonObject();
        var arr = new JsonArray();
        arr.asList().addAll(ids.stream().map(JsonPrimitive::new).toList());
        obj.add("fileIds", arr);
        String pst = gson.toJson(obj);
        String ans = post("/v1/mods/files", pst);
        var abc = gson.fromJson(ans, JsonObject.class).get("data").getAsJsonArray().asList().stream().map(x -> gson.fromJson(x, ForgeFile.class));
        return abc.distinct().toList();
    }

    public Path extractModpack(Path path, ForgeModpack mp) throws NoConnectionException, HttpException, StopException {
        var zip = path.to("mpInfo.zip");
        String name = StrUtil.pure(mp.getPack().name);
        var tempDir = path.to(name);
        var manifest = path.to("manifest-" + name + ".json");
        if (!manifest.exists()){
            var ppp = NetUtil.download(mp.getPack().fileUrl, zip, false);
            //Modder.getModder().getHandler().execute(new KeyEvent("stop"));
            zip.extract(tempDir, null);
            assert ppp != null;
            ppp.delete();
            tempDir.to("manifest.json").move(manifest);
            tempDir.to("overrides").move(path);
            tempDir.delete();
        }

        return manifest;
    }

    private String get(String api, List<RequestParameter> params) throws NoConnectionException, HttpException {
        var r = factory.create().to(api)
                .withHeader(new RequestParameter("x-api-key", API_KEY))
                .withParam(new RequestParameter("gameId", GAME_ID));
        if (params != null)
            r.withParams(params);
        return r.getString();
    }
    private String post(String api, String body) throws NoConnectionException {
        var r = factory.create().to(api)
                .withHeader(new RequestParameter("x-api-key", API_KEY))
                .withHeader(RequestParameter.contentType("application/json"));
        return r.post(body);
    }

    public void reload(){
        String get = null;
        try{
            get = get("/v1/categories", null);
        }
        catch (NoConnectionException | HttpException ignored) {

        }
        if (get == null){
            categories = new ArrayList<>();
            return;
        }

        var data = gson.fromJson(get, JsonObject.class).get("data").getAsJsonArray();

        categories = data.asList().stream().map(x -> gson.fromJson(x, ForgeCategory.class)).toList();

    }

    public List<ForgeCategory> getCategories(){
        if (categories == null || categories.isEmpty())
            reload();
        return categories;
    }

    @Override
    public List<CResource> getCoreResources(List<Object> ids, Options opt) throws NoConnectionException, HttpException {
        if (ids.isEmpty())
            return null;

        var resources = getResources(ids);

        if (opt.useMeta() && !opt.getIncludeDependencies())
            return resources.stream().map(x -> (CResource)CResource.fromForgeResourceGeneric(null, null, x, null)).toList();

        var all = new ArrayList<CResource>();
        for (var x : resources){
            try {
                getFullResource(x, opt);
            } catch (NoConnectionException | HttpException ignored) {

            }
            var r = CResource.fromForgeResourceGeneric(opt.getVersionId(), opt.hasLoaderType() ? opt.getLoaderType().getIdentifier() : null, x, null);
            if (r instanceof Modpack mp && opt.getAggregateModpack()){
                var p = Configurator.getConfig().getTemporaryFolder().to(StrUtil.pure(mp.name));
                try {
                    applyModpack(mp, p, opt);
                } catch (StopException ignored) {

                }
                p.delete();
                all.addAll(mp.mods);
                all.addAll(mp.resources);
                all.addAll(mp.shaders);
            }
            else
                all.add(r);
        }

        if (opt.getIncludeDependencies())
            all.addAll(getDependencies(all, opt.clone().self(false)));

        /*return data.asList().stream().map(x -> {
            var res = gson.fromJson(x, ResourceForge.class);
            if (!opt.useMeta() || opt.getIncludeDependencies()) {
                try {
                    getFullResource(res, opt);
                } catch (NoConnectionException | HttpException ignored) {

                }
                return CResource.fromForgeResourceGeneric(null, null, res, null);
            }
            return (CResource) CResource.fromForgeResourceGeneric(opt.getVersionId(), opt.getLoaderType().getIdentifier(), res, null);
        }).toList();*/

        return all;
    }

    @Override
    public List<CResource> getCoreResource(Object id, Options opt) throws NoConnectionException, HttpException {
        return getCoreResources(List.of(id), opt);
    }

    @Override
    public List<CResource> getAllCoreResources(Object id, Options opt) throws NoConnectionException, HttpException {
        var res = getResources(List.of(id));
        if (res == null)
            return null;
        var r = res.get(0);
        return getAllCoreResources(r, opt);
    }

    @Override
    public List<CResource> getAllCoreResources(ModResource r, Options opt) throws NoConnectionException, HttpException {
        var full = getFullResource((ResourceForge) r, opt);

        var loader = opt.hasLoaderType() ? opt.getLoaderType().getIdentifier() : null;
        var files = full.searchGame(opt.getVersionId(), loader);
        return files.stream().map(a -> (CResource)CResource.fromForgeResourceGeneric(opt.getVersionId(), loader, full, a)).toList();
    }

    @Override
    public List<CResource> getCoreResource(ModResource res, Options opt) throws NoConnectionException, HttpException {
        return getCoreResources(List.of(res.getId()), opt);
    }

    @Override
    public List<CResource> getDependencies(List<CResource> res, Options opt) throws NoConnectionException, HttpException {
        var mds = new ArrayList<CResource>();

        //var sendOpt = opt.getIncludeSelf() ? opt : opt.clone().includeSelf();

        for (var m : res){
            if (m.fileUrl == null)
                continue;

            if (opt.getIncludeSelf())
                mds.add(m);

            if (m.dependencies == null)
                continue;
            var ms = getCoreResources(m.dependencies.stream().map(a -> a.id).toList(), opt.dependencies(true));
            if (ms != null)
                mds.addAll(ms);
            //mds.addAll(getDependencies(ms.stream().toList(), sendOpt));
        }

        return mds;
    }

    @Override
    public Type getType() {
        return Type.CURSEFORGE;
    }

    @Override
    public <T extends Enum> Search<T> getSearch(Profile p) {
        return (Search<T>) new ForgeSearch(p);
    }

    @Override
    public void applyModpack(Modpack m, Path path, Options opt) throws NoConnectionException, HttpException, StopException {
        var mp = new ForgeModpack(m);

        var manifest = gson.fromJson(extractModpack(path, mp).read(), Manifest.class);
        mp.applyManifest(manifest);

        var resources = getCoreResources(mp.getProjectIds(), Options.create(opt.getVersionId(), null).meta());
        mp.applyResources(resources, getFiles(mp.getFileIds()));

        m.mods = resources.stream().filter(x -> x instanceof Mod).map(x -> (Mod)x).toList();
        m.resources = resources.stream().filter(x -> x instanceof Resourcepack).map(x -> (Resourcepack)x).toList();
        m.shaders = List.of();
    }
}
