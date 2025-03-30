package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.modding.entity.resource.*;

import java.util.Arrays;

public enum ResourceType {
    MOD(6, "mod", Mod.class, "mods"),
    MODPACK(4471, "modpack", Modpack.class, "."),
    RESOURCEPACK(12, "resourcepack", Resourcepack.class, "resourcepacks"),
    WORLD(17, "world", World.class, "saves"),
    SHADER(6552, "shader", Shader.class, "shaderpacks");

    private static final ResourceType[] GLOBAL = { ResourceType.RESOURCEPACK, ResourceType.SHADER, ResourceType.WORLD };

    private final int forgeId;
    private final String name;
    private final Class clz;
    private final String storingFolder;
    ResourceType(int id, String name, Class clz, String storingFolder) {
        this.forgeId = id;
        this.name = name;
        this.clz = clz;
        this.storingFolder = storingFolder;
    }

    public int getId(){
        return forgeId;
    }
    public static ResourceType fromName(String name){
        for(ResourceType type : values()){
            if(type.name.equals(name))
                return type;
        }

        return null;
    }
    public static ResourceType fromId(int id){
        for(ResourceType type : values()){
            if(type.forgeId == id)
                return type;
        }

        return null;
    }

    public Class getEntityClass(){
        return clz;
    }

    public String getStoringFolder(){
        return storingFolder;
    }

    public String getName(){
        return name;
    }

    public boolean isGlobal(){
        return Arrays.stream(GLOBAL).anyMatch(x -> x == this);
    }

    public static boolean isGlobal(String name){
        return Arrays.stream(GLOBAL).anyMatch(x -> x.name.equals(name));
    }

    public static boolean isGlobal(int id){
        return Arrays.stream(GLOBAL).anyMatch(x -> x.forgeId == id);
    }
}
