package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ModrinthSearchResponse {
    public List<ModrinthResource> hits;
    public int offset;
    public int limit;
    @SerializedName("total_hits")
    public int totalHits;
}
