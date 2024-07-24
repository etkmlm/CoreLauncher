package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.google.gson.annotations.SerializedName;

public enum FileStatus {
    NONE,
    @SerializedName("1") PROCESSING,
    @SerializedName("2") CHANGES_REQUIRED,
    @SerializedName("3") UNDER_REVIEW,
    @SerializedName("4") APPROVED,
    @SerializedName("5") REJECTED,
    @SerializedName("6") MALWARE,
    @SerializedName("7") DELETED,
    @SerializedName("8") ARCHIVED,
    @SerializedName("9") TESTING,
    @SerializedName("10") RELEASED,
    @SerializedName("11") READY,
    @SerializedName("12") DEPRECATED,
    @SerializedName("13") BAKING,
    @SerializedName("14") AWAITING,
    @SerializedName("15") FAILED
}
