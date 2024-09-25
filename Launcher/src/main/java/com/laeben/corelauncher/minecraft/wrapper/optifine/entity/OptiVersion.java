package com.laeben.corelauncher.minecraft.wrapper.optifine.entity;

import com.laeben.corelauncher.minecraft.wrapper.entity.WrapperVersion;

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

    @Override
    public String getClientName(){
        return getJsonName();
    }

    public boolean checkForge(String version){
        return forgeWrapperVersion.startsWith("#") ? version.endsWith(forgeWrapperVersion.substring(1)) : version.equals(forgeWrapperVersion);
    }
}
