package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.google.gson.annotations.SerializedName;

public class RinthFile {
    public transient String id;
    public String url;
    public String filename;
    @SerializedName("file_type")
    public String type;
}
