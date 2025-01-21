package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.google.gson.annotations.SerializedName;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.minecraft.modding.entity.*;

import java.util.Date;
import java.util.List;

public class ResourceRinth implements ModResource {
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
    public List<String> getCategories() {
        return categories;
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
    public List<String> getAuthors() {
        return List.of(team == null ? author : team);
    }

    @Override
    public List<String> getGameVersions() {
        return gameVersions;
    }

    @Override
    public ModSource.Type getSourceType() {
        return ModSource.Type.MODRINTH;
    }

    @Override
    public Date getCreationDate() {
        return DateUtil.fromString(published);
    }


    /*public List<CResource> getAllVersions(Profile p){
        List<Version> versions;
        try{
            versions = Modrinth.getModrinth().getProjectVersions(
                    getId(),
                    p.getVersionId(),
                    p.getWrapperIdentifier(projectType));
        } catch (NoConnectionException | HttpException e) {
            versions = List.of();
        }
        return versions.stream().map(a -> (CResource)CResource.fromRinthResourceGeneric(this, a)).toList();
    }


    public List<CResource> getResourceWithDependencies(Profile p) {
        var id = p.getWrapperIdentifier(projectType);

        List<Version> versions;

        try{
            versions = Modrinth.getModrinth().getProjectVersions(getId(), p.getVersionId(), id);
        } catch (NoConnectionException | HttpException e) {
            versions = List.of();
        }

        if (versions.isEmpty())
            return null;

        try{
            return Modrinth.getModrinth().getDependenciesFromVersion(versions.get(0), p.getVersionId(), id);
        } catch (NoConnectionException | HttpException e) {
            return null;
        }
    }*/

}
