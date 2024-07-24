package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

public class SearchForge {
    public String searchFilter;
    public String gameVersion;
    public int classId;
    public int categoryId;
    public String categoryIds;
    public int pageSize;
    public int index;
    public ModsSearchSortField sortField;
    public String sortOrder;
    public CurseWrapper.Type modLoaderType;

    public void setSortOrder(boolean asc){
        sortOrder = asc ? "asc" : "desc";
    }

    public void setSearchFilter(String filter){
        searchFilter = filter.replace(" ", "+");
    }
}
