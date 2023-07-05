package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

public enum ClassType {
    MOD(6), MODPACK(4471), RESOURCE(12), WORLD(17);

    private final int id;
    ClassType(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }
}
