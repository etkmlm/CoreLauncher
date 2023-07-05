package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

import java.util.List;

public class SearchResponse {
    public static class Pagination{
        public int index;
        public int pageSize;
        public int resultCount;
        public int totalCount;
    }
    public List<Resource> data;
    public Pagination pagination;
}
