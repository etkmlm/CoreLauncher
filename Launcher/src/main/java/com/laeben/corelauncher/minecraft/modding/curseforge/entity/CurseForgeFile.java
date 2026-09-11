package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.List;

public class CurseForgeFile {
    public static class Dependency{
        public int modId;
        public FileRelationType relationType;
    }

    public static class Module{
        public String name;
        public long fingerprint;
    }

    public enum HashAlg{
        NONE, SHA1, MD5
    }

    public static class Hash{
        public String value;
        public int algo;

        public HashAlg getAlgorithm(){
            return HashAlg.values()[algo];
        }
    }

    public CurseForgeFile(){

    }

    public CurseForgeFile(int id, int modId){
        this.id = id;
        this.modId = modId;
    }

    public int id;
    public int modId;
    @Expose
    public boolean isAvailable;
    public String displayName;
    public String fileName;
    @Expose
    public FileReleaseType releaseType;
    @Expose
    public FileStatus fileStatus;
    @Expose
    public Date fileDate;
    @Expose
    public long fileLength;
    @Expose
    public int downloadCount;
    public String downloadUrl;
    public String[] gameVersions;
    public transient String mainGameVersion;
    public transient String mainLoader;

    @Expose
    public List<Dependency> dependencies;
    @Expose
    public List<Module> modules;
    @Expose
    public List<Hash> hashes;
    @Expose
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

    public String getHash(HashAlg alg){
        if (hashes == null || hashes.isEmpty()) return null;

        for (var hash : hashes){
            if (hash.getAlgorithm() == alg) return hash.value;
        }
        return null;
    }

    @Override
    public int hashCode() {
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CurseForgeFile f && f.id == id;
    }
}
