package com.laeben.corelauncher.minecraft.wrapper.neoforge.entity;

import com.laeben.corelauncher.minecraft.wrapper.entity.WrapperVersion;

public class NeoForgeVersion extends WrapperVersion {
    @Override
    public String getJsonName(){
        return "neoforge-" + wrapperVersion;
    }
    @Override
    public String getClientName(){
        return "neoforge-" + wrapperVersion + "-client";
    }

    public NeoForgeVersion(String wr){
        wrapperVersion = wr;
        var a = wr.split("\\.");
        id = a.length == 3 ? "1." + a[0] + (a[1].equals("0") ? "" : "." + a[1]) : "*";
    }

    public NeoForgeVersion(String id, String wr){
        wrapperVersion = wr;
        this.id = id;
    }

}
