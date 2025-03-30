package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Mod;
import com.laeben.corelauncher.minecraft.wrapper.optifine.entity.OptiVersion;

import java.util.Date;

public class ResourceOpti implements ModResource {

    private String id;
    private Date date;
    private String jsonName;
    private String versionId;

    public static ResourceOpti fromOptiVersion(String versionId, OptiVersion ver){
        var res = new ResourceOpti();
        res.id = ver.getWrapperVersion();
        res.date = ver.releaseTime;
        res.jsonName = ver.getJsonName();
        res.versionId = versionId;
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
    public String[] getCategories() {
        return null;
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
    public String[] getAuthors() {
        return new String[]{"OptiFine"};
    }

    @Override
    public String[] getGameVersions() {
        return new String[] {versionId};
    }

    @Override
    public LoaderType[] getLoaders() {
        return new LoaderType[]{LoaderType.FORGE};
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
