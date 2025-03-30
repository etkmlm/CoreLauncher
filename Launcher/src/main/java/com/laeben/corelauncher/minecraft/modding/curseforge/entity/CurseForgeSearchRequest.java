package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import java.util.List;

public class CurseForgeSearchRequest {
    public String searchFilter;
    public String gameVersion;
    public List<String> gameVersions;
    public int classId;
    public int categoryId;
    public List<String> categoryIds;
    //public String categoryIds;
    public int pageSize;
    public int index;
    public ModsSearchSortField sortField;
    public String sortOrder;
    public CurseForgeWrapper.Type modLoaderType;
    public List<CurseForgeWrapper.Type> modLoaderTypes;

    public void setSortOrder(boolean asc){
        sortOrder = asc ? "asc" : "desc";
    }

    public void setSearchFilter(String filter){
        searchFilter = filter.replace(" ", "+");
    }
}
