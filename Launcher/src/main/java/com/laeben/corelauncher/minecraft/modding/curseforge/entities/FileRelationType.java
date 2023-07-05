package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

import com.google.gson.annotations.SerializedName;

public enum FileRelationType {
    NONE,
    @SerializedName("1") EMBEDDED_LIBRARY,
    @SerializedName("2") OPTIONAL,
    @SerializedName("3") REQUIRED,
    @SerializedName("4") TOOL,
    @SerializedName("5") INCOMPATIBLE,
    @SerializedName("6") INCLUDE
}
