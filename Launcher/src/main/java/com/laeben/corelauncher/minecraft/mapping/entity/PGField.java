package com.laeben.corelauncher.minecraft.mapping.entity;

public class PGField {
    private final String type;
    private final String name;
    private final String oName;

    public PGField(String identifier, String oName){
        var spl = identifier.split(" ");
        type = spl[0];
        name = spl[1];
        this.oName = oName;
    }

    public String getType(){
        return type;
    }

    public String getName(){
        return name;
    }

    public String getObfuscatedName(){
        return oName;
    }

    @Override
    public String toString(){
        return oName;
    }
}
