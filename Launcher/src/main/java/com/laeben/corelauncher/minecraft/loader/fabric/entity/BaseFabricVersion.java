package com.laeben.corelauncher.minecraft.loader.fabric.entity;

import com.laeben.corelauncher.minecraft.loader.entity.LoaderVersion;

public abstract class BaseFabricVersion extends LoaderVersion {
    private String name;

    public BaseFabricVersion(){

    }

    public BaseFabricVersion(String id, String wrId){
        this.id = id;
        this.loaderVersion = wrId;
    }

    public abstract String getForkName();

    @Override
    public String getLoaderVersion(){
        String ver = loaderVersion;
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
        String ver = getLoaderVersion();
        return name + "-loader-" + ver + "-" + id;
    }
}
