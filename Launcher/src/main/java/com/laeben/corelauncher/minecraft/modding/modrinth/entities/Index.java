package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import java.util.Arrays;

public enum Index {

    RELEVANCE("relevance"), DOWNLOADS("downloads"), FOLLOWS("follows"), NEWEST("newest"), UPDATED("updated");

    private final String id;

    Index(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }

    public static Index fromId(String id){
        return Arrays.stream(Index.values()).filter(x -> x.id.equals(id)).findFirst().orElse(Index.RELEVANCE);
    }

    @Override
    public String toString(){
        return id;
    }
}
