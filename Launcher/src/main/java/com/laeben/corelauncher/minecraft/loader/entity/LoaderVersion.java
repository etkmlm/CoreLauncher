package com.laeben.corelauncher.minecraft.loader.entity;

import com.laeben.corelauncher.minecraft.entity.Version;

public class LoaderVersion extends Version {
    protected String loaderVersion;

    public <T extends LoaderVersion> T setLoaderVersion(String wrId){
        loaderVersion = wrId;

        return (T) this;
    }

    public String getLoaderVersion(){
        return loaderVersion;
    }
}
