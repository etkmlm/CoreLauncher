package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import java.util.List;

public class Manifest {
    public static class Minecraft{
        public String version;
        public List<CurseWrapper> modLoaders;
    }

    public static class ManifestFile{
        public int projectID;
        public int fileID;
        public boolean required;
    }

    public String name;
    public String version;
    public String author;
    public List<ManifestFile> files;
    public Minecraft minecraft;



}
