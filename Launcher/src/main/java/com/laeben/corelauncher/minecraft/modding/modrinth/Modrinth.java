package com.laeben.corelauncher.minecraft.modding.modrinth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.RequesterFactory;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.*;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;
import com.laeben.corelauncher.ui.controller.browser.ModrinthSearch;
import com.laeben.corelauncher.ui.controller.browser.Search;
import com.laeben.corelauncher.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Modrinth implements ModSource {
    //private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.modrinth.com";
    private static final String PREFERRED_SHADER_MOD = "YL57xq9U"; // Iris

    private static Modrinth instance;
    private final Gson gson;
    private final RequesterFactory factory;

    private List<ModrinthCategory> categories;

    public Modrinth(){
        gson = GsonUtil.EMPTY_GSON;

        factory = new RequesterFactory(BASE_URL);

        instance = this;
    }

    /**
     * Get the category from cache.
     * @param type main type of the mod resource. (modpack, mod, resourcepack, shader, etc.)
     * @return categories
     */
    public List<ModrinthCategory> getCategories(String type){
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
    public ModrinthSearchResponse search(ModrinthSearchRequest search) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/search")
                .withParams(search.getParams())
                .getString();

        return gson.fromJson(str, ModrinthSearchResponse.class);
    }

    /**
     * Get Modrinth projects from ids.
     * @param ids project ids
     * @return found projects
     */
    public List<ModrinthResource> getResources(List<String> ids) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/projects")
                .withParam(new RequestParameter("ids", StrUtil.jsArray(ids)).markAsEscapable())
                .getString();

        var json = gson.fromJson(str, JsonArray.class);

        return json.asList().stream().map(x -> gson.fromJson(x, ModrinthResource.class)).toList();
    }

    @Override
    public Path extractModpack(Modpack mp, Path path, boolean overwriteManifest) throws NoConnectionException, HttpException, StopException {
        var zip = path.to("mpInfo.zip");
        String name = StrUtil.pure(mp.name);
        var tempDir = path.to(name);
        var manifest = path.to("manifest-" + name + ".json");
        if (overwriteManifest)
            manifest.delete();

        if (!manifest.exists()){
            var ppp = NetUtil.download(mp.fileUrl, zip, false);
            //Modder.getModder().getHandler().execute(new KeyEvent("stop"));
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

    public List<CResource> getDependenciesFromVersion(Version v, ModrinthResource res, Options opt) throws NoConnectionException, HttpException {
        if (opt.getIncludeSelf() && res == null)
            res = getResources(List.of(v.projectId)).get(0);

        var projects = new ArrayList<>();
        var versions = new ArrayList<String>();
        for (var dep : v.getDependencies()){
            if (!dep.isRequired() && !dep.isEmbedded())
                continue;
            if (dep.versionId != null)
                versions.add(dep.versionId);
            else if (dep.projectId != null)
                projects.add(dep.projectId);
        }

        var all = new ArrayList<CResource>();
        if (opt.getIncludeSelf())
            all.add(CResource.fromRinthResourceGeneric(res, v));

        if (!projects.isEmpty())
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
     * @param versionIds version id
     * @param loaders loaders
     * @return full-loaded version entities
     */
    public List<Version> getProjectVersions(String rId, List<String> versionIds, List<LoaderType> loaders) throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/project/" + rId + "/version");

        boolean validVersions = versionIds != null && !versionIds.isEmpty();
        boolean validLoaders = false;

        List<String> loaderIds = null;

        if (loaders != null){
            loaderIds = loaders.stream().filter(a -> !a.isNative()).map(LoaderType::getIdentifier).toList();
            if (!loaderIds.isEmpty())
                validLoaders = true;
        }

        if (validVersions)
            str.withParam(new RequestParameter("game_versions", StrUtil.jsArray(versionIds)).markAsEscapable());

        if (validLoaders)
            str.withParam(new RequestParameter("loaders", StrUtil.jsArray(loaderIds)).markAsEscapable());

        var vers = processVersions(str.getString());
        if (!validVersions && !validLoaders)
            return vers;

        var newVers = new ArrayList<Version>();
        for (var ver : vers){
            if (ver.gameVersions != null){
                if (validVersions){
                    boolean match = false;
                    for (var v : ver.gameVersions){
                        if (v == null || v.isBlank())
                            continue;

                        for (var x : versionIds){
                            if (x.startsWith(v)){
                                match = true;
                                break;
                            }
                        }

                        if (match)
                            break;
                    }
                    if (!match)
                        continue;
                }
                /*if (validVersions && ver.gameVersions.stream().allMatch(x -> x != null && !x.isBlank() && versionIds.stream().noneMatch(v -> v.startsWith(x))))
                    continue;*/
            }

            if (ver.loaders != null){
                if (validLoaders && Collections.disjoint(ver.loaders, loaderIds))
                    continue;
            }

            newVers.add(ver);
        }


        return newVers;
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
    public List<ModrinthCategory> getAllCategories() throws NoConnectionException, HttpException {
        var str = factory.create()
                .to("/v2/tag/category")
                .getString();

        var f = gson.fromJson(str, JsonArray.class);
        if (f == null)
            return List.of();

        return f.asList().stream().map(x -> gson.fromJson(x, ModrinthCategory.class)).toList();
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
                var v = getNewestVersionOfProject(r, opt.getVersionIds(), opt.getLoaders());
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

    private Version getNewestVersionOfProject(ModrinthResource r, List<String> versionIds, List<LoaderType> loaders) throws NoConnectionException, HttpException {
        /*List<Version> vs = r.versions == null || r.versions.isEmpty() ?
                getProjectVersions(r.id, vId, loader) :
                getVersions(List.of(r.versions.get(0)));*/

        var vs = getProjectVersions(r.getId(), versionIds, loaders);

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
        var versions = getProjectVersions(res.getId().toString(), opt.getVersionIds(), opt.getLoaders());
        return versions.stream().map(a -> (CResource)CResource.fromRinthResourceGeneric((ModrinthResource) res, a)).toList();
    }

    @Override
    public List<CResource> getCoreResource(ModResource res, Options opt) throws NoConnectionException, HttpException {
        if (!(res instanceof ModrinthResource r))
            return null;

        var v = opt.useMeta() ? null : getNewestVersionOfProject(r, opt.getVersionIds(), opt.getLoaders());

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
        mp.shaders = new ArrayList<>();

        var vDeps = getVersions(mp.dependencies.stream().filter(x -> x.fileId != null).map(x -> x.fileId.toString()).toList());
        var pDeps = getResources(vDeps.stream().map(x -> x.projectId).toList());

        var vId = opt.getVersionId();
        var loader = opt.getLoaderType();

        var all = new ArrayList<>(getCoreResources(mp.dependencies.stream().filter(x -> x.id != null).map(x -> x.id).toList(), Options.create(vId, loader)));

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

        var mf = gson.fromJson(extractModpack(mp, path, true).read(), JsonObject.class);
        String key;

        key = loader.getIdentifier();

        if (loader == LoaderType.FABRIC)
            key += "-loader";

        // check disabled dependencies
        if (mf.has("files")){
            var files = mf.get("files").getAsJsonArray().asList();
            for (var f : files){
                var fobj = f.getAsJsonObject();
                List<JsonElement> downloads;
                if (!fobj.has("path") || !fobj.get("path").getAsString().endsWith(".disabled") || !fobj.has("downloads") || (downloads = fobj.get("downloads").getAsJsonArray().asList()).isEmpty())
                    continue;
                for (var d : downloads){
                    var ds = d.getAsString();
                    if (!ds.startsWith("https://cdn.modrinth.com/data"))
                        continue;
                    var spl = ds.split("/");
                    var pId = spl[4];
                    all.removeIf(a -> a.getId().equals(pId));
                }

            }
        }

        for (var a : all){
            if (a instanceof Mod m)
                mp.mods.add(m);
            else if (a instanceof Resourcepack r)
                mp.resources.add(r);
            else if (a instanceof Shader s)
                mp.shaders.add(s);
        }

        if (!mf.has("dependencies"))
            return;

        var deps = mf.get("dependencies").getAsJsonObject();
        if (deps == null)
            return;

        mp.targetVersionId = deps.get("minecraft").getAsString();

        String ver = deps.get(key).getAsString();
        mp.wr = opt.getLoader();
        mp.wrId = ver;
    }


    public List<CResource> getPreferredShaderMod(Options opt) throws NoConnectionException, HttpException {
        return getCoreResource(PREFERRED_SHADER_MOD, opt);
    }
}
