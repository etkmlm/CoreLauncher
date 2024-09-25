package com.laeben.corelauncher.minecraft.mapping.entity;

public class PGMethod {
    private final String type;
    private final String name;
    private final String oName;
    private final String[] parameters;
    public PGMethod(String identifier, String oName){
        var spl = identifier.split(" ");
        type = spl[0];
        var spl2 = spl[1].replace(")", "").split("\\(");
        name = spl2[0];
        parameters = spl2.length > 1 ? spl2[1].split(",") : new String[0];
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
    public String[] getParameters(){
        return parameters;
    }

    @Override
    public String toString(){
        return oName;
    }
}
