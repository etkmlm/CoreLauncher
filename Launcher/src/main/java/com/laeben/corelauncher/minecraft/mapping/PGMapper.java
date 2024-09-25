package com.laeben.corelauncher.minecraft.mapping;

import com.laeben.corelauncher.minecraft.mapping.entity.PGClass;

import java.util.regex.Pattern;

public class PGMapper {
    private static final String REGEX = " -> ([^:]*):\\n(((# [^\\n]+\\n?)|( {4}(\\d+:\\d+:)?[^-]*-> ([^\\n]*)\\n?))+)?";

    private final String mapping;

    public PGMapper(String mapping){
        this.mapping = mapping;
    }

    public PGClass getClass(String pkgName){
        var pattern = Pattern.compile(pkgName.replace(".", "\\.").replace("$", "\\$") + REGEX, Pattern.DOTALL);
        var matcher = pattern.matcher(mapping);
        if (!matcher.find())
            return null;

        var cls = new PGClass(pkgName, matcher.group(1));

        matcher.group(2).lines().forEach(cls::process);
        return cls;
    }
}
