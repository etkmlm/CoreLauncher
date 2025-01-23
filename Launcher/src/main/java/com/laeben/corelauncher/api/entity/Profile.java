package com.laeben.corelauncher.api.entity;

import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.core.entity.Path;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Profile {
    public static final class ProfileFactory implements JsonSerializer<Profile>, JsonDeserializer<Profile> {

        @Override
        public Profile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            //return Profiler.getProfiler() == null ? Profile.empty().rename(jsonElement.getAsString()) : Profile.get(Profiler.profilesDir().to(jsonElement.getAsString()));
            return Profiler.getProfiler() == null ? Profile.empty().setName(jsonElement.getAsString()) : Profiler.getProfiler().getProfile(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(Profile p, Type type, JsonSerializationContext jsonSerializationContext) {
            return p.isEmpty() ? null : new JsonPrimitive(p.name);
        }
    }

    /* Fields */
    private transient boolean isEmpty;
    private ImageEntity icon;
    private String name;
    private String versionId;
    private String wrapperVersion;
    private Account customUser;
    private String[] jvmArgs;
    private Java java;
    private List<Mod> mods;
    private List<Resourcepack> resources;
    private List<Modpack> modpacks;
    private List<World> worlds;
    private List<Shader> shaders;
    private int minRAM;
    private int maxRAM;
    private int type;
    private Wrapper wrapper;

    /* Generation */

    protected Profile(){
        wrapper = new Vanilla();
    }

    public static Profile get(Path profilePath) {
        try{
            var file = profilePath.to("profile.json");
            return (file.exists() ? GsonUtil.DEFAULT_GSON.fromJson(file.read(), Profile.class) : new Profile())
                    .setName(profilePath.getName()).save();
        }
        catch (Exception e){
            Logger.getLogger().logHyph("CORRUPT PROFILE: " + profilePath);
            Logger.getLogger().log(e);
            return Profile.empty();
        }
    }

    public static Profile empty(){
        var p = new Profile();
        p.isEmpty = true;
        return p;
    }

    public static Profile fromName(String name){
        return Profile.empty().setName(name);
    }

    /* Getters */

    public String getName() {
        return name;
    }

    public String getWrapperVersion(){
        return wrapper instanceof Vanilla ? versionId : wrapperVersion;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getWrapperIdentifier(ResourceType type){
        return type == null || type.isGlobal() ? null : getWrapper().getType().getIdentifier();
    }

    public LoaderType getLoaderType(ResourceType resType){
        return resType == null || resType.isGlobal() ? null : getWrapper().getType();
    }

    public Wrapper getWrapper(){
        return wrapper == null ? new Vanilla() : wrapper;
    }

    public List<World> getOnlineWorlds(){
        if (worlds == null)
            worlds = new ArrayList<>();
        return worlds;
    }

    public List<World> getLocalWorlds(){
        if (getPath() == null)
            return List.of();
        var path = getPath().to("saves");
        return path.getFiles().stream().map(x -> World.fromGzip(null, x.to("level.dat"))).filter(x -> x.levelName != null).toList();
    }

    public boolean isEmpty(){
        return isEmpty;
    }

    public boolean isValid(){
        return !isEmpty() && versionId != null;
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

    public List<Resourcepack> getResources(){
        if (resources == null)
            resources = new ArrayList<>();
        return resources;
    }

    public List<Mod> getMods(){
        if (mods == null)
            mods = new ArrayList<>();

        return mods;
    }

    public List<Shader> getShaders(){
        if (shaders == null)
            shaders = new ArrayList<>();

        return shaders;
    }

    public List<Modpack> getModpacks(){
        if (modpacks == null)
            modpacks = new ArrayList<>();

        return modpacks;
    }

    public CResource getResource(Object id){
        var r1 = getMods().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r1.isPresent())
            return r1.get();

        var r2 = getModpacks().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r2.isPresent())
            return r2.get();

        var r3 = getResources().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r3.isPresent())
            return r3.get();

        var r4 = getOnlineWorlds().stream().filter(x -> id.equals(x.id)).findFirst();

        if (r4.isPresent())
            return r4.get();

        var r5 = getShaders().stream().filter(x -> id.equals(x.id)).findFirst();

        return r5.orElse(null);

    }

    public Path getPath(){
        if (name == null)
            return null;
        return Profiler.profilesDir().to(name);
    }

    /* Setters */

    public Profile setWrapperVersion(String wrapperVersion){
        this.wrapperVersion = wrapperVersion;
        return this;
    }

    public Profile setVersionId(String versionId){
        this.versionId = versionId;
        return this;
    }

    public Profile setWrapper(Wrapper<?> w){
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

    public void removeSource(CResource r){
        if (r instanceof Mod m)
            getMods().removeIf(a -> a.isSameResource(m));
        else if (r instanceof Modpack mp){
            getMods().removeIf(x -> x.belongs(mp));
            getResources().removeIf(x -> x.belongs(mp));
            getShaders().removeIf(x -> x.belongs(mp));
            getModpacks().removeIf(x -> x.isSameResource(mp));
        }
        else if (r instanceof Resourcepack rp){
            getResources().removeIf(x -> x.isSameResource(rp));
        }
        else if (r instanceof World w){
            getOnlineWorlds().removeIf(x -> x.isSameResource(w));
        }
        else if (r instanceof Shader s){
            getShaders().removeIf(x -> x.isSameResource(s));
        }
    }

    public Profile cloneFrom(Profile p){
        if (this.isEmpty)
            this.isEmpty = false;
        if (p.getPath() != null){
            this.name = p.name;
        }
        this.icon = p.icon;
        this.versionId = p.versionId;
        this.wrapperVersion = p.wrapperVersion;
        this.customUser = p.customUser;
        this.jvmArgs = p.jvmArgs;
        this.java = p.java;
        this.minRAM = p.minRAM;
        this.maxRAM = p.maxRAM;
        this.wrapper = p.wrapper;
        this.mods = p.mods;
        this.resources = p.resources;
        this.modpacks = p.modpacks;
        this.worlds = p.worlds;
        this.shaders = p.shaders;
        return this;
    }

    public Profile save() {
        if (isEmpty)
            return null;

        try {
            String json = GsonUtil.DEFAULT_GSON.toJson(this);

            getPath().to("profile.json").write(json);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        return this;
    }
}











