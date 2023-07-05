package com.laeben.corelauncher.minecraft.wrappers.fabric.entities;

import com.laeben.corelauncher.minecraft.wrappers.entities.WrapperVersion;

public class FabricVersion extends WrapperVersion {

    private String name;
    public FabricVersion(){

    }

    public FabricVersion(String id, String wrId){
        this.id = id;
        this.wrapperVersion = wrId;
    }

    @Override
    public String getWrapperVersion(){
        String ver = wrapperVersion;
        if (ver.startsWith(".")){
            String[] spl = ver.substring(1).split(":");
            name = spl[0];
            ver = spl[1];
        }
        else
            name = "fabric";
        return ver;
    }
    @Override
    public String getJsonName() {
        String ver = getWrapperVersion();
        return name + "-loader-" + ver + "-" + id;
    }
}
