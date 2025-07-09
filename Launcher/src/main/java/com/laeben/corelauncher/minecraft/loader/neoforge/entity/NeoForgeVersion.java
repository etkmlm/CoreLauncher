package com.laeben.corelauncher.minecraft.loader.neoforge.entity;

import com.laeben.corelauncher.minecraft.loader.entity.LoaderVersion;

public class NeoForgeVersion extends LoaderVersion {
    @Override
    public String getJsonName(){
        return "neoforge-" + loaderVersion;
    }
    @Override
    public String getClientName(){
        return "neoforge-" + loaderVersion + "-client";
    }

    public NeoForgeVersion(String wr){
        loaderVersion = wr;
        var a = wr.split("\\.");
        id = a.length == 3 ? "1." + a[0] + (a[1].equals("0") ? "" : "." + a[1]) : "*";
    }

    public NeoForgeVersion(String id, String wr){
        loaderVersion = wr;
        this.id = id;
    }

}
