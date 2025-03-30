package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.wrapper.Custom;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.fabric.Fabric;
import com.laeben.corelauncher.minecraft.wrapper.forge.Forge;
import com.laeben.corelauncher.minecraft.wrapper.neoforge.NeoForge;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.minecraft.wrapper.quilt.Quilt;

import java.util.*;

public enum LoaderType {

    CUSTOM("custom", Custom.class, true),
    VANILLA("vanilla", Vanilla.class, true),
    FORGE("forge", Forge.class, false),
    CAULDRON("cauldron", null, false),
    LITELOADER("liteloader", null, false),
    FABRIC("fabric", Fabric.class, false),
    QUILT("quilt", Quilt.class, false),
    NEOFORGE("neoforge", NeoForge.class, false),
    OPTIFINE("optifine", OptiFine.class, true);

    public final static Map<String, LoaderType> TYPES = new HashMap<>();

    static {
        for (var type : LoaderType.values()) {
            TYPES.put(type.getIdentifier(), type);
        }
    }

    final String identifier;
    final Class cls;
    final boolean ntv;

    LoaderType(final String identifier, final Class cls, boolean ntv){
        this.identifier = identifier;
        this.cls = cls;
        this.ntv = ntv;
    }

    public static LoaderType fromIdentifier(final String identifier) {
        return Arrays.stream(values()).filter(a -> a.identifier.equals(identifier)).findFirst().orElse(null);
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

    public boolean isSupported(){
        return cls != null;
    }

    @Override
    public String toString(){
        return getIdentifier();
    }
}
