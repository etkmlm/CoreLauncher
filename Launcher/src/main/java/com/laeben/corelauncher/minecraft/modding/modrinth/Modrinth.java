package com.laeben.corelauncher.minecraft.modding.modrinth;

import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.util.RequesterFactory;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entities.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.*;
import com.laeben.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.laeben.corelauncher.utils.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Modrinth {
    //private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.modrinth.com";
    private static Modrinth instance;
    private final Gson gson;
    private final RequesterFactory factory;

    private List<RinthCategory> categories;

    public Modrinth(){
        gson = GsonUtils.empty();

        factory = new RequesterFactory(BASE_URL);

        instance = this;
    }

    public List<RinthCategory> getCategories(String type){
        return categories.stream().filter(x -> x.projectType.equals(type)).toList();
    }

    public static Modrinth getModrinth(){
        return instance;
    }

    public SearchResponseRinth search(SearchRinth search) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/search")
                .withParams(search.getParams())
                .getString();

        return gson.fromJson(str, SearchResponseRinth.class);
    }

    public List<ResourceRinth> getResources(List<String> ids) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/projects")
                .withParam(new RequestParameter("ids", StringUtils.jsArray(ids)).markAsEscapable())
                .getString();

        var json = gson.fromJson(str, JsonArray.class);

        return json.asList().stream().map(x -> gson.fromJson(x, ResourceRinth.class)).toList();
    }

    public <T extends CResource> List<T> getCResources(List<String> ids, String vId, String loader) throws NoConnectionException, HttpException {
        var resources = getResources(ids);
        var all = new ArrayList<T>();
        for (var r : resources){
            var v = getProjectVersions(r.id, vId, loader, DependencyInfo.noDependencies());
            if (v.isEmpty())
                continue;
            all.add(CResource.fromRinthResourceGeneric(r, v.get(0)));
        }

        return all;
    }

    public Path extractModpack(Path path, Modpack mp) throws NoConnectionException, HttpException {
        var zip = path.to("mpInfo.zip");
        String name = StringUtils.pure(mp.name);
        var tempDir = path.to(name);
        var manifest = path.to("manifest-" + name + ".json");
        if (!manifest.exists()){
            var ppp = NetUtils.download(mp.fileUrl, zip, false);
            Modder.getModder().getHandler().execute(new KeyEvent("stop"));
            zip.extract(tempDir, null);
            ppp.delete();
            tempDir.to("modrinth.index.json").move(manifest);
            tempDir.to("overrides").move(path);
            tempDir.delete();
        }

        return manifest;
    }

    public void applyModpack(Modpack mp, Path path, Wrapper wr) throws NoConnectionException, HttpException {
        mp.mods = new ArrayList<>();
        mp.resources = new ArrayList<>();

        var vDeps = getVersions(mp.dependencies.stream().map(x -> x.id.toString()).toList(), DependencyInfo.noDependencies());
        var pDeps = getResources(vDeps.stream().map(x -> x.projectId).toList());
        for (var a : vDeps){
            var p = pDeps.stream().filter(x -> x.id.equals(a.projectId)).findFirst().orElse(null);
            if (p == null)
                continue;

            var res = CResource.fromRinthResourceGeneric(p, a);

            if (!(res instanceof ModpackContent mpc))
                continue;

            mpc.setModpackId(mp.id);

            if (res instanceof Mod m)
                mp.mods.add(m);
            else if (res instanceof Resourcepack r)
                mp.resources.add(r);
            else if (res instanceof Shader s)
                mp.shaders.add(s);
        }

        var mf = gson.fromJson(extractModpack(path, mp).read(), JsonObject.class);
        String key;

        if (wr instanceof Fabric)
            key = wr.getIdentifier() + "-loader";
        else
            key = wr.getIdentifier();

        var deps = mf.get("dependencies").getAsJsonObject();
        if (deps == null)
            return;

        String ver = deps.get(key).getAsString();
        mp.wr = wr;
        mp.wrId = ver;
    }

    public List<Version> getVersions(List<String> versionIds, DependencyInfo dp) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/versions")
                .withParam(new RequestParameter("ids", StringUtils.jsArray(versionIds)).markAsEscapable())
                .getString();

        return processVersions(str, dp);
    }
    public List<Version> getProjectVersions(String rId, String vId, String loader, DependencyInfo dp) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/project/" + rId + "/version");

        if (vId != null)
            str.withParam(new RequestParameter("game_versions", StringUtils.jsArray(List.of(vId))).markAsEscapable());

        if (loader != null)
            str.withParam(new RequestParameter("loaders", StringUtils.jsArray(List.of(loader))).markAsEscapable());

        return processVersions(str.getString(), dp);
    }

    public List<RinthCategory> getAllCategories() throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/tag/category")
                .getString();

        var f = gson.fromJson(str, JsonArray.class);

        return f.asList().stream().map(x -> gson.fromJson(x, RinthCategory.class)).toList();
    }

    private List<Version> processVersions(String json, DependencyInfo dp) throws NoConnectionException, HttpException {
        var response = gson.fromJson(json, JsonArray.class);
        if (response == null)
            return List.of();

        var list = response.asList().stream().map(x -> gson.fromJson(x, Version.class)).collect(Collectors.toList());

        if (dp.includeDependencies()){
            var vv = list.get(0);
            list.clear();
            list.add(vv);

            var depends = new ArrayList<String>();
            for (var l : list){
                if (l.dependencies == null)
                    continue;

                for(var dep : l.dependencies){
                    if (!dep.isRequired())
                        continue;
                    String verId = dep.versionId;
                    if (dep.versionId == null){
                        var ver = getProjectVersions(dep.projectId, dp.versionId(), dp.loader(), dp);
                        verId = ver.get(0).id;
                    }

                    depends.add(verId);
                }
            }

            if (!depends.isEmpty())
                list.addAll(getVersions(depends, dp));
        }

        return list.stream().distinct().toList();
    }

    public void reload(){
        try{
            categories = getAllCategories();
        } catch (NoConnectionException | HttpException e) {
            categories = List.of();
        }

    }
}
