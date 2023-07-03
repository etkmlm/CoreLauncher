package com.cdev.corelauncher.minecraft.modding.curseforge.entities;

import com.google.gson.annotations.SerializedName;

public enum FileReleaseType {
    NONE,
    @SerializedName("1") RELEASE,
    @SerializedName("2") BETA,
    @SerializedName("3") ALPHA
}
