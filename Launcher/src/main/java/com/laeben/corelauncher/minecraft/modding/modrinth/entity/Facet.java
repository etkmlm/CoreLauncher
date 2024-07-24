package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.laeben.core.util.StrUtil;

import java.util.List;

public class Facet{

    public enum Statement{
        NONE, AND, OR, NOT
    }

    public static Facet or(String key, List<String> values){
        return new Facet(key, values, Statement.OR);
    }
    public static Facet and(String key, List<String> values){
        return new Facet(key, values, Statement.AND);
    }
    public static Facet get(String key, String value){
        return new Facet(key, List.of(value), Statement.NONE);
    }
    public static Facet not(String key, List<String> value) { return new Facet(key, value, Statement.NOT); }


    public String id;
    public String key;
    public List<String> values;
    public Statement statement;

    public Facet(String key, List<String> values, Statement s){
        this.key = key;
        this.values = values;
        this.statement = s;
    }

    public Facet setId(String id){
        this.id = id;
        return this;
    }

    public boolean isEmpty(){
        return values == null || values.isEmpty();
    }

    public boolean isPresent(){
        return !isEmpty();
    }

    @Override
    public String toString(){

        return switch (statement){
            case NONE, OR -> StrUtil.jsArray(values.stream().map(x -> key + ":" + x).toList());
            case AND -> String.join(",", values.stream().map(a -> "[\"" + key + ":" + a + "\"]").toList());
            case NOT -> String.join(",", values.stream().map(a -> "[\"" + key + "!=" + a + "\"]").toList());
        };

        //return StrUtil.jsArray(values.stream().map(x -> key + ":" + x).toList());
    }
}