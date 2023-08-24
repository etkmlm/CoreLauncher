package com.laeben.corelauncher.minecraft.modding.curseforge;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.core.util.RequesterFactory;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.*;
import com.laeben.corelauncher.minecraft.modding.entities.*;
import com.laeben.core.entity.Path;
import com.google.gson.*;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CurseForge {
    private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.curseforge.com";
    private static final int GAME_ID = 432;
    private static CurseForge instance;

    private final Gson gson;

    private List<ForgeCategory> categories;
    private final RequesterFactory factory;


    public CurseForge(){
        gson = GsonUtils.empty();

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

    public List<CResource> getResources(String vId, List<Integer> ids, Profile p, boolean useFullResource) throws NoConnectionException {
        if (ids.isEmpty())
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
            var res = gson.fromJson(x, ResourceForge.class);
            if (type != null && useFullResource) {
                try {
                    getFullResource(vId, type, res);
                } catch (NoConnectionException | HttpException ignored) {

                }
            }
            return (CResource) CResource.fromForgeResourceGeneric(vId, iden, res);
        }).toList();
    }
    public List<CResource> getDependencies(List<CResource> res, Profile p) throws NoConnectionException {
        var mds = new ArrayList<CResource>();
        String vId = p.getVersionId();

        for (var m : res){
            if (m.fileUrl == null)
                continue;
            mds.add(m);

            if (m.dependencies == null)
                continue;
            var ms = getResources(vId, m.dependencies.stream().map(x -> (int)x.id).toList(), p, true);
            mds.addAll(ms);
            mds.addAll(getDependencies(ms.stream().toList(), p));
        }

        return mds;
    }

    public ResourceForge getFullResource(String vId, CurseWrapper.Type type, ResourceForge r) throws NoConnectionException, HttpException {

        var params = new ArrayList<RequestParameter>();
        params.add(new RequestParameter("gameVersion", vId));
        if (type != CurseWrapper.Type.ANY)
            params.add(new RequestParameter("modLoaderType", type.ordinal()));

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

    public void applyModpack(String vId, Path path, Modpack m) throws NoConnectionException, HttpException {
        var mp = new ForgeModpack(m);

        var manifest = gson.fromJson(extractModpack(path, mp).read(), Manifest.class);
        mp.applyManifest(manifest);

        var resources = getResources(vId, mp.getProjectIds(), null, false);
        mp.applyResources(resources, getFiles(mp.getFileIds()));

        m.mods = resources.stream().filter(x -> x instanceof Mod).map(x -> (Mod)x).toList();
        m.resources = resources.stream().filter(x -> x instanceof Resourcepack).map(x -> (Resourcepack)x).toList();
    }

    public Path extractModpack(Path path, ForgeModpack mp) throws NoConnectionException, HttpException {
        var zip = path.to("mpInfo.zip");
        String name = StringUtils.pure(mp.getPack().name);
        var tempDir = path.to(name);
        var manifest = path.to("manifest-" + name + ".json");
        if (!manifest.exists()){
            var ppp = NetUtils.download(mp.getPack().fileUrl, zip, false);
            Modder.getModder().getHandler().execute(new KeyEvent("stop"));
            zip.extract(tempDir, null);
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
        return categories;
    }
}
