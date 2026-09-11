package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.google.gson.annotations.SerializedName;

public class ModrinthFile {
    public static class Hashes{
        public String sha512;
        public String sha1;
    }

    public transient String id;
    public String url;
    public String filename;
    @SerializedName("file_type")
    public String type;
    public long size;
    public Hashes hashes;
}
