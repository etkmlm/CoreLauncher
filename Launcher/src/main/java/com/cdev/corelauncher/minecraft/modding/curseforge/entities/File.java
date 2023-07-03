package com.cdev.corelauncher.minecraft.modding.curseforge.entities;

import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.List;

public class File {
    public static class Dependency{
        public int modId;
        public FileRelationType relationType;
    }

    public static class Module{
        public String name;
        public long fingerprint;
    }

    public File(){

    }

    public File(int id, int modId){
        this.id = id;
        this.modId = modId;
    }

    public int id;
    public int modId;
    @Expose(serialize = false)
    public boolean isAvailable;
    public String displayName;
    public String fileName;
    @Expose(serialize = false)
    public FileReleaseType releaseType;
    @Expose(serialize = false)
    public FileStatus fileStatus;
    @Expose(serialize = false)
    public Date fileDate;
    @Expose(serialize = false)
    public long fileLength;
    @Expose(serialize = false)
    public int downloadCount;
    public String downloadUrl;
    public String[] gameVersions;
    @Expose(serialize = false)
    public List<Dependency> dependencies;
    @Expose(serialize = false)
    public List<Module> modules;
    @Expose(serialize = false)
    public boolean isServerPack;

    public List<Dependency> getDependencies(){
        if (dependencies == null)
            return List.of();
        return dependencies.stream().filter(y -> y.relationType == FileRelationType.REQUIRED).toList();
    }

    public List<Module> getModules(){
        if (modules == null)
            return List.of();
        return modules;
    }

    @Override
    public int hashCode() {
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof File f && f.id == id;
    }
}
