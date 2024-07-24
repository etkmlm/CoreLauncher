package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.Mod;
import com.laeben.corelauncher.minecraft.modding.entity.ModResource;
import com.laeben.corelauncher.minecraft.modding.entity.ModSource;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.wrapper.optifine.entity.OptiVersion;

import java.util.Date;
import java.util.List;

public class ResourceOpti implements ModResource {

    private String id;
    private Date date;
    private String jsonName;
    private Profile profile;

    public static ResourceOpti fromOptiVersion(Profile p, OptiVersion ver){
        var res = new ResourceOpti();
        res.id = ver.getWrapperVersion();
        res.date = ver.releaseTime;
        res.jsonName = ver.getJsonName();
        res.profile = p;
        return res;
    }

    public Mod getMod(){
        var mod = new Mod();
        mod.id = id;
        mod.fileName = jsonName + ".jar";
        mod.fileUrl = id;
        mod.name = jsonName;
        mod.logoUrl = "/com/laeben/corelauncher/images/wrapper/optifine.png";

        return mod;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getName() {
        return jsonName;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.MOD;
    }

    @Override
    public List<String> getCategories() {
        return List.of();
    }

    @Override
    public String getIcon() {
        return "optifine.png";
    }

    @Override
    public String getURL() {
        return "https://optifine.net/downloads";
    }

    @Override
    public List<String> getAuthors() {
        return List.of("OptiFine");
    }

    @Override
    public List<String> getGameVersions() {
        return List.of(profile.getVersionId());
    }

    @Override
    public ModSource.Type getSourceType() {
        return ModSource.Type.CURSEFORGE;
    }

    @Override
    public Date getCreationDate() {
        return date;
    }
}
