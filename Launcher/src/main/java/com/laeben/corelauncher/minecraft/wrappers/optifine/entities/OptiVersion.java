package com.laeben.corelauncher.minecraft.wrappers.optifine.entities;

import com.laeben.corelauncher.minecraft.wrappers.entities.WrapperVersion;

public class OptiVersion extends WrapperVersion {
    public String forgeWrapperVersion;
    public String rawUrl;

    public OptiVersion(){

    }

    public OptiVersion(String id, String wrId){
        this.id = id;
        this.wrapperVersion = wrId;
    }

    @Override
    public String getJsonName(){
        return id + "-" + wrapperVersion.replace(' ', '_');
    }

    public boolean checkForge(String version){
        return forgeWrapperVersion.startsWith("#") ? version.endsWith(forgeWrapperVersion.substring(1)) : version.equals(forgeWrapperVersion);
    }
}
