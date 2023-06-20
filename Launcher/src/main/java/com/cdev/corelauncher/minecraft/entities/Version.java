package com.cdev.corelauncher.minecraft.entities;

import com.cdev.corelauncher.utils.entities.Java;

import java.util.Date;
import java.util.List;

public class Version {
    public String id;
    public String type;
    public String url;
    public Date time;
    public Date releaseTime;
    public String SHA1;
    public Java javaVersion;
    public List<Library> libraries;
    public String mainClass;
    public String minecraftArguments;
    public Arguments arguments;
    public Asset assetIndex;
    public String assets;
    public DownloadOptions downloads;

    public Version(){

    }
    public Version(String id){
        this.id = id;
    }

    public boolean checkId(String id){
        return this.id != null && (this.id.equals("*") || this.id.equals(id));
    }

    public String getJsonName(){
        return id;
    }
    public String getClientName(){
        return id;
    }
}
