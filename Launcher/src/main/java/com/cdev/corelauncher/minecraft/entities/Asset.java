package com.cdev.corelauncher.minecraft.entities;

import com.google.gson.*;

import java.lang.reflect.Type;

public class Asset {
    public String id;
    public String SHA1;
    public int size;
    public int totalSize;
    public String url;
    public String path;

    public Asset(String path, String hash, int size){
        this.path = path;
        SHA1 = hash;
        this.size = size;
    }

    public boolean isLegacy(){
        return id.equals("legacy");
    }

    public boolean isVeryLegacy(){
        return id.equals("pre-1.6");
    }
}
