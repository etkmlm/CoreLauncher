package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;

public class CurseWrapper {
    public enum Type{
        ANY,
        FORGE,
        CAULDRON,
        LITELOADER,
        FABRIC,
        QUILT,
        NEOFORGE,
        OPTIFINE,
        NONE;

        public static Type fromLoaderType(LoaderType loaderType){
            return loaderType == null ? null : values()[loaderType.ordinal() - 1];
        }
    }
    public String id;
    public boolean primary;
}
