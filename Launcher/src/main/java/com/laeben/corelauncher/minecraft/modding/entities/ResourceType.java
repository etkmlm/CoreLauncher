package com.laeben.corelauncher.minecraft.modding.entities;

import java.util.Arrays;

public enum ResourceType {
    MOD(6, "mod"), MODPACK(4471, "modpack"), RESOURCE(12, "resourcepack"), WORLD(17, "world"), SHADER(0, "shader");

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
