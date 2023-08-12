package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import com.google.gson.annotations.SerializedName;

public class RinthFile {
    public String url;
    public String filename;
    @SerializedName("file_type")
    public String type;
}
