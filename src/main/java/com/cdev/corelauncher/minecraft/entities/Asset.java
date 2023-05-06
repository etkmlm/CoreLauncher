package com.cdev.corelauncher.minecraft.entities;

public class Asset {
    public String id;
    public String SHA1;
    public int size;
    public int totalSize;
    public String url;
    public String path;

    public Asset(String hash, int size){
        SHA1 = hash;
        this.size = size;
    }
}
