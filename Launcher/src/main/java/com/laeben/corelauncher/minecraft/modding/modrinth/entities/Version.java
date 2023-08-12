package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Version {

    public static class Dependency{
        @SerializedName("version_id")
        public String versionId;
        @SerializedName("project_id")
        public String projectId;
        @SerializedName("file_name")
        public String fileName;
        @SerializedName("dependency_type")
        public String type;

        public boolean isRequired(){
            return type == null || type.equals("required");
        }
    }

    public String id;
    @SerializedName("project_id")
    public String projectId;
    @SerializedName("author_id")
    public String authorId;
    public List<Dependency> dependencies;
    public String name;
    @SerializedName("version_number")
    public String version;
    @SerializedName("game_Versions")
    public List<String> gameVersions;
    public List<String> loaders;
    public List<RinthFile> files;

    public List<Dependency> getDependencies(){
        if (dependencies == null)
            return List.of();

        return dependencies;
    }

    public List<RinthFile> getFiles(){
        if (files == null)
            return List.of();

        return files;
    }

    public RinthFile getFile(){
        return getFiles().get(0);
    }


    @Override
    public boolean equals(Object e){
        return e instanceof Version v && v.id.equals(id);
    }

    @Override
    public int hashCode(){
        return -1;
    }
}
