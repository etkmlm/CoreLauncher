package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

public class Profile {
    public static final class ProfileFactory implements JsonSerializer<Profile>, JsonDeserializer<Profile> {

        @Override
        public Profile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return Profiler.getProfiler() == null ? Profile.empty().rename(jsonElement.getAsString()) : Profile.get(Profiler.getProfiler().getProfilesDir().to(jsonElement.getAsString()));
        }

        @Override
        public JsonElement serialize(Profile p, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(p.name);
        }
    }

    private transient boolean isEmpty;
    private String name;
    private String versionId;
    private Account customUser;
    private String[] jvmArgs;
    private Java java;
    private int minRAM;
    private int maxRAM;
    private transient Wrapper wrapper;
    private Path path;

    protected Profile(){
        wrapper = new Vanilla();
    }

    public String getName() {
        return name;
    }

    public String getVersionId() {
        return versionId;
    }

    public Profile setVersionId(String versionId){
        this.versionId = versionId;
        return this;
    }

    public Profile setWrapper(Wrapper w){
        wrapper = w;
        return this;
    }

    public Wrapper getWrapper(){
        return wrapper;
    }

    public Profile setCustomUser(Account user){
        this.customUser = user;
        return this;
    }

    public static Profile empty(){
        var p = new Profile();
        p.isEmpty = true;
        return p;
    }

    public Profile cloneFrom(Profile p){
        if (p.path != null){
            this.name = p.name;
            this.path = p.path;
        }
        this.versionId = p.versionId;
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

        if (path != null)
            path = path.parent().to(name);

        return this;
    }

    public Profile save() {
        if (isEmpty)
            return null;

        String json = GsonUtils.DEFAULT_GSON.toJson(this);

        path.to("profile.json").write(json);

        return this;
    }

    private Profile setProfilePath(Path path){
        this.path = path;
        this.name = path.getName();
        return this;
    }

    public static Profile get(Path profilePath) {
        var file = profilePath.to("profile.json");
        return (file.exists() ? GsonUtils.DEFAULT_GSON.fromJson(file.read(), Profile.class) : new Profile())
                .setProfilePath(profilePath).save();
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

    public Path getPath() {
        return path;
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











