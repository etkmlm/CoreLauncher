package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public class ResourceRinth {
    public String id;
    @SerializedName("project_id")
    public String pId;

    public String slug;
    public String title;
    public String description;
    public List<String> categories;
    @SerializedName("project_type")
    public String projectType;
    @SerializedName("icon_url")
    public String icon;
    public String team;
    public String author;
    @SerializedName("game_versions")
    public List<String> gameVersions;
    public List<String> loaders;


    public String getId(){
        return id == null ? pId : id;
    }
}
