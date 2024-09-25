package com.laeben.corelauncher.minecraft.mapping.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PGClass {
    private static final Pattern LINE_PATTERN = Pattern.compile(" {4}(\\d+:\\d+:)?(?! -)(.*) -> (.*)");

    private final String name;
    private final String oName;

    private final List<PGField> fields;
    private final List<PGMethod> methods;

    public PGClass(String name, String obfuscatedName){
        this.name = name;
        this.oName = obfuscatedName;

        fields = new ArrayList<>();
        methods = new ArrayList<>();
    }

    public void process(String oLine){
        if (oLine.startsWith("#"))
            return;

        var matcher = LINE_PATTERN.matcher(oLine);
        if (!matcher.find())
            return;
        String lines = matcher.group(1);
        String original = matcher.group(2);
        String obfuscated = matcher.group(3);
        if (lines == null)
            fields.add(new PGField(original, obfuscated));
        else
            methods.add(new PGMethod(original, obfuscated));
    }

    public String getName(){
        return name;
    }

    public String getObfuscatedName(){
        return oName;
    }

    public PGField getField(String name){
        return fields.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);
    }

    public PGMethod getMethod(String name){
        return methods.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public String toString(){
        return oName;
    }
}
