package com.laeben.corelauncher.minecraft.wrapper.entity;

import com.laeben.corelauncher.minecraft.entity.Version;

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
