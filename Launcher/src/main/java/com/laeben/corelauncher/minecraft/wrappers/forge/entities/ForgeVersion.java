package com.laeben.corelauncher.minecraft.wrappers.forge.entities;

import com.laeben.corelauncher.minecraft.entities.Arguments;
import com.laeben.corelauncher.minecraft.wrappers.entities.WrapperVersion;

import java.util.List;

public class ForgeVersion extends WrapperVersion {
    public enum ForgeVersionType{
        LATEST, RECOMMENDED, NORMAL
    }

    public ForgeVersion(String id){
        this.id = id;
    }

    public ForgeVersion(String id, String wrId){
        this.id = id;
        this.wrapperVersion = wrId;
    }
    public ForgeVersionType forgeVersionType;
    public List<FArtifact> fArtifacts;
    @Override
    public String getJsonName(){
        return id + "-forge-" + wrapperVersion;
    }
    public String getBaseIdentity(){
        return id + "-" + wrapperVersion;
    }

    @Override
    public String getClientName(){
        return "forge-" + getBaseIdentity();
    }
    public Arguments arguments;
}
