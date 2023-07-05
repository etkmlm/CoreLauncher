package com.laeben.corelauncher.minecraft.wrappers.entities;

import com.laeben.corelauncher.minecraft.entities.Version;

public class WrapperVersion extends Version {
    protected String wrapperVersion;

    public <T extends WrapperVersion> T setWrapperVersion(String wrId){
        wrapperVersion = wrId;

        return (T) this;
    }

    public String getWrapperVersion(){
        return wrapperVersion;
    }
}
