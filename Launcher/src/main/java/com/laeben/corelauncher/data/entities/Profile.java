package com.laeben.corelauncher.data.entities;

import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entities.*;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.entities.Java;
import com.laeben.core.entity.Path;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Profile {
    public static final class ProfileFactory implements JsonSerializer<Profile>, JsonDeserializer<Profile> {

        @Override
        public Profile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return Profiler.getProfiler() == null ? Profile.empty().rename(jsonElement.getAsString()) : Profile.get(Profiler.profilesDir().to(jsonElement.getAsString()));
        }

        @Override
        public JsonElement serialize(Profile p, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(p.name);
        }
    }

    private transient boolean isEmpty;
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
    private Wrapper wrapper;

    protected Profile(){
        wrapper = new Vanilla();

    }

    public String getName() {
        return name;
    }

    public String getWrapperVersion(){
        return wrapper instanceof Vanilla ? versionId : wrapperVersion;
    }

    public Profile setWrapperVersion(String wrapperVersion){
        this.wrapperVersion = wrapperVersion;
        return this;
    }

    public String getVersionId() {
        return versionId;
    }

    public Profile setVersionId(String versionId){
        this.versionId = versionId;
        return this;
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

    public Profile setWrapper(Wrapper<?> w){
        wrapper = w;
        return this;
    }

    public Path getPath(){
        if (name == null)
            return null;
        return Profiler.profilesDir().to(name);
    }

    public Wrapper getWrapper(){
        return wrapper == null ? new Vanilla() : wrapper;
    }

    public Profile setCustomUser(Account user){
        this.customUser = user;
        return this;
    }

    public List<World> getOnlineWorlds(){
        if (worlds == null)
            worlds = new ArrayList<>();
        return worlds;
    }

    public List<World> getLocalWorlds(){
        var path = getPath().to("saves");
        return path.getFiles().stream().map(x -> World.fromGzip(null, x.to("level.dat"))).filter(x -> x.levelName != null).toList();
    }

    public static Profile empty(){
        var p = new Profile();
        p.isEmpty = true;
        return p;
    }

    public boolean isEmpty(){
        return isEmpty;
    }

    public Profile cloneFrom(Profile p){
        if (p.getPath() != null){
            this.name = p.name;
        }
        this.versionId = p.versionId;
        this.wrapperVersion = p.wrapperVersion;
        this.customUser = p.customUser;
        this.jvmArgs = p.jvmArgs;
        this.java = p.java;
        this.minRAM = p.minRAM;
        this.maxRAM = p.maxRAM;
        this.wrapper = p.wrapper;
        return this;
    }

    public Profile rename(String name){
        this.name = name;

        return this;
    }

    public Profile save() {
        if (isEmpty)
            return null;

        try {
            String json = GsonUtils.DEFAULT_GSON.toJson(this);

            getPath().to("profile.json").write(json);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        return this;
    }

    private Profile setProfileName(String name){
        this.name = name;
        return this;
    }

    public static Profile get(Path profilePath) {
        try{
            var file = profilePath.to("profile.json");
            return (file.exists() ? GsonUtils.DEFAULT_GSON.fromJson(file.read(), Profile.class) : new Profile())
                    .setProfileName(profilePath.getName()).save();
        }
        catch (Exception e){
            Logger.getLogger().logHyph("CORRUPT PROFILE: " + profilePath);
            Logger.getLogger().log(e);
            return null;
        }
    }

    public String[] getJvmArgs() {
        return jvmArgs;
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

    public Java getJava() {
        return java;
    }
    public Profile setJava(Java java) {
        this.java = java;
        return this;
    }

    public int getMinRAM() {
        return minRAM;
    }

    public Account getUser() {
        return customUser;
    }

    public int getMaxRAM() {
        return maxRAM;
    }

}











