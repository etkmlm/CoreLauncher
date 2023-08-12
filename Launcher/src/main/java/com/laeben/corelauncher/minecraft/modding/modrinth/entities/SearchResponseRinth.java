package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchResponseRinth {
    public List<ResourceRinth> hits;
    public int offset;
    public int limit;
    @SerializedName("total_hits")
    public int totalHits;
}
