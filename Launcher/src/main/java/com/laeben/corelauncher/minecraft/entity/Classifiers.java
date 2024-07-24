package com.laeben.corelauncher.minecraft.entity;

import com.laeben.corelauncher.api.entity.OS;
import com.google.gson.annotations.SerializedName;

public class Classifiers {
    @SerializedName("natives-linux")
    public Asset linuxNatives;

    @SerializedName("natives-osx")
    public Asset osxNatives;

    @SerializedName("natives-windows")
    public Asset windowsNatives;
    public Asset sources;
    public Asset javadoc;


    public Asset getNatives(OS os){
        if (os == OS.WINDOWS)
            return windowsNatives;
        else if (os == OS.OSX)
            return osxNatives;
        else
            return linuxNatives;
    }
}
