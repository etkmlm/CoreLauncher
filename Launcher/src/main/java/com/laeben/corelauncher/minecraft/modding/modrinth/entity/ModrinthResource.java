package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.google.gson.annotations.SerializedName;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.minecraft.modding.entity.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ModrinthResource implements ModResource {
    @SerializedName("project_id")
    public String pId;
    public String slug;
    public String team;
    public List<String> loaders;
    @SerializedName("date_modified")
    public String published;
    public List<String> versions;

    public String id;
    public String title;
    public String description;
    @SerializedName("project_type")
    public String projectType;
    public List<String> categories;
    @SerializedName("icon_url")
    public String icon;
    public String author;
    @SerializedName("game_versions")
    public List<String> gameVersions;

    @Override
    public String getId(){
        return id == null ? pId : id;
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.fromName(projectType);
    }

    @Override
    public String[] getCategories() {
        return categories.toArray(String[]::new);
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public String getURL(){
        return "https://modrinth.com/mod/" + getId();
    }

    @Override
    public String[] getAuthors() {
        return new String[] {team == null ? author : team};
    }

    @Override
    public String[] getGameVersions() {
        var vers = new ArrayList<String>();
        var vs = versions != null ? versions : gameVersions;
        if (vs != null){
            for (var v : vs){
                if (v.contains(".") && !v.contains("-"))
                    vers.add(v);
            }
        }

        return vers.toArray(String[]::new);
    }

    @Override
    public LoaderType[] getLoaders() {
        if (categories == null)
            return null;

        var arr = new ArrayList<LoaderType>();
        for (var c : categories){
            if (LoaderType.TYPES.containsKey(c)){
                arr.add(LoaderType.TYPES.get(c));
            }
        }
        return arr.stream().distinct().toArray(LoaderType[]::new);
    }

    @Override
    public ModSource.Type getSourceType() {
        return ModSource.Type.MODRINTH;
    }

    @Override
    public Date getCreationDate() {
        return DateUtil.fromString(published);
    }

}
