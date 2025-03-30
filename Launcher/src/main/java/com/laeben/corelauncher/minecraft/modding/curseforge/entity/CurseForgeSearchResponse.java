package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import java.util.List;

public class CurseForgeSearchResponse {
    public static class Pagination{
        public int index;
        public int pageSize;
        public int resultCount;
        public int totalCount;
    }
    public List<CurseForgeResource> data;
    public Pagination pagination;
}
