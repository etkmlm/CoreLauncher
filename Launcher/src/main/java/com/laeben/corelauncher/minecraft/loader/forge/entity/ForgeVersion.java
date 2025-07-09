package com.laeben.corelauncher.minecraft.loader.forge.entity;

import com.laeben.corelauncher.minecraft.entity.Arguments;
import com.laeben.corelauncher.minecraft.loader.entity.LoaderVersion;

import java.util.List;

public class ForgeVersion extends LoaderVersion {
    public enum ForgeVersionType{
        LATEST, RECOMMENDED, NORMAL
    }

    public ForgeVersion(String id){
        this.id = id;
    }

    public ForgeVersion(String id, String wrId){
        this.id = id;
        this.loaderVersion = wrId;
    }
    public ForgeVersionType forgeVersionType;
    public List<FArtifact> fArtifacts;
    @Override
    public String getJsonName(){
        return id + "-forge-" + loaderVersion;
    }
    public String getBaseIdentity(){
        return id + "-" + loaderVersion;
    }

    @Override
    public String getClientName(){
        return "forge-" + getBaseIdentity();
    }
    public Arguments arguments;
}
