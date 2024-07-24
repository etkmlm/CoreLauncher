package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.wrapper.Custom;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.fabric.Fabric;
import com.laeben.corelauncher.minecraft.wrapper.forge.Forge;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.minecraft.wrapper.quilt.Quilt;

public enum LoaderType {

    CUSTOM("custom", Custom.class, true),
    VANILLA("vanilla", Vanilla.class, true),
    FORGE("forge", Forge.class, false),
    CAULDRON("cauldron", null, false),
    LITELOADER("liteloader", null, false),
    FABRIC("fabric", Fabric.class, false),
    QUILT("quilt", Quilt.class, false),
    OPTIFINE("optifine", OptiFine.class, true);

    final String identifier;
    final Class cls;
    final boolean ntv;

    LoaderType(final String identifier, final Class cls, boolean ntv){
        this.identifier = identifier;
        this.cls = cls;
        this.ntv = ntv;
    }

    public String getIdentifier(){
        return identifier;
    }

    public Class getCls(){
        return cls;
    }

    public boolean isNative(){
        return ntv;
    }

    public String getQueryIdentifier(){
        return ntv ? null : identifier;
    }

    @Override
    public String toString(){
        return getIdentifier();
    }
}
