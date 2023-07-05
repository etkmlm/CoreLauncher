package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

public class CurseWrapper {
    public enum Type {
        ANY,
        FORGE,
        CAULDRON,
        LITELOADER,
        FABRIC,
        QUILT
    }

    public String id;
    public boolean primary;
}
