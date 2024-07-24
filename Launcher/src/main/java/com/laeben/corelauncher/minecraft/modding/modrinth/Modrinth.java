package com.laeben.corelauncher.minecraft.modding.modrinth;

import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.RequesterFactory;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;
import com.laeben.corelauncher.ui.controller.browser.ModrinthSearch;
import com.laeben.corelauncher.ui.controller.browser.Search;
import com.laeben.corelauncher.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

public class Modrinth implements ModSource {
    //private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.modrinth.com";
    private static Modrinth instance;
    private final Gson gson;
    private final RequesterFactory factory;

    private List<RinthCategory> categories;

    public Modrinth(){
        gson = GsonUtil.empty();

        factory = new RequesterFactory(BASE_URL);

        instance = this;
    }

    /**
     * Get the category from cache.
     * @param type main type of the mod resource. (modpack, mod, resourcepack, shader, etc.)
     * @return categories
     */
    public List<RinthCategory> getCategories(String type){
        if (categories == null || categories.isEmpty())
            reload();
        return categories.stream().filter(x -> x.projectType.equals(type)).toList();
    }

    public static Modrinth getModrinth(){
        return instance;
    }

    /**
     * Modrinth search.
     * @param search parameters
     * @return response
     */
    public SearchResponseRinth search(SearchRinth search) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/search")
                .withParams(search.getParams())
                .getString();

        return gson.fromJson(str, SearchResponseRinth.class);
    }

    /**
     * Get Modrinth projects from ids.
     * @param ids project ids
     * @return found projects
     */
    public List<ResourceRinth> getResources(List<String> ids) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/projects")
                .withParam(new RequestParameter("ids", StrUtil.jsArray(ids)).markAsEscapable())
                .getString();

        var json = gson.fromJson(str, JsonArray.class);

        return json.asList().stream().map(x -> gson.fromJson(x, ResourceRinth.class)).toList();
    }

    /**
     * Extracts modpack content to target path.
     *
     * @param path target directory path
     * @param mp modpack meta
     * @return the path of modpack manifest file
     */
    public Path extractModpack(Path path, Modpack mp) throws NoConnectionException, HttpException, StopException {
        var zip = path.to("mpInfo.zip");
        String name = StrUtil.pure(mp.name);
        var tempDir = path.to(name);
        var manifest = path.to("manifest-" + name + ".json");
        if (!manifest.exists()){
            var ppp = NetUtil.download(mp.fileUrl, zip, false);
            Modder.getModder().getHandler().execute(new KeyEvent("stop"));
            zip.extract(tempDir, null);
            assert ppp != null;
            ppp.delete();
            tempDir.to("modrinth.index.json").move(manifest);
            tempDir.to("overrides").move(path);
            tempDir.delete();
        }

        return manifest;
    }

    /**
     * Get version entites from Modrinth.
     * @param versionIds version entity ids
     * @return full-loaded version entities
     */
    public List<Version> getVersions(List<String> versionIds) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/versions")
                .withParam(new RequestParameter("ids", StrUtil.jsArray(versionIds)).markAsEscapable())
                .getString();

        return processVersions(str);
    }

    public List<CResource> getDependenciesFromVersion(Version v, ResourceRinth res, Options opt) throws NoConnectionException, HttpException {
        if (opt.getIncludeSelf() && res == null)
            res = getResources(List.of(v.projectId)).get(0);

        var projects = new ArrayList<>();
        var versions = new ArrayList<String>();
        for (var dep : v.getDependencies()){
            if (!dep.isRequired() && !dep.isEmbedded())
                continue;
            if (dep.projectId != null)
                projects.add(dep.projectId);
            else if (dep.versionId != null)
                versions.add(dep.versionId);
        }

        var all = new ArrayList<CResource>();
        if (opt.getIncludeSelf())
            all.add(CResource.fromRinthResourceGeneric(res, v));

        if (projects.isEmpty())
            return all;

        all.addAll(getCoreResources(projects, res.getResourceType() == ResourceType.MODPACK ? opt.clone().dependencies(false) : opt));

        if (versions.isEmpty())
            return all;

        var vers = getVersions(versions);
        var prs = getResources(vers.stream().map(a -> a.projectId).toList());
        for (var ve : vers){
            var r = prs.stream().filter(a -> a.getId().equals(ve.projectId)).findFirst();
            if (r.isEmpty())
                continue;
            if (res.getResourceType() == ResourceType.MODPACK)
                all.add(CResource.fromRinthResourceGeneric(r.get(), ve));
            else
                all.addAll(getDependenciesFromVersion(ve, r.get(), opt.self(true)));
            //all.addAll(getDependenciesFromVersion(ve, r.get(), opt.self(true)));
        }

        return all;
    }

    /**
     * Get projects' version entities from Modrinth.
     * @param rId project id
     * @param vId version id
     * @param loader loader identifier
     * @return full-loaded version entities
     */
    public List<Version> getProjectVersions(String rId, String vId, LoaderType loader) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/project/" + rId + "/version");

        if (vId != null)
            str.withParam(new RequestParameter("game_versions", StrUtil.jsArray(List.of(vId))).markAsEscapable());

        if (loader != null && loader.getIdentifier() != null)
            str.withParam(new RequestParameter("loaders", StrUtil.jsArray(List.of(loader.getIdentifier()))).markAsEscapable());

        return processVersions(str.getString());
    }


    private List<Version> processVersions(String json) {
        var response = gson.fromJson(json, JsonArray.class);
        if (response == null)
            return List.of();

        return response.asList().stream().map(x -> gson.fromJson(x, Version.class)).toList();
    }

    /**
     * Get all categories from Modrinth.
     * @return categories
     */
    public List<RinthCategory> getAllCategories() throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/tag/category")
                .getString();

        var f = gson.fromJson(str, JsonArray.class);
        if (f == null)
            return List.of();

        return f.asList().stream().map(x -> gson.fromJson(x, RinthCategory.class)).toList();
    }

    /**
     * Reload and cache the categories.
     * Error-safe method, categories are set to empty in error case.
     */

    public void reload(){
        try{
            categories = getAllCategories();
        } catch (NoConnectionException | HttpException e) {
            categories = List.of();
        }

    }

    @Override
    public List<CResource> getCoreResources(List<Object> ids, Options opt) throws NoConnectionException, HttpException {
        var resources = getResources(ids.stream().map(Object::toString).toList());
        List<CResource> all;
        if (opt.useMeta() && !opt.getIncludeDependencies() && !opt.getAggregateModpack())
            all = resources.stream().map(a -> (CResource)CResource.fromRinthResourceGeneric(a, null)).toList();
        else{
            all = new ArrayList<>();
            //var vers = getVersions(resources.stream().map(a -> a.versions.get(0)).toList());
            for (var r : resources){
                /*var v = vers.stream().filter(a -> a.projectId.equals(r.getId())).findFirst();
                if (v.isEmpty())
                    continue;*/
                var v = getNewestVersionOfProject(r, opt.getVersionId(), opt.getLoaderType());
                if (v == null)
                    continue;

                if (opt.getIncludeDependencies() || (opt.getAggregateModpack() && r.getResourceType() == ResourceType.MODPACK))
                    all.addAll(getDependenciesFromVersion(v, r, opt.self(true)));
                else
                    all.add(CResource.fromRinthResourceGeneric(r, v));
            }
        }
        return all;
    }

    private Version getNewestVersionOfProject(ResourceRinth r, String vId, LoaderType loader) throws NoConnectionException, HttpException {
        /*List<Version> vs = r.versions == null || r.versions.isEmpty() ?
                getProjectVersions(r.id, vId, loader) :
                getVersions(List.of(r.versions.get(0)));*/

        var vs = getProjectVersions(r.getId(), vId, loader);

        return vs.isEmpty() ? null : vs.get(0);
    }

    @Override
    public List<CResource> getCoreResource(Object id, Options opt) throws NoConnectionException, HttpException {
        var rs = getResources(List.of(id.toString()));
        return rs.isEmpty() ? null : getCoreResource(rs.get(0), opt);
    }

    @Override
    public List<CResource> getAllCoreResources(Object id, Options opt) throws NoConnectionException, HttpException {
        var res = getResources(List.of(id.toString()));
        if (res.isEmpty())
            return null;
        return getAllCoreResources(res, opt);
    }

    @Override
    public List<CResource> getAllCoreResources(ModResource res, Options opt) throws NoConnectionException, HttpException {
        var versions = getProjectVersions(res.getId().toString(), opt.getVersionId(), opt.getLoaderType());
        return versions.stream().map(a -> (CResource)CResource.fromRinthResourceGeneric((ResourceRinth) res, a)).toList();
    }

    @Override
    public List<CResource> getCoreResource(ModResource res, Options opt) throws NoConnectionException, HttpException {
        if (!(res instanceof ResourceRinth r))
            return null;

        var v = opt.useMeta() ? null : getNewestVersionOfProject(r, opt.getVersionId(), opt.getLoaderType());

        return ((opt.getAggregateModpack() && res.getResourceType() == ResourceType.MODPACK) || opt.getIncludeDependencies()) && v != null ? getDependenciesFromVersion(v, r, opt) : List.of(CResource.fromRinthResourceGeneric(r, v));
    }

    @Override
    public List<CResource> getDependencies(List<CResource> crs, Options opt) throws NoConnectionException, HttpException {
        var projects = new ArrayList<>();
        var versions = new ArrayList<String>();

        for (var v : crs){
            if (v.dependencies == null)
                continue;
            for (var dep : v.dependencies){
                if (dep.id != null)
                    projects.add(dep.id.toString());
                else
                    versions.add(dep.fileId.toString());
            }
        }

        var all = new ArrayList<CResource>();
        if (opt.getIncludeSelf())
            all.addAll(crs);
        all.addAll(getCoreResources(projects, opt));

        var vers = getVersions(versions);
        var prs = getResources(vers.stream().map(a -> a.projectId).toList());
        for (var ve : vers){
            var r = prs.stream().filter(a -> a.getId().equals(ve.projectId)).findFirst();
            if (r.isEmpty())
                continue;
            if (r.get().getResourceType() == ResourceType.MODPACK)
                all.add(CResource.fromRinthResourceGeneric(r.get(), ve));
            else
                all.addAll(getDependenciesFromVersion(ve, r.get(), opt.self(true)));
        }

        return all;
    }

    @Override
    public Type getType() {
        return Type.MODRINTH;
    }

    @Override
    public <T extends Enum> Search<T> getSearch(Profile p) {
        return (Search<T>) new ModrinthSearch(p);
    }

    @Override
    public void applyModpack(Modpack mp, Path path, Options opt) throws NoConnectionException, HttpException, StopException {
        mp.mods = new ArrayList<>();
        mp.resources = new ArrayList<>();

        var vDeps = getVersions(mp.dependencies.stream().filter(x -> x.fileId != null).map(x -> x.fileId.toString()).toList());
        var pDeps = getResources(vDeps.stream().map(x -> x.projectId).toList());

        var all = new ArrayList<>(getCoreResources(mp.dependencies.stream().filter(x -> x.id != null).map(x -> x.id).toList(), Options.create(opt.getVersionId(), opt.getLoaderType())));

        for (var a : vDeps){
            var p = pDeps.stream().filter(x -> x.getId().equals(a.projectId)).findFirst().orElse(null);
            if (p == null)
                continue;

            var res = CResource.fromRinthResourceGeneric(p, a);

            if (!(res instanceof ModpackContent mpc))
                continue;

            mpc.setModpackId(mp.id);

            all.add(res);
        }

        for (var a : all){
            if (a instanceof Mod m)
                mp.mods.add(m);
            else if (a instanceof Resourcepack r)
                mp.resources.add(r);
            else if (a instanceof Shader s)
                mp.shaders.add(s);
        }

        var mf = gson.fromJson(extractModpack(path, mp).read(), JsonObject.class);
        String key;

        key = opt.getLoaderType().getIdentifier();

        if (opt.getLoaderType() == LoaderType.FABRIC)
            key += "-loader";

        var deps = mf.get("dependencies").getAsJsonObject();
        if (deps == null)
            return;

        String ver = deps.get(key).getAsString();
        mp.wr = opt.getWrapper();
        mp.wrId = ver;
    }
}
