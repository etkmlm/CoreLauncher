package com.laeben.corelauncher.api.entity;

import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.annotation.ReturnsNull;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.*;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.core.entity.Path;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Profile {
    public static final class ProfileFieldFactory implements JsonSerializer<Profile>, JsonDeserializer<Profile> {

        @Override
        public Profile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            //return Profiler.getProfiler() == null ? Profile.empty().rename(jsonElement.getAsString()) : Profile.get(Profiler.profilesDir().to(jsonElement.getAsString()));
            return Profiler.getProfiler() == null ? Profile.create().setName(jsonElement.getAsString()) : Profiler.getProfiler().getProfile(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(Profile p, Type type, JsonSerializationContext jsonSerializationContext) {
            return p == null ? null : new JsonPrimitive(p.name);
        }
    }

    public static final class ProfileFactory implements JsonDeserializer<Profile> {

        @Override
        public Profile deserialize(JsonElement jsonElement, Type elementType, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            Profile p = GsonUtil.DEFAULT_GSON.fromJson(jsonElement, Profile.class);

            var obj = jsonElement.getAsJsonObject();

            if (!obj.has("allResources")) {
                p.allResources = new ArrayList<>();
                for (var key : ResourceType.values()){
                    var name = key.getName().equals("resourcepack") ? "resources" : key.getName() + "s";
                    if (!obj.has(name))
                        continue;
                    var c = obj.get(name);
                    if (!c.isJsonArray())
                        continue;
                    for (var ex : c.getAsJsonArray()){
                        var t = (CResource) GsonUtil.EMPTY_GSON.fromJson(ex, key.getEntityClass());
                        t.type = key;
                        p.allResources.add(t);
                    }
                }
            }

            return p;
        }
    }

    public static final Gson PROFILE_GSON = GsonUtil.DEFAULT_GSON.newBuilder()
            .registerTypeAdapter(Profile.class, new ProfileFactory())
            .create();

    /* Fields */
    private ImageEntity icon;
    private String name;
    private String versionId;
    private String wrapperVersion;
    private Account customUser;
    private String[] jvmArgs;
    private Java java;
    private List<CResource> allResources;
    /*private List<Mod> mods;
    private List<Resourcepack> resources;
    private List<Modpack> modpacks;
    private List<World> worlds;
    private List<Shader> shaders;*/
    private int minRAM;
    private int maxRAM;
    private int type;
    private Loader wrapper;

    /* Generation */

    protected Profile(){
        wrapper = new Vanilla();
    }

    /**
     * Creates a profile instance or reads it from the given path.
     * @param profilePath profile's folder path
     * @return null if the given folder's name starts with dot (.)
     */
    @ReturnsNull
    public static Profile fromFolder(Path profilePath) {
        if (profilePath.getName().startsWith("."))
            return null;

        var file = profilePath.to("profile.json");
        return file.exists() ? PROFILE_GSON.fromJson(file.read(), Profile.class).setName(profilePath.getName()) : null;
    }

    public static Profile create(){
        return new Profile();
    }

    public static Profile fromName(String name){
        return Profile.create().setName(name);
    }

    /* Getters */

    public String getName() {
        return name;
    }

    public String getLoaderVersion(){
        return wrapper instanceof Vanilla ? versionId : wrapperVersion;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getLoaderIdentifier(ResourceType type){
        return type == null || type.isGlobal() ? null : getLoader().getType().getIdentifier();
    }

    public Loader getLoader(){
        return wrapper == null ? new Vanilla() : wrapper;
    }

    public List<CResource> getAllResources() {
        if (allResources == null)
            allResources = new ArrayList<>();
        return allResources;
    }

    public <T extends CResource> List<T> getResources(Class<T> clz, boolean onlyEnabled){
        return getAllResources().stream()
                .filter(a -> a.getClass().equals(clz) && (!onlyEnabled || !a.disabled))
                .map(a -> (T) a)
                .toList();
    }

    public List<World> getOnlineWorlds(boolean onlyEnabled){
        return getResources(World.class, onlyEnabled);
    }

    public List<World> getLocalWorlds(){
        if (getPath() == null)
            return List.of();
        var path = getPath().to("saves");
        return path.getFiles().stream().map(x -> World.fromGzip(null, x.to("level.dat"))).filter(x -> x.levelName != null).toList();
    }

    public boolean isValid(){
        return versionId != null;
    }

    public String[] getJvmArgs() {
        return jvmArgs;
    }

    public Account tryGetUser(){
        return customUser == null ? Configurator.getConfig().getUser() : customUser;
    }

    public int getMaxRAM() {
        return maxRAM;
    }

    public ImageEntity getIcon(){
        return icon;
    }

    public int getMinRAM() {
        return minRAM;
    }

    public Account getUser() {
        return customUser;
    }

    public Java getJava() {
        return java;
    }

    public List<Resourcepack> getResourcepacks(boolean onlyEnabled){
        return getResources(Resourcepack.class, onlyEnabled);
    }

    public List<Mod> getMods(boolean onlyEnabled){
        return getResources(Mod.class, onlyEnabled);
    }

    public List<Shader> getShaders(boolean onlyEnabled){
        return getResources(Shader.class, onlyEnabled);
    }

    public List<Modpack> getModpacks(boolean onlyEnabled){
        return getResources(Modpack.class, onlyEnabled);
    }

    public CResource getResource(Object id){
        /*var r1 = getMods().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r1.isPresent())
            return r1.get();

        var r2 = getModpacks().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r2.isPresent())
            return r2.get();

        var r3 = getResourcepacks().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r3.isPresent())
            return r3.get();

        var r4 = getOnlineWorlds().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r4.isPresent())
            return r4.get();

        var r5 = getShaders().stream().filter(x -> id.equals(x.id)).findFirst();

        return r5.orElse(null);*/
        return getAllResources().stream().filter(a -> a.checkId(id)).findFirst().orElse(null);
    }

    public Path getPath(){
        if (name == null)
            return null;
        return Profiler.profilesDir().to(name);
    }

    /* Setters */

    public Profile setLoaderVersion(String wrapperVersion){
        this.wrapperVersion = wrapperVersion;
        return this;
    }

    public Profile setVersionId(String versionId){
        this.versionId = versionId;
        return this;
    }

    public Profile setLoader(Loader<?> w){
        wrapper = w;
        return this;
    }

    public Profile setCustomUser(Account user){
        this.customUser = user;
        return this;
    }

    public Profile setName(String name){
        this.name = name;
        return this;
    }

    public Profile setJvmArgs(String[] args){
        this.jvmArgs = args;
        return this;
    }

    public Profile setMinRAM(int min){
        this.minRAM = min;
        return this;
    }

    public Profile setMaxRAM(int max){
        this.maxRAM = max;
        return this;
    }

    public Profile setJava(Java java) {
        this.java = java;
        return this;
    }

    public void setIcon(ImageEntity icon){
        this.icon = icon;
    }

    /* Utils */

    public void removeResource(CResource r){
        if (r instanceof Modpack mp){
            getAllResources().removeIf(x -> (x instanceof ModpackContent mpc && mpc.belongs(mp)) || x.isSameResource(mp));
        }
        else
            getAllResources().removeIf(x -> x.isSameResource(r));

        /*if (r instanceof Mod m)
            getMods().removeIf(a -> a.isSameResource(m));
        else if (r instanceof Modpack mp){
            getMods().removeIf(x -> x.belongs(mp));
            getResourcepacks().removeIf(x -> x.belongs(mp));
            getShaders().removeIf(x -> x.belongs(mp));
            getModpacks().removeIf(x -> x.isSameResource(mp));
        }
        else if (r instanceof Resourcepack rp){
            getResourcepacks().removeIf(x -> x.isSameResource(rp));
        }
        else if (r instanceof World w){
            getOnlineWorlds().removeIf(x -> x.isSameResource(w));
        }
        else if (r instanceof Shader s){
            getShaders().removeIf(x -> x.isSameResource(s));
        }*/
    }

    public Profile cloneFrom(Profile p){
        if (p == null)
            throw new IllegalStateException("Cannot clone from a null profile.");

        if (p.getPath() != null){
            this.name = p.name;
        }
        this.icon = p.icon;
        this.versionId = p.versionId;
        this.wrapperVersion = p.wrapperVersion;
        this.customUser = p.customUser;

        this.java = p.java;
        this.minRAM = p.minRAM;
        this.maxRAM = p.maxRAM;
        this.wrapper = p.wrapper;

        this.jvmArgs = p.jvmArgs == null ? null : Arrays.copyOf(p.jvmArgs, p.jvmArgs.length);
        this.allResources = p.allResources == null ? null : new ArrayList<>(p.allResources);
        /*this.mods = p.mods == null ? null : new ArrayList<>(p.mods);
        this.resources = p.resources == null ? null : new ArrayList<>(p.resources);
        this.modpacks = p.modpacks == null ? null : new ArrayList<>(p.modpacks);
        this.worlds = p.worlds == null ? null : new ArrayList<>(p.worlds);
        this.shaders = p.shaders == null ? null : new ArrayList<>(p.shaders);*/

        // mark of smartness :O
        /*this.jvmArgs = p.jvmArgs;
        this.mods = p.mods;
        this.resources = p.resources;
        this.modpacks = p.modpacks;
        this.worlds = p.worlds;
        this.shaders = p.shaders;*/
        return this;
    }

    public Profile copyModdingFrom(Profile p){
        allResources = p.allResources == null ? null : new ArrayList<>(p.allResources);
        wrapper = p.wrapper;
        wrapperVersion = p.wrapperVersion;

        return this;
    }

    public Profile save() {
        try {
            String json = PROFILE_GSON.toJson(this);

            getPath().to("profile.json").write(json);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        return this;
    }
}











