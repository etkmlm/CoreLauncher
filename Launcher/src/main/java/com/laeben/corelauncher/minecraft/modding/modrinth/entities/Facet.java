package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import com.laeben.corelauncher.utils.StringUtils;

import java.util.List;

public class Facet{

    public static Facet or(String key, List<String> values){
        return new Facet(key, values);
    }
    public static Facet get(String key, String value){
        return new Facet(key, List.of(value));
    }

    public String id;
    public String key;
    public List<String> values;

    public Facet(String key, List<String> values){
        this.key = key;
        this.values = values;
    }

    public Facet setId(String id){
        this.id = id;
        return this;
    }

    @Override
    public String toString(){
        return StringUtils.jsArray(values.stream().map(x -> key + ":" + x).toList());
    }
}