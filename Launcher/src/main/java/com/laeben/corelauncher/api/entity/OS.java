package com.laeben.corelauncher.api.entity;

import com.google.gson.annotations.SerializedName;

public enum OS {
    @SerializedName("linux")
    LINUX("linux"),
    @SerializedName("windows")
    WINDOWS("windows"),
    @SerializedName("osx")
    OSX("mac"),

    UNKNOWN("");

    private final String name;

    OS(String name){
        this.name = name;
    }

    public static OS getSystemOS(){
        String sysOs = System.getProperty("os.name").toLowerCase();
        var all = OS.values();
        for (OS os : all)
            if (sysOs.contains(os.name))
                return os;

        return OS.UNKNOWN;
    }

    public String getName(){
        return name;
    }
}
