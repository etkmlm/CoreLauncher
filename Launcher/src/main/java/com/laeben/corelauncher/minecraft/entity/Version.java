package com.laeben.corelauncher.minecraft.entity;

import com.laeben.corelauncher.api.entity.Java;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Version {
    public String id;
    public String type;
    public String url;
    public transient Date time;
    public transient Date releaseTime;
    public String SHA1;
    public Java javaVersion;
    public List<Library> libraries;
    public String mainClass;
    public String minecraftArguments;
    public Arguments arguments;
    private Asset assetIndex;
    public String inheritsFrom;
    public String assets;
    public DownloadOptions downloads;

    public Version(){

    }
    public Version(String id){
        this.id = id;
    }

    public Asset getAssetIndex(){
        return assetIndex == null ? new Asset(inheritsFrom == null ? id : inheritsFrom) : assetIndex;
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


    public static class VersionIdComparator implements Comparator<String> {

        public static final VersionIdComparator INSTANCE = new VersionIdComparator();

        @Override
        public int compare(String o1, String o2) {
            try{
                int i1 = Integer.parseInt(o1.replace(".", ""));
                int i2 = Integer.parseInt(o2.replace(".", ""));

                return i1 - i2;
            }
            catch (NumberFormatException ignored){

            }

            return 0;
        }
    }
}
