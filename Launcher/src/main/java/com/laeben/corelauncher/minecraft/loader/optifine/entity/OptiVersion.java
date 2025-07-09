package com.laeben.corelauncher.minecraft.loader.optifine.entity;

import com.laeben.corelauncher.minecraft.loader.entity.LoaderVersion;

public class OptiVersion extends LoaderVersion {
    public String forgeLoaderVersion;
    public String rawUrl;

    public OptiVersion(){

    }

    public OptiVersion(String id, String wrId){
        this.id = id;
        this.loaderVersion = wrId;
    }

    @Override
    public String getJsonName(){
        return id + "-" + loaderVersion.replace(' ', '_');
    }

    @Override
    public String getClientName(){
        return getJsonName();
    }

    public boolean checkForge(String version){
        return forgeLoaderVersion.startsWith("#") ? version.endsWith(forgeLoaderVersion.substring(1)) : version.equals(forgeLoaderVersion);
    }
}
