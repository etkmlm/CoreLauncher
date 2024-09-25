package com.laeben.corelauncher.minecraft.wrapper.fabric.entity;

import com.laeben.corelauncher.minecraft.wrapper.entity.WrapperVersion;

public abstract class BaseFabricVersion extends WrapperVersion {
    private String name;

    public BaseFabricVersion(){

    }

    public BaseFabricVersion(String id, String wrId){
        this.id = id;
        this.wrapperVersion = wrId;
    }

    public abstract String getForkName();

    @Override
    public String getWrapperVersion(){
        String ver = wrapperVersion;
        if (ver.startsWith(".")){
            String[] spl = ver.substring(1).split(":");
            name = spl[0];
            ver = spl[1];
        }
        else
            name = getForkName();
        return ver;
    }
    @Override
    public String getJsonName() {
        String ver = getWrapperVersion();
        return name + "-loader-" + ver + "-" + id;
    }
}
