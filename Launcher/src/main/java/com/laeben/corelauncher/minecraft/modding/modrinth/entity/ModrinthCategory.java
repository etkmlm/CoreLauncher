package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.google.gson.annotations.SerializedName;

public class ModrinthCategory {
    public String name;
    public String header;
    @SerializedName("project_type")
    public String projectType;
}
