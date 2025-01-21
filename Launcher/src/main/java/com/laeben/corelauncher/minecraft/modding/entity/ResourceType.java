package com.laeben.corelauncher.minecraft.modding.entity;

import java.util.Arrays;

public enum ResourceType {
    MOD(6, "mod"), MODPACK(4471, "modpack"), RESOURCE(12, "resourcepack"), WORLD(17, "world"), SHADER(6552, "shader");

    private static final ResourceType[] GLOBAL = { ResourceType.RESOURCE, ResourceType.SHADER, ResourceType.WORLD };

    private final int forgeId;
    private final String name;
    ResourceType(int id, String name){
        this.forgeId = id;
        this.name = name;
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
