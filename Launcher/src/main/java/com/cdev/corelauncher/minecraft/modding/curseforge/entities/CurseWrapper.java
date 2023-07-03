package com.cdev.corelauncher.minecraft.modding.curseforge.entities;

import com.google.gson.annotations.SerializedName;

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
