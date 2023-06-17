package com.cdev.corelauncher.minecraft.wrappers.forge.entities;

import com.cdev.corelauncher.minecraft.entities.Arguments;
import com.cdev.corelauncher.minecraft.entities.Version;

import java.util.List;

public class ForgeVersion extends Version {
    public enum ForgeVersionType{
        LATEST, RECOMMENDED, NORMAL
    }

    public ForgeVersion(String id){
        this.id = id;
    }
    public ForgeVersionType forgeVersionType;
    public List<FArtifact> fArtifacts;
    public String wrapperVersion;
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
